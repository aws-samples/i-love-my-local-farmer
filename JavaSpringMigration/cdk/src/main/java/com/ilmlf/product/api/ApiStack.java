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

package com.ilmlf.product.api;

import java.util.Map;

import lombok.Data;
import software.amazon.awscdk.core.Annotations;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Fn;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecr.assets.DockerImageAssetProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ClusterProps;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateServiceProps;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.secretsmanager.Secret;

public class ApiStack extends Stack {

  @lombok.Builder
  @Data
  public static class ApiStackProps implements StackProps {
    private String jdbcEndpointUrl;
    private String jdbcUsername;
    private String jdbcSecretArn;
    private String dbName;
    private Vpc vpc;
  }

  public ApiStack(final Construct scope, final String id, final ApiStackProps props) throws Exception {
    super(scope, id, props);

    DockerImageAsset imageAsset = this.createContainerImage();

    Cluster cluster = new Cluster(this, "ProvmanCluster", ClusterProps.builder()
        .vpc(props.vpc)
        .clusterName("ProvmanCluster")
        .build());

    ISecret dbPasswordSecret = Secret.fromSecretCompleteArn(this, "DbPasswordSecret", Fn.importValue(props.jdbcSecretArn));

    ApplicationLoadBalancedFargateService fargateService = new ApplicationLoadBalancedFargateService(this, "FargateService",
        ApplicationLoadBalancedFargateServiceProps.builder()
            .cluster(cluster)
            .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                .image(ContainerImage.fromDockerImageAsset(imageAsset))
                .containerPort(8080)
                .enableLogging(true)
                .secrets(Map.of("DB_PASSWORD",
                    software.amazon.awscdk.services.ecs.Secret.fromSecretsManager(dbPasswordSecret)))
                .environment(Map.of(
                    "DB_USERNAME", props.jdbcUsername,
                    "ENDPOINT_URL", "jdbc:mysql://" + Fn.importValue(props.jdbcEndpointUrl) + "/" + props.dbName
                ))
                .build())
            .desiredCount(1)
            .publicLoadBalancer(true)
            .serviceName("FargateEcsService")
            .build());

  }

  /**
   * Try to bundle the package locally. CDK can use this method to build locally
   * (which is faster). If the build doesn't work, it will build within a Docker
   * image which should work regardless of local environment.
   *
   * Note that CDK expects this function to return either true or false based on
   * bundling result.
   *
   * @param outputPath
   * @return whether the bundling script was successfully executed
   * @throws Exception
   */
  private Boolean buildWar(String outputPath) throws Exception {
    try {
      ProcessBuilder pb = new ProcessBuilder("bash", "-c",
          "cd ../app && mvn package && cp target/provman.war " + outputPath).inheritIO();

      Process p = pb.start(); // Start the process.
      p.waitFor(); // Wait for the process to finish.

      if (p.exitValue() == 0) {
        System.out.println("Script executed successfully");
        return true;
      } else {
        System.out.println("Script executed failed");
        throw new Error("Script executed failed");
      }

    } catch (Exception e) {
      e.printStackTrace();
      Annotations.of(this).addError(e.getMessage());
      throw e;
    }
  }

  public DockerImageAsset createContainerImage() throws Exception {
    this.buildWar("../cdk/src/main/container/");

    DockerImageAsset imageAsset = new DockerImageAsset(this, "ProvmanImage",
        DockerImageAssetProps.builder().directory("./src/main/container").build());

    return imageAsset;
  }
}
