/*
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.ilmlf.delivery.db;

import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import lombok.Getter;
import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.CfnOutputProps;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SecurityGroupProps;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.PolicyStatementProps;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;
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
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretProps;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;

@Getter
public class DbStack extends Stack {
  private final String user = "lambda_iam";
  private final String instanceEndpoint;
  private final String proxyEndpoint;
  private final Vpc vpc;
  private final SecurityGroup securityGroup;
  private final ISecret adminSecret;
  private final String proxyArn;
  private final Secret userSecret;


  public DbStack(final Construct scope, final String id) {
    this(scope, id, null);
  }

  public DbStack(final Construct scope, final String id, final StackProps props) {
    super(scope, id, props);

    String region = Stack.of(this).getRegion();

    this.vpc = new Vpc(this, "farmer-Vpc");
    this.securityGroup =
        new SecurityGroup(
            this,
            "FarmerDeliverySG",
            SecurityGroupProps.builder()
                .vpc(vpc)
                .description("Shared SG for database and proxy")
                .allowAllOutbound(true)
                .build());

    String dbName = "FarmerDB";

    DatabaseInstance farmerDb =
        new DatabaseInstance(
            this,
            dbName,
            DatabaseInstanceProps.builder()
                .vpc(vpc)
                .securityGroups(List.of(securityGroup))
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
                .publiclyAccessible(true)
                .storageType(StorageType.GP2)
                .backupRetention(Duration.days(7))
                .deletionProtection(false)
                .credentials(Credentials.fromGeneratedSecret(dbName + "admin"))
                .databaseName(dbName)
                .port(3306)
                .build());

    this.userSecret = new Secret(this, "DbUserSecret",
        SecretProps.builder()
            .description("Db Username and password")
            .secretName("DbUserSecret")
            .generateSecretString(
                SecretStringGenerator.builder()
                    .secretStringTemplate("{\"username\": \"" + this.user + "\"}")
                    .generateStringKey("password")
                    .passwordLength(16)
                    .excludePunctuation(true)
                    .build()
            )
            .build()
    );
    
    // RDS Proxy

    Role rdsProxyRole = new Role(this, "RdsProxyRole",
        RoleProps.builder().assumedBy(new ServicePrincipal("rds.amazonaws.com")).build());

    rdsProxyRole.addToPolicy(new PolicyStatement(PolicyStatementProps.builder()
        .effect(Effect.ALLOW)
        .actions(Collections.singletonList("kms:Decrypt"))
        .resources(Collections.singletonList(userSecret.getSecretArn()))
        .build()));


    DatabaseProxy dbProxy =
        new DatabaseProxy(
            this,
            "FarmerDbProxy",
            DatabaseProxyProps.builder()
                .borrowTimeout(Duration.seconds(30))
                .maxConnectionsPercent(50)
                .secrets(List.of(farmerDb.getSecret(), userSecret))
                .role(rdsProxyRole)
                .proxyTarget(ProxyTarget.fromInstance(farmerDb))
                .vpc(vpc)
                .securityGroups(List.of(securityGroup))
                .iamAuth(true)
                .requireTls(true)
                .build());

    this.proxyEndpoint = dbProxy.getEndpoint();
    this.proxyArn = dbProxy.getDbProxyArn();
    this.instanceEndpoint = farmerDb.getDbInstanceEndpointAddress() + ":" + farmerDb.getDbInstanceEndpointPort();
    this.adminSecret = farmerDb.getSecret();

    TreeMap<String, String> configJsonContents = new TreeMap<>();
    configJsonContents.put("DB_REGION", region);
    configJsonContents.put("DB_PROXY_ENDPOINT", dbProxy.getEndpoint() + ":3306/" + dbName);
    configJsonContents.put("DB_ENDPOINT", farmerDb.getDbInstanceEndpointAddress() + ":" + farmerDb.getDbInstanceEndpointPort());
    configJsonContents.put("DB_VPC_ID", vpc.getVpcId());
    configJsonContents.put("DB_SG_ID", securityGroup.getSecurityGroupId());
    configJsonContents.put("DB_USER", this.user);
    configJsonContents.put("DB_USER_SECRET", this.userSecret.getSecretName());
    configJsonContents.put("DB_USER_SECRET_ARN", this.userSecret.getSecretArn());
    configJsonContents.put("DB_ADMIN_SECRET", farmerDb.getSecret().getSecretName());
    configJsonContents.put("DB_ADMIN_SECRET_ARN", farmerDb.getSecret().getSecretArn());
    configJsonContents.put("DB_PROXY_ARN", dbProxy.getDbProxyArn());

    // TODO: Fix to Json with ordering using GSON ? https://stackoverflow.com/a/29491895/1331590
    new CfnOutput(
        this,
        "configJsonContents",
        CfnOutputProps.builder()
            .description("Config file (in json format) for Stacks with Lambdas")
            .value(configJsonContents.toString())
            .build());
  }
}
