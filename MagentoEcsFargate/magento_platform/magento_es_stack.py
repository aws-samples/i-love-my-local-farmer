from aws_cdk import (
    Stack,
    aws_ec2 as ec2,
    aws_ecs as ecs,
    aws_rds as rds,
    aws_iam as iam,
    Fn, App, RemovalPolicy,
    aws_ecs_patterns as ecs_patterns,
    aws_opensearchservice as opensearch,
    aws_secretsmanager as sm,
    CfnOutput,
    Duration,
    aws_ecr as ecr
)

from constructs import Construct


class MagentoElasticsearchStack(Stack):

    def __init__(self, scope: Construct, construct_id: str, vpc, osSG, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # Create opensearch domain
        self.es_domain = opensearch.Domain(self, "Mangeto_ES_Domain",
                                           version=opensearch.EngineVersion.OPENSEARCH_1_2,
                                           ebs=opensearch.EbsOptions(
                                               volume_size=100,
                                               volume_type=ec2.EbsDeviceVolumeType.GP2
                                           ),
                                           node_to_node_encryption=True,
                                           encryption_at_rest=opensearch.EncryptionAtRestOptions(
                                               enabled=True
                                           ),
                                           capacity=opensearch.CapacityConfig(
                                               data_nodes=2,
                                               data_node_instance_type="r6g.large.search"
                                           ),
                                           # use_unsigned_basic_auth=True,
                                           vpc=vpc,
                                           # vpc_subnets={vpc.private_subnets[0],}
                                           zone_awareness=opensearch.ZoneAwarenessConfig(
                                               availability_zone_count=2
                                           ),
                                           removal_policy=RemovalPolicy.DESTROY,
                                           enforce_https=True,
                                           security_groups=[osSG, ]
                                           )

        self.es_domain.add_access_policies(iam.PolicyStatement(
            principals=[iam.AnyPrincipal()],
            actions=["es:*"],
            resources=[self.es_domain.domain_arn + "/*"]))

        CfnOutput(self, "Opensearch_Hostname",
                  value=self.es_domain.domain_endpoint)
