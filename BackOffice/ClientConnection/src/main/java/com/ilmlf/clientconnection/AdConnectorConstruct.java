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

package com.ilmlf.clientconnection;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.core.BundlingOutput.ARCHIVED;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.BundlingOptions;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.CustomResource;
import software.amazon.awscdk.core.CustomResourceProps;
import software.amazon.awscdk.core.DockerVolume;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.customresources.Provider;
import software.amazon.awscdk.customresources.ProviderProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.PolicyStatementProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;

/**
 * Custom resource to create AD Connector.
 */
public class AdConnectorConstruct extends Construct {

  public final String directoryId;

  /**
   * AD Connector construct's required properties.
   */
  @lombok.Builder
  @Data
  public static class AdConnectorProps {

    private String vpcId;
    private String domainName;
    private String secretId;
    private List<String> dnsIps;
    private List<String> subnetIds;
  }

  /**
   * AD Connector construct.

   * @param scope CDK scope
   * @param id construct Id
   * @param props AdConnector Properties
   */
  public AdConnectorConstruct(
      software.constructs.@NotNull Construct scope, @NotNull String id, AdConnectorProps props) {
    super(scope, id);


    List<String> adConnectorCustomResourcePackagingInstructions = Arrays.asList(
        "/bin/sh",
        "-c",
        "mvn clean install "
        + "&& cp /asset-input/target/AdConnectorCustomResource.jar /asset-output/"
    );


    BundlingOptions.Builder builderOptions = BundlingOptions.builder()
        .command(adConnectorCustomResourcePackagingInstructions)
        .image(Runtime.JAVA_11.getBundlingImage())
        .volumes(singletonList(
            // Mount local .m2 repo to avoid download all the deps again inside the container
            DockerVolume.builder()
                .hostPath(System.getProperty("user.home") + "/.m2/")
                .containerPath("/root/.m2/")
                .build()
        ))
        .user("root")
        .outputType(ARCHIVED);

    Function onEventHandler = new Function(this, "onEventHandler", FunctionProps.builder()
        .runtime(Runtime.JAVA_11)
        .code(Code.fromAsset("./AdConnectorCustomResource", AssetOptions.builder()
            .bundling(builderOptions
                // TODO: add capability to use local bundling (.local) instead of docker one
                .command(adConnectorCustomResourcePackagingInstructions)
                .build())
            .build()))
        .handler("com.ilmlf.adconnector.customresource.OnEventHandler")
        .environment(Map.of(
            "POWERTOOLS_LOG_LEVEL", "DEBUG",
            "POWERTOOLS_SERVICE_NAME", "com.ilmlf.adconnector.customresource.OnEventHandler"
        ))
        .memorySize(1024)
        .timeout(Duration.seconds(10))
        .logRetention(RetentionDays.ONE_WEEK)
        .build());

    onEventHandler.addToRolePolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .actions(singletonList("secretsmanager:GetSecretValue"))
                .resources(Collections.singletonList(props.secretId))
                .build()
        )
    );

    onEventHandler.addToRolePolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .actions(Arrays.asList(
                    "secretsmanager:GetSecretValue",
                    "ds:ConnectDirectory",
                    "ds:DeleteDirectory",
                    "ec2:DescribeSubnets",
                    "ec2:DescribeVpcs",
                    "ec2:CreateSecurityGroup",
                    "ec2:CreateNetworkInterface",
                    "ec2:DescribeNetworkInterfaces",
                    "ec2:AuthorizeSecurityGroupIngress",
                    "ec2:AuthorizeSecurityGroupEgress",
                    "ec2:CreateTags"
                ))
                .resources(Collections.singletonList("*"))
                .build()
        )
    );


    Function isCompleteHandler = new Function(this, "isCompleteHandler", FunctionProps.builder()
        .runtime(Runtime.JAVA_11)
        .code(Code.fromAsset("./AdConnectorCustomResource",
            AssetOptions.builder()
                .bundling(builderOptions
                    .command(adConnectorCustomResourcePackagingInstructions)
                    .build())
                .build()))
        .handler("com.ilmlf.adconnector.customresource.IsCompleteHandler")
        .environment(Map.of(
            "POWERTOOLS_LOG_LEVEL", "DEBUG",
            "POWERTOOLS_SERVICE_NAME", "com.ilmlf.adconnector.customresource.IsCompleteHandler"
        ))
        .memorySize(1024)
        .timeout(Duration.seconds(10))
        .logRetention(RetentionDays.ONE_WEEK)
        .build());

    isCompleteHandler.addToRolePolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .actions(singletonList("ds:DescribeDirectories"))
                .resources(Collections.singletonList("*"))
                .build()
        )
    );


    Provider provider = new Provider(scope, "adConnectorProvider", ProviderProps.builder()
        .onEventHandler(onEventHandler)
        .isCompleteHandler(isCompleteHandler)
        .build()
    );

    CustomResource resource = new CustomResource(scope, "ADConnector", CustomResourceProps.builder()
        .serviceToken(provider.getServiceToken())
        .resourceType("Custom::ADConnector")
        .properties(Map.of(
            "vpcId", props.vpcId,
            "domainName", props.domainName,
            "dnsIps", props.dnsIps,
            "subnetIds", props.subnetIds,
            "secretId", props.secretId
        ))
        .build()
    );
    this.directoryId = resource.getAttString("DirectoryId");
  }
}
