from unicodedata import name

from aws_cdk import (
    Stack,
    aws_ec2 as ec2,
    aws_ecs as ecs,
    aws_ssm as ssm,
    aws_iam as iam,
    CfnOutput,
    aws_rds as rds,
    Fn, App, RemovalPolicy,
    aws_opensearchservice as opensearch,
    Duration,
    aws_secretsmanager as sm,
    aws_ecs_patterns as ecs_patterns,
    aws_elasticloadbalancingv2 as elbv2,
)

from constructs import Construct


class MagentoAll(Stack):

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
        self.RDS_security_group.add_ingress_rule(
            self.App_security_group,
            ec2.Port(protocol=ec2.Protocol.TCP, string_representation="Authorize Fargate to RDS", from_port=3306, to_port=3306)
        )

        #Open Opensearch security group for fargate to access it 
        self.OS_security_group.add_ingress_rule(
            self.App_security_group,
            ec2.Port(protocol=ec2.Protocol.TCP, string_representation="Autorize Fargate to ES", from_port=443, to_port=443)
        )
        
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
                                         vpc=self.vpc,
                                         port=3306,
                                         removal_policy=RemovalPolicy.DESTROY,
                                         deletion_protection=False,
                                         multi_az=True,
                                         vpc_subnets= ec2.SubnetType.PUBLIC,
                                         security_groups = [self.RDS_security_group,]
                                         )
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
                                           vpc=self.vpc,
                                           # vpc_subnets={vpc.private_subnets[0],}
                                           zone_awareness=opensearch.ZoneAwarenessConfig(
                                               availability_zone_count=2
                                           ),
                                           removal_policy=RemovalPolicy.DESTROY,
                                           enforce_https=True,
                                           security_groups=[self.OS_security_group, ]
                                           )

        self.es_domain.add_access_policies(iam.PolicyStatement(
            principals=[iam.AnyPrincipal()],
            actions=["es:*"],
            resources=[self.es_domain.domain_arn + "/*"]))

            # Create ECS cluster
        _cluster = ecs.Cluster(self, "MagentoCluster", vpc=self.vpc)

        dbsecret = sm.Secret.from_secret_complete_arn(self, "DatabaseSecret",
                                                      secret_complete_arn=self.database_instance.secret.secret_arn,
                                                      )

        # Create Fargate service running on an ECS cluster fronted by an application load balancer.

        magento_lb = elbv2.ApplicationLoadBalancer(self, "MagentoLB", vpc=self.vpc, internet_facing=True, load_balancer_name="magento-lb") 
       
        self.magento_service = ecs_patterns.ApplicationLoadBalancedFargateService(self, "MagentoService",
                                                                              cluster=_cluster,            # Required
                                                                              cpu=2048,                    # Default is 256
                                                                              task_image_options=ecs_patterns.ApplicationLoadBalancedTaskImageOptions(
                                                                                  image=ecs.ContainerImage.from_registry(
                                                                                      "public.ecr.aws/bitnami/magento:2.4.4"),  # return RepositoryImage
                                                                                  environment={
                                                                                    "BITNAMI_DEBUG": "true",
                                                                                    "MAGENTO_EXTRA_INSTALL_ARGS": "--enable-debug-logging=true",
                                                                                    "MAGENTO_HOST": magento_lb.load_balancer_dns_name,
                                                                                    "MAGENTO_DATABASE_HOST": self.database_instance.db_instance_endpoint_address,
                                                                                    "MAGENTO_DATABASE_PORT_NUMBER": "3306",
                                                                                    "MAGENTO_DATABASE_NAME": "MagentoDB",
                                                                                    "MAGENTO_ELASTICSEARCH_HOST":self.es_domain.domain_endpoint,
                                                                                    "MAGENTO_ELASTICSEARCH_PORT_NUMBER": "443",
                                                                                    "MAGENTO_ELASTICSEARCH_USE_HTTPS": "yes",
                                                                                    "MAGENTO_SEARCH_ENGINE": "elasticsearch7",
                                                                                    "MAGENTO_DEPLOY_STATIC_CONTENT": "yes",
                                                                                    "MAGENTO_EXTERNAL_HTTP_PORT_NUMBER": "80",
                                                                                    "APACHE_HTTP_PORT_NUMBER" : "80"
                                                                                    #"MAGENTO_SKIP_BOOTSTRAP":"yes", #Keep commented for the first run
                                                                                    #"MAGENTO_SKIP_REINDEX" : "yes"  #Keep commented for the first run
                                                                                  },
                                                                                  secrets={
                                                                                      "MAGENTO_DATABASE_USER": ecs.Secret.from_secrets_manager(dbsecret, "username"),
                                                                                      "MAGENTO_DATABASE_PASSWORD": ecs.Secret.from_secrets_manager(dbsecret, "password"),
                                                                                  },
                                                                                  container_port=80
                                                                              ),
                                                                              memory_limit_mib=4096,      # Default is 512
                                                                              public_load_balancer=True,  # Default is False
                                                                              health_check_grace_period=Duration.minutes(
                                                                                  3000),
                                                                              listener_port=80,
                                                                              task_subnets=ec2.SubnetSelection(
                                                                                  subnet_type=ec2.SubnetType.PRIVATE_WITH_NAT),
                                                                              #task_subnets=ec2.SubnetSelection(
                                                                              #    subnet_type=ec2.SubnetType.PUBLIC),
                                                                              #assign_public_ip=True,
                                                                              load_balancer_name = "magento_lb",
                                                                              load_balancer = magento_lb,
                                                                              security_groups = [self.App_security_group,]
                                                                              )
        
        # Define health check
        self.magento_service.target_group.configure_health_check(
            path="/pub/health_check.php"
        )

        # Define scaling boundaries
        _scalable_target = self.magento_service.service.auto_scale_task_count(
            min_capacity=1,
            max_capacity=4
        )

        # Scaling policy for CPU
        _scalable_target.scale_on_cpu_utilization("CpuScaling",
                                                  target_utilization_percent=70
                                                  )

        # Scaling policy for Memory
        _scalable_target.scale_on_memory_utilization("MemoryScaling",
                                                     target_utilization_percent=70
                                                     )

        CfnOutput(self, "Magento_Hostname",
                  value=self.magento_service.load_balancer.load_balancer_dns_name)