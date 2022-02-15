import { Construct } from 'constructs';
import {
  Duration,
  CustomResource,
  aws_iam as iam,
  aws_certificatemanager as certificateManager,
  aws_lambda_nodejs as lambda,
  custom_resources as customResources,
} from 'aws-cdk-lib';

export interface CertificateProps {
  /** subdomain name e.g. dev.localfarmer.com */
  domain: string;
  /** hosted zone domain name */
  zoneName: string;
  region?: string;
  zoneDelegationRoleArn: string;
  /** optional config that allows Certificate to create & validate with root domain hosed in different AWS account */
  rootConfig?: {
    /** root domain name e.g. localfarmer.com */
    rootZoneName: string;
    /** the cross account delegation role that was passed in from the Root Stack */
    rootZoneDelegationRoleArn: string;
  }
}

/** Certificate Construct that can validate domains belonging to a remote Hosted Zone in a different AWS account  */
export class Certificate extends Construct {
  public readonly certificate: certificateManager.ICertificate;

  constructor(scope: Construct, id: string, props: CertificateProps) {
    super(scope, id);
    const { domain, zoneName, region, zoneDelegationRoleArn, rootConfig } = props;

    const createEnvironment: any = {
      DOMAIN: domain,
    };
    if (region) createEnvironment['REGION'] = region;
    if (rootConfig) createEnvironment['ROOT_ZONE_NAME'] = rootConfig.rootZoneName;

    const certificateHandler = new lambda.NodejsFunction(this, 'Create', {
      environment: createEnvironment,
      initialPolicy: [new iam.PolicyStatement({ actions: ['acm:RequestCertificate'], resources: ['*'] })],
    });

    const validateEnvironment: any = {
        DOMAIN: domain,
        ZONE_NAME: zoneName,
        DELEGATION_ROLE_ARN: zoneDelegationRoleArn
    }
    if (region) validateEnvironment['REGION'] = region;
    if(rootConfig) {
        validateEnvironment['ROOT_ZONE_NAME'] = rootConfig.rootZoneName;
        validateEnvironment['ROOT_DELEGATION_ROLE_ARN'] = rootConfig.rootZoneDelegationRoleArn;
    }

    const validationHandler = new lambda.NodejsFunction(this, 'Validate', {
      environment: validateEnvironment,
      timeout: Duration.minutes(14),
      initialPolicy: [
        new iam.PolicyStatement({
          actions: ['sts:AssumeRole', 'route53:ChangeResourceRecordSets', 'acm:DescribeCertificate', 'acm:DeleteCertificate'],
          resources: ['*'],
        }),
      ],
    });

    const provider = new customResources.Provider(this, 'CertificateProvider', {
      onEventHandler: certificateHandler,
      isCompleteHandler: validationHandler,
    });

    const customResource = new CustomResource(this, 'CertificateCustomResource', {
      serviceToken: provider.serviceToken,
    });

    const certificateArn = customResource.getAttString('CertificateArn');
    this.certificate = certificateManager.Certificate.fromCertificateArn(this, 'DomainCertificate', certificateArn);
  }
}
