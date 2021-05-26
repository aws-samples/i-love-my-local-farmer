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

public class DeliveryAppTest {
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
  public void testDbStack() throws IOException {
    App app = new App();

    DbStack db = new DbStack(app, "DeliveryProject-Db", StackProps.builder()
        .env(Environment.builder()
            .account("433621526002")
            .region("eu-west-1")
            .build())
        .build());

    ApiStack api = new ApiStack(
        app,
        "DeliveryProject-Api",
        ApiStack.ApiStackProps.builder()
            .env(Environment.builder()
                .account("433621526002")
                .region("eu-west-1")
                .build())
            .dbEndpoint(db.getInstanceEndpoint())
            .dbProxyEndpoint(db.getProxyEndpoint())
            .dbProxyArn(db.getProxyArn())
            .dbRegion(db.getRegion())
            .dbUser(db.getUser())
            .dbUserSecretName(db.getUserSecret().getSecretName())
            .dbUserSecretArn(db.getUserSecret().getSecretArn())
            .dbAdminSecretName(db.getAdminSecret().getSecretName())
            .dbAdminSecretArn(db.getAdminSecret().getSecretArn())
            .dbSg(db.getSecurityGroup())
            .dbVpc(db.getVpc())
            .build());

    // synthesize the stack to a CloudFormation template
    JsonNode dbActual =
        JSON.valueToTree(app.synth().getStackArtifact(db.getArtifactId()).getTemplate());

//    JsonNode apiActual =
//        JSON.valueToTree(app.synth().getStackArtifact(api.getArtifactId()).getTemplate());

    expect(dbActual).toMatchSnapshot();
//    expect(apiActual).toMatchSnapshot();
  }

  @Test
  public void testApiStack() throws IOException {
    App app = new App();

    DbStack db = new DbStack(app, "DeliveryProject-Db", StackProps.builder()
        .env(Environment.builder()
            .account("433621526002")
            .region("eu-west-1")
            .build())
        .build());

    ApiStack api = new ApiStack(
        app,
        "DeliveryProject-Api",
        ApiStack.ApiStackProps.builder()
            .env(Environment.builder()
                .account("433621526002")
                .region("eu-west-1")
                .build())
            .dbEndpoint(db.getInstanceEndpoint())
            .dbProxyEndpoint(db.getProxyEndpoint())
            .dbProxyArn(db.getProxyArn())
            .dbRegion(db.getRegion())
            .dbUser(db.getUser())
            .dbUserSecretName(db.getUserSecret().getSecretName())
            .dbUserSecretArn(db.getUserSecret().getSecretArn())
            .dbAdminSecretName(db.getAdminSecret().getSecretName())
            .dbAdminSecretArn(db.getAdminSecret().getSecretArn())
            .dbSg(db.getSecurityGroup())
            .dbVpc(db.getVpc())
            .build());

    // synthesize the stack to a CloudFormation template
    JsonNode apiActual =
        JSON.valueToTree(app.synth().getStackArtifact(api.getArtifactId()).getTemplate());

    expect(apiActual).toMatchSnapshot();
  }
}
