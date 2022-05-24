from unicodedata import name

from aws_cdk import (
    Stack,
    aws_ec2 as ec2,
    aws_ssm as ssm,
    CfnOutput,
)

from constructs import Construct

from magento_platform.magento_app_stack import MagentoAppStack
from magento_platform.magento_db_stack import MagentoDBStack
from magento_platform.magento_es_stack import MagentoElasticsearchStack


class MagentoPlatformStack(Stack):

    def __init__(self, scope: Construct, construct_id: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # Create VPC
        # default is all AZs in region
        self.vpc = ec2.Vpc(self, "MagentoVpc", max_azs=3)
        
        #Create Security Group for each stack
        self.RDS_security_group = ec2.SecurityGroup(self, "RDS SG",vpc=self.vpc)
        self.OS_security_group = ec2.SecurityGroup(self, "Opensearch SG", vpc=self.vpc)
        self.App_security_group = ec2.SecurityGroup(self, "Magento SG", vpc=self.vpc)

        #Open RDS security group for fargate to access it 
        self.RDS_security_group.add_ingress_rule(
            self.App_security_group,
            ec2.Port(protocol=ec2.Protocol.TCP, string_representation="Authorize Fargate to RDS", from_port=3306, to_port=3306)
        )

        #Open Opensearch security group for fargate to access it 
        self.OS_security_group.add_ingress_rule(
            self.App_security_group,
            ec2.Port(protocol=ec2.Protocol.TCP, string_representation="Autorize Fargate to ES", from_port=443, to_port=443)
        )
        
        #Launch RDS stack
        databaseStack = MagentoDBStack(self,
            vpc=self.vpc, 
            rdsSG=self.RDS_security_group, 
        )

        #Launch Opensearch stack
        elasticsearchStack = MagentoElasticsearchStack(self,
            vpc=self.vpc, 
            osSG=self.OS_security_group, 
        )

        #Launch Magento application stack
        applicationStack = MagentoAppStack(self,
            vpc=self.vpc, 
            appSG=self.App_security_group,
            database_instance= databaseStack.database_instance, 
            es_domain = elasticsearchStack.es_domain 
        )
        
        CfnOutput(self, "MAGENTO_URL",
                  value="http://" + applicationStack.magento_service.load_balancer.load_balancer_dns_name)
        CfnOutput(self, "MAGENTO_ADMIN_URL",
                  value="http://" + applicationStack.magento_service.load_balancer.load_balancer_dns_name + "/admin")