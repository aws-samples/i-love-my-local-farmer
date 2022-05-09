import { Construct } from 'constructs';
import {
  Duration,
  aws_certificatemanager as certificateManager,
  aws_cloudfront as cloudFront,
  aws_cloudfront_origins as origins,
  aws_s3 as s3,
  aws_s3_deployment as s3deploy,
  aws_apigateway as apigateway,
  aws_lambda as lambda,
  aws_route53 as route53,
  custom_resources as customResources,
  aws_route53_targets as route53Targets,
} from 'aws-cdk-lib';
import { ICertificate } from 'aws-cdk-lib/aws-certificatemanager';

export interface FrontendProps {
  frontendDomains: string[];
  frontendCertificate: certificateManager.ICertificate;
  apiDomain: string;
  apiCertificate: certificateManager.ICertificate;
}

/** App Construct responsible for creating the frontend & backend Api resources for a simple demo App */
export class App extends Construct {
  public cloudFrontDistro: cloudFront.Distribution;
  public api: apigateway.LambdaRestApi;

  constructor(scope: Construct, id: string, props: FrontendProps) {
    super(scope, id);
    const { frontendDomains, frontendCertificate, apiDomain, apiCertificate } = props;

    const frontend = this.createFrontend(frontendDomains, frontendCertificate);
    this.api = this.createBackend(apiDomain, apiCertificate);
    const config = { apiEndpoint: 'https://' + apiDomain };
    this.deployPublicAssets(frontend.assetsBucket, frontend.cloudFrontDistro, config);

    this.cloudFrontDistro = frontend.cloudFrontDistro;
  }

  createFrontend(domainNames: string[], domainCertificate: ICertificate) {
    const assetsBucket = new s3.Bucket(this, 'Assets');

    const cloudFrontDistro = new cloudFront.Distribution(this, 'Distribution', {
      defaultBehavior: {
        origin: new origins.S3Origin(assetsBucket),
        viewerProtocolPolicy: cloudFront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
      },
      domainNames,
      certificate: domainCertificate,
      errorResponses: [
        {
          ttl: Duration.seconds(0),
          httpStatus: 404,
          responseHttpStatus: 200,
          responsePagePath: '/',
        },
      ],
      defaultRootObject: 'index.html',
    });

    return { cloudFrontDistro, assetsBucket };
  }

  createBackend(apiDomain: string, apiCertificate: certificateManager.ICertificate) {
    const [, env] = apiDomain.split('.');
    const handler = new lambda.Function(this, 'Api', {
      runtime: lambda.Runtime.NODEJS_12_X,
      handler: 'index.handler',
      code: lambda.Code.fromInline(`exports.handler = async () => ({
        'body': '${env}',
        'headers': {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET"
        }
      });`),
    });

    const api = new apigateway.LambdaRestApi(this, 'API', {
      handler,
      domainName: {
        domainName: apiDomain,
        certificate: apiCertificate,
        basePath: 'env',
      },
    });

    return api;
  }

  /** deploy a config file that contains any backend details to be served to the frontend */
  deployPublicAssets(bucket: s3.Bucket, cloudFrontDistro: cloudFront.Distribution, config: object) {
    const deployment = new s3deploy.BucketDeployment(this, 'DeployAssets', {
      sources: [s3deploy.Source.asset('./lib/subDomain/app/assets')],
      destinationBucket: bucket,
      distribution: cloudFrontDistro,
    });

    const filename = 'config.json';
    const putConfig = new customResources.AwsCustomResource(this, 'PutConfig', {
      onUpdate: {
        service: 'S3',
        action: 'putObject',
        parameters: {
          Bucket: bucket.bucketName,
          Key: filename,
          Body: JSON.stringify(config),
        },
        physicalResourceId: customResources.PhysicalResourceId.of('NO_DELETE_REQUIRED'),
      },
      policy: customResources.AwsCustomResourcePolicy.fromSdkCalls({
        resources: customResources.AwsCustomResourcePolicy.ANY_RESOURCE,
      }),
    });
    putConfig.node.addDependency(deployment);
  }
}
