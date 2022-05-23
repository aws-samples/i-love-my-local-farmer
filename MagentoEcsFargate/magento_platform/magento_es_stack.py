from aws_cdk import (
    NestedStack,
    aws_ec2 as ec2,
    aws_iam as iam,
    RemovalPolicy,
    aws_opensearchservice as opensearch,
    CfnOutput,
)

from constructs import Construct


class MagentoElasticsearchStack(NestedStack):

    def __init__(self, scope: Construct, vpc, osSG, **kwargs):
        super().__init__(scope, "MagentoElasticsearchStack", **kwargs)
        
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
                                           vpc=vpc,
                                           zone_awareness=opensearch.ZoneAwarenessConfig(
                                               availability_zone_count=2
                                           ),
                                           removal_policy=RemovalPolicy.DESTROY,
                                           enforce_https=True,
                                           security_groups=[osSG, ]
                                           )

        # Define access policies
        self.es_domain.add_access_policies(iam.PolicyStatement(
            principals=[iam.AnyPrincipal()],
            actions=["es:*"],
            resources=[self.es_domain.domain_arn + "/*"]))

        CfnOutput(self, "Opensearch_Hostname",
                  value=self.es_domain.domain_endpoint)
