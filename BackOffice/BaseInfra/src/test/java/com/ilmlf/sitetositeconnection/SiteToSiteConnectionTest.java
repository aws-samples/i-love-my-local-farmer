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

package com.ilmlf.sitetositeconnection;


import io.github.jsonSnapshot.SnapshotMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awscdk.core.App;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import static io.github.jsonSnapshot.SnapshotMatcher.expect;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.core.AppProps;


public class SiteToSiteConnectionTest {
  private final static ObjectMapper JSON =
      new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

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
    App app = new App(AppProps.builder().context(Map.of(
        "customerGatewayDeviceIP", "34.244.24.124",
        "vpcCidr", "172.30.0.0/16",
        "onPremiseCidr", "172.16.0.0/16"
    )).build());
    SiteToSiteConnectionStack stack = new SiteToSiteConnectionStack(app, "test");

    // synthesize the stack to a CloudFormation template
    JsonNode actual =
        JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());

    expect(actual).toMatchSnapshot();

  }
}

