import { Construct } from 'constructs';
import {
  Stack,
  StackProps,
  Fn,
  aws_route53 as route53,
  aws_iam as iam,
  CustomResource,
  aws_lambda_nodejs as lambda,
  custom_resources,
} from 'aws-cdk-lib';
import { DomainEnv } from '../../bin/cdk-dns-subdomains';

export interface RootDomainStackProps extends StackProps {
  /** The root domain environment */
  env: DomainEnv;
  /** List of child environments that will host the subdomains i.e dev.localfarmer.com, staging.localfarmer.com, ...etc */
  subDomainEnvs: { [env: string]: DomainEnv };
  /** the child environment with access to update root hosted zone and create root domain records */
  envWithRootAccess: DomainEnv;
}

/**
 * The Main Parent Stack that holds the hosted Zone of the Root Domain
 * @public @member {iam.Role} zoneDelegationRole - the role to be used by children accounts to update root hosted zone with their validation records
 */
export class RootDomainStack extends Stack {
  public rootZoneDelegationRole: iam.Role;
  public subZoneDelegationRoles: { [key: string]: iam.Role };

  constructor(scope: Construct, id: string, props: RootDomainStackProps) {
    super(scope, id, props);
    const { env, subDomainEnvs, envWithRootAccess } = props;

    const rootDomainHosedZone = this.createHostedZone(env.domain, envWithRootAccess.account);
    this.rootZoneDelegationRole = rootDomainHosedZone.crossAccountZoneDelegationRole!;

    this.subZoneDelegationRoles = Object.entries(subDomainEnvs).reduce(
      (zones, [name, env]) => ({
        ...zones,
        [name]: this.createHostedZone(env.domain, env.account, rootDomainHosedZone).crossAccountZoneDelegationRole,
      }),
      {},
    );

    this.updateRegDomain(env.domain, rootDomainHosedZone);
  }

  /** HostedZone with CrossAccoutZoneDelegationRole */
  createHostedZone(domain: string, account: string, rootZone?: route53.HostedZone) {
    const [prefix] = domain.split('.');
    const hostedZone = new route53.PublicHostedZone(this, `${prefix}-HostedZone`, {
      zoneName: domain,
      crossAccountZoneDelegationPrincipal: new iam.AccountPrincipal(account),
      crossAccountZoneDelegationRoleName: `${prefix}-ZoneDelegationRole`,
    });
    if (rootZone) {
      new route53.ZoneDelegationRecord(this, `${prefix}-ZoneDelegationRecord`, {
        zone: rootZone,
        recordName: domain,
        nameServers: hostedZone.hostedZoneNameServers!,
      });
    }
    return hostedZone;
  }

  /** Custom Resource to update the Domain registrar with Hosted Zone name servers */
  updateRegDomain(domain: string, hostedZone: route53.HostedZone) {
    const provider = new custom_resources.Provider(this, 'Provider', {
      onEventHandler: new lambda.NodejsFunction(this, 'UpdateRegDomain', {
        initialPolicy: [
          new iam.PolicyStatement({
            actions: ['route53domains:UpdateDomainNameservers'],
            resources: ['*'],
          }),
        ],
      }),
    });
    new CustomResource(this, 'CustomResource', {
      serviceToken: provider.serviceToken,
      properties: {
        domain,
        nameServers: Fn.join(',', hostedZone.hostedZoneNameServers!),
      },
    });
  }
}
