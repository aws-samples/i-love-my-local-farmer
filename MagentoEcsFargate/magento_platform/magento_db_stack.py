from aws_cdk import (
    NestedStack,
    aws_ec2 as ec2,
    aws_rds as rds,
    RemovalPolicy,
    CfnOutput,
)

from constructs import Construct


class MagentoDBStack(NestedStack):

    def __init__(self, scope: Construct, vpc, rdsSG, **kwargs):
        super().__init__(scope, "MagentoDBStack", **kwargs)

        # Generate password for the db user magento_db_admin 
        self._credentials = rds.Credentials.from_generated_secret(
            "magento_db_admin")

        # Create database instance
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
        
        
        

        
