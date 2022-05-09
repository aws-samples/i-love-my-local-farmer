import { ZoneDelegationRecord } from 'aws-cdk-lib/aws-route53';
import { STS, ACM, Route53 } from 'aws-sdk';

type Actions = 'UPSERT' | 'DELETE';

const domain = process.env.DOMAIN!;
const zoneName = process.env.ZONE_NAME!;
const rootZoneName = process.env.ROOT_ZONE_NAME;
const region = process.env.REGION;
const delegationRoleArn = process.env.DELEGATION_ROLE_ARN!;
const rootDelegationRoleArn = process.env.ROOT_DELEGATION_ROLE_ARN;

const sts = new STS();
const acm = new ACM({ region });

/**
 * Add certificate validation record to both local & remote hosted Zone
 * for subdomain in the certificate, the local hosted zone (e.g. dev) will be updated with the validation record
 * if root domain is in the certificate, then the remote hosted zone in the Root account will be updated with the validation record
 */
export async function handler(event: any): Promise<any> {
  const { PhysicalResourceId: CertificateArn } = event;
  const { Certificate } = await acm.describeCertificate({ CertificateArn }).promise();
  const record = Certificate?.DomainValidationOptions?.find((option: any) => option.DomainName === domain)?.ResourceRecord;
  const rootRecord = Certificate?.DomainValidationOptions?.find((option: any) => option.DomainName === rootZoneName)?.ResourceRecord;

  if (!record) return { IsComplete: false };
  if (rootZoneName && !rootRecord) return { IsComplete: false };

  switch (event.RequestType) {
    case 'Create':
    case 'Update':
      await validationRecords('UPSERT', record, rootRecord);
      await acm.waitFor('certificateValidated', { CertificateArn }).promise();
      return {
        IsComplete: true,
        Data: { CertificateArn },
      };
    default:
      if(Certificate.InUseBy && Certificate.InUseBy.length > 0) return { IsComplete: false };
      await validationRecords('DELETE', record, rootRecord);
      await acm.deleteCertificate({ CertificateArn: event.PhysicalResourceId }).promise();
      return { IsComplete: true };
  }
}

export async function validationRecords(action: Actions, record: ACM.ResourceRecord, rootRecord?: ACM.ResourceRecord) {

  const role = await sts.assumeRole({ RoleArn: delegationRoleArn, RoleSessionName: 'AddValidationRecords' }).promise();
  const { AccessKeyId, SecretAccessKey, SessionToken } = role.Credentials!;
  const route53 = new Route53({ credentials: { accessKeyId: AccessKeyId, secretAccessKey: SecretAccessKey, sessionToken: SessionToken } });

  const { HostedZones } = await route53.listHostedZonesByName({ DNSName: zoneName }).promise();
  const [hostedZone] = HostedZones;
  const hostedZoneId = hostedZone.Id.replace('/hostedzone/', '');

  await route53
    .changeResourceRecordSets(generateRecordChangeParams(action, hostedZoneId, record.Name, record.Value))
    .promise();

  if(!rootRecord || !rootDelegationRoleArn) return;

  const rootRole = await sts.assumeRole({ RoleArn: rootDelegationRoleArn, RoleSessionName: 'AddRootValidationRecords' }).promise();
  const { AccessKeyId: rootAccessKeyId, SecretAccessKey: rootSecretAccessKey, SessionToken: rootSessionToken } = rootRole.Credentials!;
  const rootRoute53 = new Route53({ credentials: { accessKeyId: rootAccessKeyId, secretAccessKey: rootSecretAccessKey, sessionToken: rootSessionToken } });

  const { HostedZones: rootHostedZones } = await rootRoute53.listHostedZonesByName({ DNSName: rootZoneName }).promise();
  const [rootHostedZone] = rootHostedZones;
  const rootHostedZoneId = rootHostedZone.Id.replace('/hostedzone/', '');

  await rootRoute53
    .changeResourceRecordSets(generateRecordChangeParams(action, rootHostedZoneId, rootRecord.Name, rootRecord.Value))
    .promise();

  return;
}

function generateRecordChangeParams(action: Actions, hostedZoneId: string, recordName: string, recordValue: string) {
  return {
    HostedZoneId: hostedZoneId,
    ChangeBatch: {
      Changes: [
        {
          Action: action,
          ResourceRecordSet: {
            Name: recordName,
            ResourceRecords: [
              {
                Value: recordValue,
              },
            ],
            TTL: 60,
            Type: 'CNAME',
          },
        },
      ],
    },
  };
}
