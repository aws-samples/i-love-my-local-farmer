import { Route53, STS } from 'aws-sdk';

const sts = new STS();

/** add an alias record to remote parent hosted zone to point root domain to cloudfront i.e localfarmer.com -> aaa.cloudfront.net */
export async function handler(event: any): Promise<any> {
  const { zoneName, domain, delegationRoleArn, targetAlias, targetHostedZoneId } = event.ResourceProperties;

  const role = await sts.assumeRole({ RoleArn: delegationRoleArn, RoleSessionName: 'AliasRecord' }).promise();
  const { AccessKeyId, SecretAccessKey, SessionToken } = role.Credentials!;
  const route53 = new Route53({ credentials: { accessKeyId: AccessKeyId, secretAccessKey: SecretAccessKey, sessionToken: SessionToken } });

  const result = await route53.listHostedZonesByName({ DNSName: zoneName }).promise();
  const [hostedZone] = result.HostedZones;
  const hostedZoneId = hostedZone.Id.replace('/hostedzone/', '');

  switch (event.RequestType) {
    case 'Create':
    case 'Update':
      await route53.changeResourceRecordSets(generateRecordChangeParams('UPSERT', hostedZoneId, domain, targetAlias, targetHostedZoneId)).promise();
      return { PhysicalResourceId: 'DELETE_NOT_REQUIRED' };
    default:
      await route53.changeResourceRecordSets(generateRecordChangeParams('DELETE', hostedZoneId, domain, targetAlias, targetHostedZoneId)).promise();
  }
}

function generateRecordChangeParams(
  action: 'UPSERT' | 'DELETE',
  hostedZoneId: string,
  domain: string,
  targetAlias: string,
  targetHostedZoneId: string,
) {
  return {
    HostedZoneId: hostedZoneId,
    ChangeBatch: {
      Changes: [
        {
          Action: action,
          ResourceRecordSet: {
            Name: domain,
            Type: 'A',
            AliasTarget: {
              DNSName: targetAlias,
              EvaluateTargetHealth: false,
              HostedZoneId: targetHostedZoneId,
            },
          },
        },
      ],
    },
  };
}
