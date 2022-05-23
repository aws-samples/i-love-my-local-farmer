from aws_cdk import (
    Stack,
    aws_ec2 as ec2,
    aws_ecs as ecs,
    aws_rds as rds,
    Fn, App, RemovalPolicy,
    aws_ecs_patterns as ecs_patterns,
    aws_opensearchservice as opensearch,
    aws_secretsmanager as sm,
    CfnOutput,
    Duration,
    aws_ecr as ecr
)

from constructs import Construct


class MagentoDBStack(Stack):

    def __init__(self, scope: Construct, construct_id: str, vpc, rdsSG, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # Create database instance
        self._credentials = rds.Credentials.from_generated_secret(
            "magento_db_admin")
        self.database_instance = rds.DatabaseInstance(self, "MariaDBInstance",
                                         database_name="MagentoDB",
                                         engine=rds.DatabaseInstanceEngine.maria_db(
                                             version=rds.MariaDbEngineVersion.VER_10_4),
                                         instance_type=ec2.InstanceType.of(
                                             ec2.InstanceClass.BURSTABLE3, ec2.InstanceSize.LARGE),
                                         credentials=self._credentials,
                                         vpc=vpc,
                                         port=3306,
                                         removal_policy=RemovalPolicy.DESTROY,
                                         deletion_protection=False,
                                         multi_az=True,
                                         vpc_subnets= ec2.SubnetType.PUBLIC,
                                         security_groups = [rdsSG,]
                                         )

        CfnOutput(self, "MariaDB_Hostname",
                  value=self.database_instance.db_instance_endpoint_address)
        CfnOutput(self, "MariaDB_Username", value=self._credentials.username)
        
        

        
