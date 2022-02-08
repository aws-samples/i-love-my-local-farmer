import { ACM } from 'aws-sdk';

const domain = process.env.DOMAIN!;
const region = process.env.REGION;
const rootDomain = process.env.ROOT_ZONE_NAME;

const acm = new ACM({ region });

/** create a new SSL certificate for current domain (e.g. dev.localfarmer.com) with the option of adding the root domain (e.g. localfarmer.com) */
export async function handler(event: any): Promise<any> {
  switch (event.RequestType) {
    case 'Create':
    case 'Update':
      const { CertificateArn } = await acm
        .requestCertificate({
          DomainName: domain,
          SubjectAlternativeNames: rootDomain ? [rootDomain] : undefined,
          ValidationMethod: 'DNS',
        })
        .promise();

      return { PhysicalResourceId: CertificateArn };

    default:
      return;
  }
}
