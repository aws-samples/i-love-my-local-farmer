import { Construct } from 'constructs';
import { Stack, StackProps, CustomResource, aws_iam as iam, aws_lambda_nodejs as lambda, custom_resources } from 'aws-cdk-lib';
import { App } from './app/App';
import { Certificate } from './certificate/Certificate';
import { DomainEnv } from '../../bin/cdk-dns-subdomains';

export interface SubDomainStackProps extends StackProps {
  /**  domain i.e for dev.localfarmer.com */
  env: DomainEnv;
  /** the cross account role used to add delegation records in parent domain hosted zone */
  zoneDelegationRole: iam.Role;
  /** will root domain point to this environment i.e will localfarmer.com point to prod.localfaremer.com */
  rootZoneDelegationRole?: iam.Role;
}

/**
 * Children stack that will host each subDomain Environment i.e dev, staging & prod
 */
export class SubDomainStack extends Stack {
  private createAliasRecordFn = new lambda.NodejsFunction(this, `AliasRecord`, {
    initialPolicy: [new iam.PolicyStatement({ actions: ['sts:AssumeRole'], resources: ['*'] })],
  });

  constructor(scope: Construct, id: string, props: SubDomainStackProps) {
    super(scope, id, props);
    const { env, zoneDelegationRole, rootZoneDelegationRole } = props;
    const { domain } = env;
    const [prefix, ...root] = domain.split('.');
    const rootDomain = root.join('.');
    const apiDomain = 'api.' + domain;

    const cloudfrontCertificate = new Certificate(this, 'CloudfrontCertificate', {
      region: 'us-east-1', // for cloudfront it must be in us-east-1
      domain,
      zoneName: domain,
      zoneDelegationRoleArn: zoneDelegationRole.roleArn,
      rootConfig: rootZoneDelegationRole ? { rootZoneName: rootDomain, rootZoneDelegationRoleArn: rootZoneDelegationRole.roleArn } : undefined,
    });

    const apiCertificate = new Certificate(this, 'ApiCertificate', {
      domain: apiDomain,
      zoneName: domain,
      zoneDelegationRoleArn: zoneDelegationRole.roleArn,
    });

    const frontendDomains = [domain];
    if (rootZoneDelegationRole) frontendDomains.push(rootDomain);

    const { cloudFrontDistro, api } = new App(this, 'Frontend', {
      frontendDomains,
      frontendCertificate: cloudfrontCertificate.certificate,
      apiDomain,
      apiCertificate: apiCertificate.certificate,
    });

    this.createAliasRecord(
      domain,
      apiDomain,
      api.domainName?.domainNameAliasDomainName!,
      api.domainName?.domainNameAliasHostedZoneId!,
      zoneDelegationRole.roleArn,
    );

    this.createAliasRecord(
      domain,
      domain,
      cloudFrontDistro.distributionDomainName,
      'Z2FDTNDATAQYW2', // This is a constant value for cloudfront
      zoneDelegationRole.roleArn,
    );

    if (rootZoneDelegationRole) {
      this.createAliasRecord(
        rootDomain,
        rootDomain,
        cloudFrontDistro.distributionDomainName,
        'Z2FDTNDATAQYW2', // This is a constant value for cloudfront
        rootZoneDelegationRole.roleArn,
      );
    }
  }

  /** Custom Resource to update the hosted zone in root account with alias record to point to cloudfront i.e localfarmer.com -> 123.cloudfront.net */
  createAliasRecord(zoneName: string, domain: string, targetAlias: string, targetHostedZoneId: string, delegationRoleArn: string) {
    const [prefix] = domain.split('.');
    const provider = new custom_resources.Provider(this, `${prefix}-Provider`, {
      onEventHandler: this.createAliasRecordFn,
    });
    new CustomResource(this, `${prefix}-AliasRecordCustomResource`, {
      serviceToken: provider.serviceToken,
      properties: {
        zoneName,
        domain,
        targetAlias,
        targetHostedZoneId,
        delegationRoleArn,
      },
    });
  }
}
