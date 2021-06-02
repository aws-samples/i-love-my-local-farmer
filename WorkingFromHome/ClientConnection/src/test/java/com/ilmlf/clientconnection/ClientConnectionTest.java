package com.ilmlf.clientconnection;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.jsonSnapshot.SnapshotMatcher;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.AppProps;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import java.util.Arrays;
import java.util.Map;

import static io.github.jsonSnapshot.SnapshotMatcher.expect;

public class ClientConnectionTest {
  private final static ObjectMapper JSON =
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
    App app = new App(AppProps.builder()
        .context(Map.of(
            "vpcId", "vpc-013f90d3d71400c88",
            "onPremiseCidr", "172.16.0.0/16",
            "domain", "ilovemylocalfarmer.com",
            "dns", Arrays.asList("172.16.0.94", "172.16.0.126"),
            "clientVpnCidr", "172.31.0.0/16",
            "clientVpnCertificate",
            "arn:aws:acm:eu-west-1:433621526002:certificate/f09c1fca-1ffd-4768-b4e6-7f424f2f7c61",
            "DomainAdminSecretArn",
            "arn:aws:secretsmanager:eu-west-1:053319678981:secret:DomainAdminPassword-qWxv2k"

        ))
        .build());

    Map<String, String> env = Map.of(
        "account", "account-test",
        "region", "region-test"
    );

    ClientConnectionStack stack = new ClientConnectionStack(app, "test", StackProps.builder()
        .env(Environment.builder()
            .account("account-test")
            .region("region-test")
            .build())
        .build());

    // synthesize the stack to a CloudFormation template
    JsonNode actual =
        JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());

    expect(actual).toMatchSnapshot();

  }
}

