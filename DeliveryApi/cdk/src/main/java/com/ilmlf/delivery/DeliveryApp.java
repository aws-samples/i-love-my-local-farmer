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

package com.ilmlf.delivery;

import com.ilmlf.delivery.api.ApiStack;
import com.ilmlf.delivery.db.DbStack;
import java.io.IOException;
import java.util.List;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.services.ec2.ISubnet;

/**
 * The entry point of CDK application. This class creates a CDK App with two stacks
 * 1. DbStack contains network resources (e.g. VPC, subnets), MySQL DB, DB Proxy, and secrets
 * 2. ApiStack contains API Gateway and Lambda functions for compute
 *
 * <p>
 * We separate the two stacks from each other as they have different life cycles. The ApiStack will
 * be updated more frequently while the DbStack should be rarely updated. This also allows us to
 * put different permission settings for each stack (e.g. prevent an innocent intern deleting
 * your DB accidentally).
 * </p>
 */
public class DeliveryApp {

  /**
   * Entry point of the CDK CLI.
   *
   * @param args Not used
   * @throws IOException can be thrown from ApiStack as it read and build Lambda package
   */
  public static void main(final String[] args) throws IOException {
    App app = new App();

    String dbUsername = (String) app.getNode().tryGetContext("dbUsername");
    dbUsername = (dbUsername == null ? "lambda_iam" : dbUsername);

    String dbPortStr = (String) app.getNode().tryGetContext("dbPort");
    Integer dbPort = (dbPortStr == null ? 3306: Integer.valueOf(dbPortStr));

    boolean isPublicSubnetDb = "public".equals(app.getNode().tryGetContext("subnetType"));
    boolean deployPackagingApi = "true".equals(app.getNode().tryGetContext("deployPackagingApi"));

    DbStack db = new DbStack(app, "DeliveryProject-Db", DbStack.DbStackProps.builder()
        .description("MySQL database, RDS proxy, secrets, and network components of Delivery project (uksb-1rsq7leeu)")
        .dbUsername(dbUsername)
        .dbPort(dbPort)
        .isPublicSubnetDb(isPublicSubnetDb)
        .build());

    String email = (String) app.getNode().tryGetContext("email");

    new ApiStack(
        app,
        "DeliveryProject-Api",
        ApiStack.ApiStackProps.builder()
            .description("API of Delivery project (uksb-1rsq7ledu)")
            .dbEndpoint(db.getInstanceEndpoint())
            .dbProxyEndpoint(db.getProxyEndpoint())
            .dbPort(db.getDbPort())
            .dbProxyArn(db.getProxyArn())
            .dbRegion(db.getRegion())
            .dbUser(db.getDbUsername())
            .dbUserSecretName(db.getUserSecret().getSecretName())
            .dbUserSecretArn(db.getUserSecret().getSecretArn())
            .dbAdminSecretName(db.getAdminSecret().getSecretName())
            .dbAdminSecretArn(db.getAdminSecret().getSecretArn())
            .dbSg(db.getSecurityGroup())
            .dbVpc(db.getVpc())
            .alertEmail(email)
            .deployPackagingApi(deployPackagingApi)
            .build());

    app.synth();
  }
}
