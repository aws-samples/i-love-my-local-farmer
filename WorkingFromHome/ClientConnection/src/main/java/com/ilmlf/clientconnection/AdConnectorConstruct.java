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

    /*
      Java CDK custom resources need to be packaged for use
      Packaging instructions are provided here
    */
    List<String> adConnectorCustomResourcePackagingInstructions =
        Arrays.asList(
            "/bin/sh",
            "-c",
            "mvn clean install "
                + "&& cp /asset-input/target/AdConnectorCustomResource.jar /asset-output/");

    /*
      The package builder is configured...
      - using our previous packaging instructions
      - to use a Java 11 run time
    */
    BundlingOptions.Builder builderOptions =
        BundlingOptions.builder()
            .command(adConnectorCustomResourcePackagingInstructions)
            .image(Runtime.JAVA_11.getBundlingImage())
            .user("root")
            .outputType(ARCHIVED);

    /*
      The onEventHandler function makes API calls to create AWS resources
      Here we specify:
      - that our OnEventHandler.java is defined in the ./AdConnectorCustomResource directory
      - that the resulting AWS Lambda function should:
        - have a memory size of 1024 MB
        - time out after 10 seconds
    */
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
                                    .command(adConnectorCustomResourcePackagingInstructions)
                                    .build())
                            .build()))
                .handler("com.ilmlf.adconnector.customresource.OnEventHandler")
                .memorySize(1024)
                .timeout(Duration.seconds(10))
                .logRetention(RetentionDays.ONE_WEEK)
                .build());

    /*
      Our OnEventHandler needs to access our DomainAdminPassword secret.
      Hence, we grant the GetSecretValue permission to the lambda function.
    */
    onEventHandler.addToRolePolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .actions(singletonList("secretsmanager:GetSecretValue"))
                .resources(Collections.singletonList(props.secretId))
                .build()));

    /*
      Our OnEventHandler also needs to make various VPC API calls.
      The VPC service is located under EC2, and hence most of the
      permissions we grant are of the form ec2:*
    */
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

    /*
      The isCompleteHandler is a function that is periodically called to check
      if the resource creation processes initiated in onEventHandler have completed.
      Here we are defining it in a similar way to how we defined the OnEventHandler component.
    */
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

    /*
      The IsCompleteHandler checks if the Active Directory connection has
      completed by checking if any Active Directories are listed in the Directory Service.
      Hence we must grant the Directory Service's 'DescribeDirectories' permission.
    */
    isCompleteHandler.addToRolePolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .actions(singletonList("ds:DescribeDirectories"))
                .resources(Collections.singletonList("*"))
                .build()));

    /*
      The Provider associates the onEvent and isComplete handlers to the custom resource.
    */
    Provider provider =
        new Provider(
            scope,
            "adConnectorProvider",
            ProviderProps.builder()
                .onEventHandler(onEventHandler)
                .isCompleteHandler(isCompleteHandler)
                .build());

    /*
      Define the properties that will be passed to our lambda handler functions.
      - vpcId is the identifier of the VPC in which to create the AD Connector
      - domainName is the Active Directory Domain Name
      - dnsIps is a list of IP addresses for the DNS hosts of the on-premise infrastructure
      - subnetIds are the identifiers of the subnets in which to place the AD connector
      - secretId is the ID of the Secrets Manager 'DomainAdminPassword' secret
    */
    TreeMap resourceProperties = new TreeMap();
    resourceProperties.put("vpcId", props.vpcId);
    resourceProperties.put("domainName", props.domainName);
    resourceProperties.put("dnsIps", props.dnsIps);
    resourceProperties.put("subnetIds", props.subnetIds);
    resourceProperties.put("secretId", props.secretId);

    /*
      Finally, create the CDK Custom Resource using all the previous parts we defined.
    */
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
