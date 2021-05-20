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

import static com.ilmlf.clientconnection.Hashing.hashDirectory;
import static java.util.Collections.singletonList;
import static software.amazon.awscdk.core.BundlingOutput.ARCHIVED;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.AssetHashType;
import software.amazon.awscdk.core.BundlingOptions;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.CustomResource;
import software.amazon.awscdk.core.CustomResourceProps;
import software.amazon.awscdk.core.DockerImage;
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

/** Custom resource to create AD Connector. */
public class AdConnectorConstruct extends Construct {

  /** The Active directory connector Id created by the construct. */
  @Getter public final String directoryId;

  /** AD Connector construct's required properties. */
  @lombok.Builder
  @Data
  public static class AdConnectorProps {

    /** VpcId hosting the AD Connector. */
    private String vpcId;

    /** AD Domain Name. */
    private String domainName;

    /** Secret manager's Secret Id of the AD domain admin password. */
    private String secretId;

    /** List of DNS hosts IPs from On Premise infrastructure. */
    private List<String> dnsIps;

    /** List of subnet ids to put the AD Connector in. */
    private List<String> subnetIds;
  }

  /**
   * AD Connector construct.
   *
   * @param scope CDK scope
   * @param id construct Id
   * @param props AdConnector Properties
   */
  public AdConnectorConstruct(
      software.constructs.@NotNull Construct scope, @NotNull String id, AdConnectorProps props)
      throws IOException {
    super(scope, id);

    List<String> adConnectorCustomResourcePackagingInstructions =
        Arrays.asList(
            "/bin/sh",
            "-c",
            "mvn clean install "
                + "&& cp /asset-input/target/AdConnectorCustomResource.jar /asset-output/");

    BundlingOptions.Builder builderOptions =
        BundlingOptions.builder()
            .command(adConnectorCustomResourcePackagingInstructions)
            .image(Runtime.JAVA_11.getBundlingImage())
            .user("root")
            .outputType(ARCHIVED);

    Function onEventHandler =
        new Function(
            this,
            "onEventHandler",
            FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(
                    Code.fromAsset(
                        "./AdConnectorCustomResource",
                        AssetOptions.builder()
                            .assetHashType(AssetHashType.CUSTOM)
                            .assetHash(hashDirectory("./AdConnectorCustomResource/src", false))
                            .bundling(
                                builderOptions
                                    // TODO: add capability to use local bundling (.local) instead
                                    // of docker one
                                    .command(adConnectorCustomResourcePackagingInstructions)
                                    .build())
                            .build()))
                .handler("com.ilmlf.adconnector.customresource.OnEventHandler")
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .logRetention(RetentionDays.ONE_WEEK)
                .build());

    onEventHandler.addToRolePolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .actions(singletonList("secretsmanager:GetSecretValue"))
                .resources(Collections.singletonList(props.secretId))
                .build()));

    onEventHandler.addToRolePolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .actions(
                    Arrays.asList(
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
                        "ec2:CreateTags"))
                .resources(Collections.singletonList("*"))
                .build()));

    Function isCompleteHandler =
        new Function(
            this,
            "isCompleteHandler",
            FunctionProps.builder()
                .runtime(Runtime.JAVA_11)
                .code(
                    Code.fromAsset(
                        "./AdConnectorCustomResource",
                        AssetOptions.builder()
                            .assetHashType(AssetHashType.CUSTOM)
                            .assetHash(hashDirectory("./AdConnectorCustomResource/src", false))
                            .bundling(
                                builderOptions
                                    .command(adConnectorCustomResourcePackagingInstructions)
                                    .build())
                            .build()))
                .handler("com.ilmlf.adconnector.customresource.IsCompleteHandler")
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .logRetention(RetentionDays.ONE_WEEK)
                .build());

    isCompleteHandler.addToRolePolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .actions(singletonList("ds:DescribeDirectories"))
                .resources(Collections.singletonList("*"))
                .build()));

    Provider provider =
        new Provider(
            scope,
            "adConnectorProvider",
            ProviderProps.builder()
                .onEventHandler(onEventHandler)
                .isCompleteHandler(isCompleteHandler)
                .build());

    TreeMap resourceProperties = new TreeMap();
    resourceProperties.put("vpcId", props.vpcId);
    resourceProperties.put("domainName", props.domainName);
    resourceProperties.put("dnsIps", props.dnsIps);
    resourceProperties.put("subnetIds", props.subnetIds);
    resourceProperties.put("secretId", props.secretId);

    CustomResource resource =
        new CustomResource(
            scope,
            "ADConnector",
            CustomResourceProps.builder()
                .serviceToken(provider.getServiceToken())
                .resourceType("Custom::ADConnector")
                .properties(resourceProperties)
                .build());
    this.directoryId = resource.getAttString("DirectoryId");
  }
}
