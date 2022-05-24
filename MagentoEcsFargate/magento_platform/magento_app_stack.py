from re import A
from aws_cdk import (
    NestedStack,
    aws_ec2 as ec2,
    aws_ecs as ecs,
    aws_elasticloadbalancingv2 as elbv2,
    aws_ecs_patterns as ecs_patterns,
    aws_secretsmanager as sm,
    aws_certificatemanager as cm,
    aws_route53 as r53,
    CfnOutput,
    Duration,
)

from constructs import Construct


class MagentoAppStack(NestedStack):

    def __init__(self, scope: Construct, vpc, appSG, database_instance, es_domain, **kwargs):
        super().__init__(scope, "MagentoAppStack", **kwargs)

        # Create ECS cluster
        _cluster = ecs.Cluster(self, "MagentoCluster", vpc=vpc)

        # Retrieve DB secret ARN
        dbsecret = sm.Secret.from_secret_complete_arn(self, "DatabaseSecret",
                                                      secret_complete_arn=database_instance.secret.secret_arn,
                                                      )
        # Create application user password
        secretMagentoUser = sm.Secret(self,  'passwordMagentoUser', secret_name = "passwordMagentoUser")
        
        # Create an application load balancer.
        # WARNING : This example is provided as a sample and rely on HTTP, which should never be used in production system. To enable HTTPS/Encrypted connection, look for ## HTTPS Configuration ## , follow the guidance and replace the missing inputs
        magento_lb = elbv2.ApplicationLoadBalancer(self, "MagentoLB", vpc=vpc, internet_facing=True, load_balancer_name="magento-lb") 

        ## HTTPS Configuration ##
        # The following code is to activate the HTTPS/Encrypted connection on the load balancer
        # Load an existing certificate from AWS Certificate Manager
        #certificate = cm.Certificate.from_certificate_arn(self, "certificate", #CERTIFICATE_ARN#)
        ## END HTTPS Configuration ##
       
       # Create Fargate service running on an ECS cluster fronted by the application load balancer.
        self.magento_service = ecs_patterns.ApplicationLoadBalancedFargateService(self, "MagentoService",
                                                                              cluster=_cluster,            # Required
                                                                              cpu=2048,                    # Default is 256
                                                                              task_image_options=ecs_patterns.ApplicationLoadBalancedTaskImageOptions(
                                                                                  image=ecs.ContainerImage.from_registry(
                                                                                      "public.ecr.aws/bitnami/magento:2.4.4"),  # return RepositoryImage
                                                                                  environment={
                                                                                    "BITNAMI_DEBUG": "true",
                                                                                    "MAGENTO_EXTRA_INSTALL_ARGS": "--enable-debug-logging=true",
                                                                                    "MAGENTO_DATABASE_HOST": database_instance.db_instance_endpoint_address,
                                                                                    "MAGENTO_DATABASE_PORT_NUMBER": "3306",
                                                                                    "MAGENTO_DATABASE_NAME": "MagentoDB",
                                                                                    "MAGENTO_ELASTICSEARCH_HOST":es_domain.domain_endpoint,
                                                                                    "MAGENTO_ELASTICSEARCH_PORT_NUMBER": "443",
                                                                                    "MAGENTO_ELASTICSEARCH_USE_HTTPS": "yes",
                                                                                    "MAGENTO_SEARCH_ENGINE": "elasticsearch7",
                                                                                    "MAGENTO_DEPLOY_STATIC_CONTENT": "yes",
                                                                                    "MAGENTO_EXTERNAL_HTTP_PORT_NUMBER": "80",
                                                                                    "APACHE_HTTP_PORT_NUMBER" : "80",
                                                                                    "MAGENTO_USERNAME": "magento_user",
                                                                                    "MAGENTO_EMAIL": "magento_user@example.com",

                                                                                    ## HTTPS Configuration ##
                                                                                    #The following code is to activate the HTTPS/Encrypted connection on the load balancer - if this is uncommented, please comment -> "MAGENTO_HOST": magento_lb.load_balancer_dns_name below
                                                                                    #"MAGENTO_HOST": #SUB_DOMAIN# + "." + #HOSTED_ZONE_ID# ,
                                                                                    "MAGENTO_HOST": magento_lb.load_balancer_dns_name, #Comment for HTTPS configuration
                                                                                    ## END HTTPS Configuration ##

                                                                                    # The following two variables needed to be commented for the first boot - initialization of the database and the search
                                                                                    #"MAGENTO_SKIP_BOOTSTRAP":"yes", #Keep commented for the first run
                                                                                    #"MAGENTO_SKIP_REINDEX" : "yes"  #Keep commented for the first run
                                                                                  },
                                                                                  secrets={
                                                                                      "MAGENTO_DATABASE_USER": ecs.Secret.from_secrets_manager(dbsecret, "username"),
                                                                                      "MAGENTO_DATABASE_PASSWORD": ecs.Secret.from_secrets_manager(dbsecret, "password"),
                                                                                      "MAGENTO_PASSWORD":ecs.Secret.from_secrets_manager(secretMagentoUser),
                                                                                  },
                                                                                  container_port=80
                                                                              ),
                                                                              memory_limit_mib=4096,      # Default is 512
                                                                              public_load_balancer=True,  # Default is False
                                                                              health_check_grace_period=Duration.minutes(3000),
                                                                              task_subnets=ec2.SubnetSelection(subnet_type=ec2.SubnetType.PRIVATE_WITH_NAT),
                                                                              load_balancer_name = "magento_lb",
                                                                              load_balancer = magento_lb,
                                                                              security_groups = [appSG,],
                                                                              
                                                                              ## HTTPS Configuration ##
                                                                              # The following code is to activate the HTTPS/Encrypted connection on the load balancer - if this is uncommented, please comment listener_port=80 below
                                                                              
                                                                              # certificate = certificate,
                                                                              # domain_name = #SUB_DOMAIN#, 
                                                                              # domain_zone = r53.HostedZone.from_hosted_zone_attributes(self, "httpszone", hosted_zone_id=#HOSTED_ZONE_ID#, zone_name=#HOSTED_ZONE_NAME#), 
                                                                              # listener_port=443,
                                                                              listener_port=80, #Comment for HTTPS configuration
                                                                              ## END HTTPS Configuration ##
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
        CfnOutput(self, "MagentoHostnameHTTP",
                  value="http://" + self.magento_service.load_balancer.load_balancer_dns_name)
        
        ## HTTPS Configuration ##
        # The following code is to activate the HTTPS/Encrypted connection on the load balancer - if this is uncommented, please comment listener_port=80 below
        #CfnOutput(self, "MagentoHostnameHTTPS",
        #          value="https://" + #SUB_DOMAIN# + "." + #HOSTED_ZONE_ID#)
        ## END HTTPS Configuration ##