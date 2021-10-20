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

package com.ilmlf.product;

import com.ilmlf.product.cicd.PipelineStack;
import com.ilmlf.product.cicd.PipelineStack.ProvmanEnv;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import software.amazon.awscdk.core.Annotations;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Construct;

/**
 * The entry point of CDK application.
 * This build a pipeline stack that will deploy the DB and API stacks 
 * of the application in the different stages specificed in deployableEnvironments.
 *
 * Configuration:
 * <ul>
 * <li>The Account hosting the CI/CD Pipeline created can be different from the stages accounts.</li>
 * <li>To specify the account hosting the CI/CD Pipeline, specify the following attributes in cdk.context.json: "CICD_ENV".</li>
 * <li>To specify the stages accounts, specify the following attributes in cdk.context.json: "QA_ENV" and "PROD_ENV".</li>
 * <li>To enable prod stage uncomment it's declaration below.</li>
 * </ul>
 *
 */
public class ProvmanApp {

  /**
   * Helper method to build an environment.
   * @param scope like app, construct etc.
   * @param contextKey referencing the env json object with region and account attributes.
   * @throws IOException can be thrown from ApiStack as it read and build Lambda package.
   */
  static ProvmanEnv makeEnv(Construct scope, String contextKey) {

    Map<String, String> envParamsFromContext = (Map<String,String>) scope.getNode().tryGetContext(contextKey);
    if(envParamsFromContext != null) {
      return ProvmanEnv.builder()
              .account(envParamsFromContext.get("account"))
              .region(envParamsFromContext.get("region"))
              .name(envParamsFromContext.get("name"))
              .build();
    } else {
      Annotations.of(scope).addWarning(String.format("No environment params found in context for env {}, using default", contextKey));
      return ProvmanEnv.builder()
              .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
              .region(System.getenv("CDK_DEFAULT_REGION"))
              .name(contextKey.replace("_", "-"))
              .build();
    }

  }

  /**
   * Entry point of the CDK CLI.
   * @param args
   * @throws Exception
   */
  public static void main(final String[] args) throws Exception {
    App app = new App();

    Set<ProvmanEnv> deployableEnvironments = new HashSet<>();

    ProvmanEnv cicd = makeEnv(app, "CICD_ENV");
    ProvmanEnv qa = makeEnv(app, "QA_ENV");
    deployableEnvironments.add(qa);

    // ** UNCOMMENT TO ENABLE PROD STAGE **
    // Environment prod = makeEnv(app, "PROD_ENV");
    // deployableEnvironments.add(prod);

    new PipelineStack(app, "Pipeline", PipelineStack.PipelineStackProps.builder()
        .env(cicd)
        .stageEnvironments(deployableEnvironments)
        .build());

    app.synth();
  }
}