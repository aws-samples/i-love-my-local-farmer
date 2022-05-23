from unicodedata import name

from aws_cdk import (
    Stack,
    aws_ec2 as ec2,
    aws_ssm as ssm,
    CfnOutput,
)

from constructs import Construct


class MagentoPlatformStack(Stack):

    def __init__(self, scope: Construct, construct_id: str, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # Create VPC
        # default is all AZs in region
        self.vpc = ec2.Vpc(self, "MagentoVpc", max_azs=3)
        
        #create an SSM parameters which store export VPC ID
        ssm.StringParameter(self, 'VPCID', string_value=self.vpc.vpc_id)
        
        
        #Create Security Group for each stack
        self.RDS_security_group = ec2.SecurityGroup(self, "RDS SG",vpc=self.vpc)
        self.OS_security_group = ec2.SecurityGroup(self, "Opensearch SG", vpc=self.vpc)
        self.App_security_group = ec2.SecurityGroup(self, "Magento SG", vpc=self.vpc)

        #Open RDS security group for fargate to access it 
        self.RDS_security_group.connections.allow_from_any_ipv4(
            port_range=ec2.Port(protocol=ec2.Protocol.TCP, string_representation="tcp_3306", from_port=3306, to_port=3306),
            description="Allow TCP connections on port 3306"
        )

        #Open Opensearch security group for fargate to access it 
        self.OS_security_group.connections.allow_from_any_ipv4(
            port_range=ec2.Port(protocol=ec2.Protocol.TCP, string_representation="tcp_443", from_port=443, to_port=443),
            description="Allow TCP connections on port 443"
        )

        """

        # Issue with cyclic dependancies

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
        
        
        CfnOutput(self, "AppSecurityGroup", value=self.App_security_group.security_group_id, export_name='AppSecurityGroup')
        CfnOutput(self, "OsSecurityGroup", value=self.OS_security_group.security_group_id, export_name='OsSecurityGroup')
        CfnOutput(self, "RDSSecurityGroup", value=self.RDS_security_group.security_group_id, export_name='RDSSecurityroup')
        
        """
        CfnOutput(self, "VPC", value=self.vpc.vpc_id, export_name='VPC')