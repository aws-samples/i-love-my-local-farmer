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

import static io.github.jsonSnapshot.SnapshotMatcher.expect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ilmlf.delivery.api.ApiStack;
import com.ilmlf.delivery.db.DbStack;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

/**
 * Test Home Delivery CDK app via snapshot testing.
 *
 * The test synthesizes a JSON CloudFormation template and verifies if it has been changed from the previous version.
 * (which was saved as DeliveryAppTest.snap)
 * If there is any change, the run will fail with "io.github.jsonSnapshot.SnapshotMatchException"
 *
 * This mechanism forces us to verify that there is no unintended change on the infrastructure when refactoring the code.
 * In the code review, we can see the changes in the diff and manually verify that.
 *
 * To update the snapshot, you have to manually delete `DeliveryAppTest.snap` and rerun the test.
 *
 * For convenience, removal of the snapshot is done automatically within the `removeSnapshot()` task in build.gradle.
 * We recommend that developers use `cdk diff` to inspect changes to their infrastructure before attempting to deploy.
 * However, for extra precaution the `removeSnapshot()` task can be omitted and developers can be forced to manually
 * remove the snapshot so they don't forget to inspect any changes.
 *
 * See https://json-snapshot.github.io/ for more details on the Snapshot library
 */
public class DeliveryAppTest {
  private static final String AWS_ACCOUNT_ID =  "123456789012";
  public static final String AWS_REGION = "eu-west-1";
  private static final ObjectMapper JSON =
      new ObjectMapper()
          .configure(SerializationFeature.INDENT_OUTPUT, true)
          .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  /**
   * Reference to the application.
   */
  private static App app;

  /**
   * Reference to the API stack. Note that we only render it once and reuse it for all snapshot test.
   */
  private static ApiStack api;

  /**
   * Reference to the DB stack. Note that we only render it once and reuse it for all snapshot test.
   */
  private static DbStack db;

  /**
   * Start snapshot testing and initialize the CDK application and all stacks.
   */
  @BeforeAll
  public static void beforeAll() throws IOException {
    SnapshotMatcher.start(Snapshot::asJsonString);

    app = new App();

    db = new DbStack(app, "DeliveryProject-Db", DbStack.DbStackProps.builder()
        .env(Environment.builder()
            .account(AWS_ACCOUNT_ID)
            .region(AWS_REGION)
            .build())
        .dbUsername("lambda_iam")
        .dbPort(3306)
        .build());

    api = new ApiStack(
        app,
        "DeliveryProject-Api",
        ApiStack.ApiStackProps.builder()
            .env(Environment.builder()
                .account(AWS_ACCOUNT_ID)
                .region(AWS_REGION)
                .build())
            .dbEndpoint(db.getInstanceEndpoint())
            .dbProxyEndpoint(db.getProxyEndpoint())
            .dbProxyArn(db.getProxyArn())
            .dbPort(db.getDbPort())
            .dbRegion(db.getRegion())
            .dbUser(db.getDbUsername())
            .dbUserSecretName(db.getUserSecret().getSecretName())
            .dbUserSecretArn(db.getUserSecret().getSecretArn())
            .dbAdminSecretName(db.getAdminSecret().getSecretName())
            .dbAdminSecretArn(db.getAdminSecret().getSecretArn())
            .dbSg(db.getSecurityGroup())
            .dbVpc(db.getVpc())
            .build());
  }

  @AfterAll
  public static void afterAll() {
    SnapshotMatcher.validateSnapshots();
  }

  /**
   * Verify that the DB stack hasn't been changed.
   * @throws IOException
   */
  @Test
  public void testDbStack() {

    // synthesize the stack to a CloudFormation template
    JsonNode dbActual =
        JSON.valueToTree(app.synth().getStackArtifact(db.getArtifactId()).getTemplate());

    expect(dbActual).toMatchSnapshot();
  }

  /**
   * Verify that the API Stack hasn't been changed.
   * @throws IOException
   */
  @Test
  public void testApiStack() {
    // synthesize the stack to a CloudFormation template
    JsonNode apiActual =
        JSON.valueToTree(app.synth().getStackArtifact(api.getArtifactId()).getTemplate());

    expect(apiActual).toMatchSnapshot();
  }
}
