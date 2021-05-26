package com.ilmlf.delivery.api;

import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awscdk.core.App;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.core.Environment;

import static io.github.jsonSnapshot.SnapshotMatcher.expect;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiTest {
  private static final ObjectMapper JSON =
      new ObjectMapper()
          .configure(SerializationFeature.INDENT_OUTPUT, true)
          .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  @BeforeAll
  public static void beforeAll() {
    SnapshotMatcher.start(Snapshot::asJsonString);
  }

  @AfterAll
  public static void afterAll() {
    SnapshotMatcher.validateSnapshots();
  }

  @Test
  public void testStack() throws IOException {
    App app = new App();

    ApiStack stack =
        new ApiStack(
            app,
            "test",
            ApiStack.ApiStackProps.builder()
                .env(Environment.builder()
                    .account("account-test")
                    .region("region-test")
                    .build())
                .dbEndpoint("farmerdb.cyo3qc04zrsp.eu-west-1.rds.amazonaws.com:3306")
                .dbProxyArn("arn:aws:rds:eu-west-1:01234567809:db-proxy:prx-test")
                .dbProxyEndpoint(
                    "farmerdbproxy.proxy-test.eu-west-1.rds.amazonaws.com:3306/FarmerDB")
                .dbRegion("eu-west-1")
                .dbUser("lambda_iam")
                .dbUserSecretName("DbUserSecret")
                .dbUserSecretArn("arn:aws:secretsmanager:eu-west-1:01234567809:secret:DbUserSecret-test")
                .dbAdminSecretArn(
                    "arn:aws:secretsmanager:eu-west-1:01234567809:secret:DeliveryProjectDbFarmerDBSe-test-test")
                .dbAdminSecretName("DeliveryProjectDbFarmerDBSe-test")
                .dbSgId("sg-test")
                .dbVpcId("vpc-test")
                .build());

    // synthesize the stack to a CloudFormation template
    JsonNode actual =
        JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());

    expect(actual).toMatchSnapshot();
  }
}
