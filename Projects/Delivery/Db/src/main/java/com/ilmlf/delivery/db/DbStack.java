package com.ilmlf.delivery.db;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.CfnOutputProps;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SecurityGroupProps;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.DatabaseInstanceProps;
import software.amazon.awscdk.services.rds.DatabaseProxy;
import software.amazon.awscdk.services.rds.DatabaseProxyProps;
import software.amazon.awscdk.services.rds.MySqlInstanceEngineProps;
import software.amazon.awscdk.services.rds.MysqlEngineVersion;
import software.amazon.awscdk.services.rds.ProxyTarget;
import software.amazon.awscdk.services.rds.StorageType;

public class DbStack extends Stack {
  public DbStack(final Construct scope, final String id) {
    this(scope, id, null);
  }

  public DbStack(final Construct scope, final String id, final StackProps props) {
    super(scope, id, props);

    String region = Stack.of(this).getRegion();

    Vpc vpc = new Vpc(this, "farmer-Vpc");
    SecurityGroup farmerDbSG =
        new SecurityGroup(
            this,
            "FarmerDeliverySG",
            SecurityGroupProps.builder()
                .vpc(vpc)
                .description("Shared SG for database and proxy")
                .allowAllOutbound(true)
                .build());

    farmerDbSG.addIngressRule(
        SecurityGroup.fromSecurityGroupId(
            this, "inboundDbAccessSecurityGroupFarmer", farmerDbSG.getSecurityGroupId()),
        Port.tcp(3306));

    String dbName = "FarmerDB";
    DatabaseInstance farmerDb =
        new DatabaseInstance(
            this,
            dbName,
            DatabaseInstanceProps.builder()
                .vpc(vpc)
                .securityGroups(List.of(farmerDbSG))
                .instanceIdentifier(dbName)
                .engine(
                    DatabaseInstanceEngine.mysql(
                        MySqlInstanceEngineProps.builder()
                            .version(MysqlEngineVersion.VER_5_7_31)
                            .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.SMALL))
                .vpcSubnets(SubnetSelection.builder().subnets(vpc.getPublicSubnets()).build())
                .storageEncrypted(true)
                .multiAz(true)
                .autoMinorVersionUpgrade(true)
                .allocatedStorage(25)
                .storageType(StorageType.GP2)
                .backupRetention(Duration.days(7))
                .deletionProtection(false)
                .credentials(Credentials.fromGeneratedSecret(dbName + "admin"))
                .databaseName(dbName)
                .port(3306)
                .build());

    // RDS Proxy
    DatabaseProxy dbProxy =
        new DatabaseProxy(
            this,
            "FarmerDbProxy",
            DatabaseProxyProps.builder()
                .borrowTimeout(Duration.seconds(30))
                .maxConnectionsPercent(50)
                .secrets(List.of(farmerDb.getSecret()))
                .proxyTarget(ProxyTarget.fromInstance(farmerDb))
                .vpc(vpc)
                .securityGroups(List.of(farmerDbSG))
                .build());

    TreeMap<String, String> configJsonContents = new TreeMap<>();
    configJsonContents.put("DB_REGION", region);
    configJsonContents.put("DB_PROXY_ENDPOINT", dbProxy.getEndpoint() + ":3306/" + dbName);
    configJsonContents.put("DB_VPC_ID", vpc.getVpcId());
    configJsonContents.put("DB_SG_ID", farmerDbSG.getSecurityGroupId());
    configJsonContents.put("DB_SECRET_NAME", farmerDb.getSecret().getSecretName());
    configJsonContents.put("DB_SECRET_ARN", farmerDb.getSecret().getSecretArn());
    configJsonContents.put("DB_PROXY_ARN", dbProxy.getDbProxyArn());

    new CfnOutput(
        this,
        "configJsonContents",
        CfnOutputProps.builder()
            .description("Config file (in json format) for Stacks with Lambdas")
            .value(configJsonContents.toString())
            .build());
  }
}
