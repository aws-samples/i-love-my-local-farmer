from re import A
from aws_cdk import (
    Stack,
    aws_ec2 as ec2,
    aws_ecs as ecs,
    aws_elasticloadbalancingv2 as elbv2,
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


class MagentoAppStack(Stack):

    def __init__(self, scope: Construct, construct_id: str, vpc, appSG, database_instance, es_domain, **kwargs) -> None:
        super().__init__(scope, construct_id, **kwargs)

        # Create ECS cluster
        _cluster = ecs.Cluster(self, "MagentoCluster", vpc=vpc)

        dbsecret = sm.Secret.from_secret_complete_arn(self, "DatabaseSecret",
                                                      secret_complete_arn=database_instance.secret.secret_arn,
                                                      )

        # Create Fargate service running on an ECS cluster fronted by an application load balancer.

        magento_lb = elbv2.ApplicationLoadBalancer(self, "magento_lb", vpc=vpc, internet_facing=True, load_balancer_name="magento-lb") 
       
        self.magento_service = ecs_patterns.ApplicationLoadBalancedFargateService(self, "MagentoService",
                                                                              cluster=_cluster,            # Required
                                                                              cpu=2048,                    # Default is 256
                                                                              desired_count=1,            # Default is 1
                                                                              task_image_options=ecs_patterns.ApplicationLoadBalancedTaskImageOptions(
                                                                                  image=ecs.ContainerImage.from_registry(
                                                                                      "public.ecr.aws/bitnami/magento:2.4.4"),  # return RepositoryImage
                                                                                  environment={
                                                                                    "BITNAMI_DEBUG": "true",
                                                                                    "MAGENTO_EXTRA_INSTALL_ARGS": "--enable-debug-logging=true",
                                                                                    "MAGENTO_HOST": magento_lb.load_balancer_dns_name,
                                                                                    "MAGENTO_DATABASE_HOST": database_instance.db_instance_endpoint_address,
                                                                                    "MAGENTO_DATABASE_PORT_NUMBER": "3306",
                                                                                    "MAGENTO_DATABASE_NAME": "MagentoDB",
                                                                                    "MAGENTO_ELASTICSEARCH_HOST":es_domain.domain_endpoint,
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
                                                                                  subnet_type=ec2.SubnetType.PUBLIC),
                                                                              assign_public_ip=True,
                                                                              load_balancer_name = "magento_lb",
                                                                              load_balancer = magento_lb,
                                                                              #security_groups = [appSG,]
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
        