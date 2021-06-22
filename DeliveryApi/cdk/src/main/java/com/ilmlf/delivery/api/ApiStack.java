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

package com.ilmlf.delivery.api;

import static software.amazon.awscdk.core.BundlingOutput.ARCHIVED;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.AssetHashType;
import software.amazon.awscdk.core.BundlingOptions;
import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.CfnOutputProps;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.CustomResource;
import software.amazon.awscdk.core.CustomResourceProps;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.customresources.Provider;
import software.amazon.awscdk.customresources.ProviderProps;
import software.amazon.awscdk.services.apigateway.ApiDefinition;
import software.amazon.awscdk.services.apigateway.CfnAccount;
import software.amazon.awscdk.services.apigateway.CfnAccountProps;
import software.amazon.awscdk.services.apigateway.Cors;
import software.amazon.awscdk.services.apigateway.SpecRestApi;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.iam.CfnRole;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.PolicyStatementProps;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.CfnFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.LogGroupProps;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.sam.CfnApi;
import software.amazon.awscdk.services.sam.CfnApiProps;

public class ApiStack extends Stack {

  private final IVpc dbVpc;
  private final ISecurityGroup dbSg;
  private final Role lambdaRdsProxyRoleWithIam;

  @lombok.Builder
  @Data
  public static class ApiStackProps implements StackProps {
    private String description;
    private String dbProxyEndpoint;
    private Integer dbPort;
    private String dbProxyArn;
    private String dbEndpoint;
    private String dbAdminSecretName;
    private String dbAdminSecretArn;
    private String dbUserSecretName;
    private String dbUserSecretArn;
    private String dbUser;

    /**
     * VPC that the database is deployed to.
     */
    private Vpc dbVpc;
    private SecurityGroup dbSg;

    private String dbRegion;
    private Environment env;
  }

  public ApiStack(final Construct scope, final String id, final ApiStackProps props)
      throws IOException {
    super(scope, id, props);

    // Role for Lambda to connect to RDS Proxy via IAM authentication
    this.lambdaRdsProxyRoleWithIam = createLambdaRdsProxyRoleWithIam(props.getDbUser());

    // Role for Lambda to connect to RDS database via user/pwd authentication
    Role lambdaRdsProxyRoleWithPw = createLambdaRdsRoleWithPw(props.dbAdminSecretArn, props.dbUserSecretArn);

    this.dbVpc = props.dbVpc;
    this.dbSg = props.dbSg;

    createApiGateway(props);

    createCustomResourceToPopulateDb(props, lambdaRdsProxyRoleWithPw);
  }

  /**
   * Create a custom resource to populate tables in the database when it's first deployed.
   *
   * A Custom resource is a CloudFormation feature for executing user provided code to create/update/delete resources.
   * For resources that aren't available in CloudFormation, we can write custom code to manage their life cycle and let
   * custom resource manage the resource when the stack is created, updated or deleted.
   *
   * In this case, the custom resource will run the Lambda function handler ("PopulateFarmDb") which contains
   * code to initialize the tables.
   *
   * See https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/template-custom-resources.html for details about custom resource
   */
  private void createCustomResourceToPopulateDb(ApiStackProps props, Role lambdaRdsProxyRoleWithPw) throws IOException {
    // See https://docs.aws.amazon.com/cdk/api/latest/java/software/amazon/awscdk/customresources/package-summary.html for details on writing a Lambda function
    // and providers
    Function dbPopulatorHandler =
        DefaultLambdaRdsProxy("PopulateFarmDb", props, lambdaRdsProxyRoleWithPw);


    Provider dbPopulatorProvider =
        new Provider(
            this,
            "InvokePopulateDataProvider",
            ProviderProps.builder().onEventHandler(dbPopulatorHandler).build());

    // we will pass in the contents of the SQL File, so that any changes in the file
    // trigger an 'Update' and executes the Populator lambda (which executes the sql statement)
    String scriptFile = "../ApiHandlers/scripts/com/ilmlf/db/dbinit.sql";
    String sqlScript = new String (Files.readAllBytes( Paths.get(scriptFile)));
    
    new CustomResource(
        this,
        "PopulateDataProviderv22",
        CustomResourceProps.builder()
            .serviceToken(dbPopulatorProvider.getServiceToken())
            .resourceType("Custom::PopulateDataProvider")
            .properties(Map.of("SqlScript",sqlScript))
            .build());
  }

  /**
   * Generate a Lambda role that has permission to access the RDS database via user/password authentication.
   *
   * @param dbAdminSecretArn
   * @return
   */
  @NotNull
  private Role createLambdaRdsRoleWithPw(
      String dbAdminSecretArn,
      String dbUserSecretArn
  ) {
    Role lambdaRdsRoleWithPw =
        new Role(
            this,
            "FarmLambdaRdsRoleWithPw",
            RoleProps.builder()
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(
                    List.of(
                        // Allow Lambda to create/put logs and create/delete network interface
                        ManagedPolicy.fromAwsManagedPolicyName(
                            "service-role/AWSLambdaVPCAccessExecutionRole")))
                .build());

    // Allow Lambda to retrieve secret values from Secret Manager
    lambdaRdsRoleWithPw.addToPolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .actions(
                    List.of(
                        "secretsmanager:GetSecretValue",
                        "secretsmanager:DescribeSecret")) // needed for lambda to read the secrets
                .resources(List.of(dbAdminSecretArn, dbUserSecretArn))
                .build()));


    // Allow Lambda to decrypt secret used to connect to the RDS database
    lambdaRdsRoleWithPw.addToPolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .actions(List.of("kms:Decrypt")) // needed for lambda to connect to RDS Proxy
                .resources(
                    List.of(
                        "arn:aws:kms:"
                            + Stack.of(this).getRegion()
                            + ":"
                            + Stack.of(this).getAccount()
                            + ":key/*"))
                .build()));

    return lambdaRdsRoleWithPw;
  }

  @NotNull
  private Role createLambdaRdsProxyRoleWithIam(String dbProxyUsername) {
    final Role lambdaRdsProxyRoleWithIam;

    lambdaRdsProxyRoleWithIam =
        new Role(
            this,
            "FarmLambdaRdsProxyRoleWithIam",
            RoleProps.builder()
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(
                    List.of(
                        // Allow Lambda to create/put logs and create/delete network interface
                        ManagedPolicy.fromAwsManagedPolicyName(
                            "service-role/AWSLambdaVPCAccessExecutionRole")))
                .build());

    lambdaRdsProxyRoleWithIam.addToPolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .actions(List.of("rds-db:connect")) // needed for lambda to connect to RDS Proxy
                .resources(
                    List.of(
                        "arn:aws:rds-db:"
                            + Stack.of(this).getRegion()
                            + ":"
                            + Stack.of(this).getAccount()
                            + ":dbuser:*"
                            + "/"
                            + dbProxyUsername))
                .build()));

    return lambdaRdsProxyRoleWithIam;
  }

  private void createApiGateway(ApiStackProps props) throws IOException {
    final String apiStageName = "Prod";

    LogGroup accessLogGroup =
        new LogGroup(
            this,
            "ILMLFDeliveryAccess",
            LogGroupProps.builder().retention(RetentionDays.TWO_MONTHS).build());

    Map logFormat = new LinkedHashMap();
    logFormat.put("status", "$context.status");
    logFormat.put("profile", "$context.authorizer.claims.profile");
    logFormat.put("ip", "$context.identity.sourceIp");
    logFormat.put("requestId", "$context.requestId");
    logFormat.put("responseLength", "$context.responseLength");
    logFormat.put("httpMethod", "$context.httpMethod");
    logFormat.put("protocol", "$context.protocol");
    logFormat.put("resourcePath", "$context.resourcePath");
    logFormat.put("requestTime", "$context.requestTime");
    logFormat.put("username", "$context.authorizer.claims['cognito:username']");

    Function createSlotsHandler =
        this.DefaultLambdaRdsProxy("CreateSlots", props, this.lambdaRdsProxyRoleWithIam);

    Function getSlotsHandler =
        this.DefaultLambdaRdsProxy("GetSlots", props, this.lambdaRdsProxyRoleWithIam);

    Function bookDeliveryHandler =
        this.DefaultLambdaRdsProxy("BookDelivery", props, this.lambdaRdsProxyRoleWithIam);

    Role apiRole =
        new Role(
            this,
            "apiRole",
            RoleProps.builder()
                .assumedBy(new ServicePrincipal("apigateway.amazonaws.com"))
                .build());

    apiRole.addToPolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .resources(
                    List.of(
                        createSlotsHandler.getFunctionArn(),
                        getSlotsHandler.getFunctionArn(),
                        bookDeliveryHandler.getFunctionArn()))
                .actions(List.of("lambda:InvokeFunction"))
                .build()));

    apiRole.addManagedPolicy(
        ManagedPolicy.fromAwsManagedPolicyName(
            "service-role/AmazonAPIGatewayPushToCloudWatchLogs"));

    Map<String, Object> variables = new HashMap<>();

    variables.put(
        "CreateSlots",
        String.format(
            "arn:aws:apigateway:%s:lambda:path/2015-03-31/functions/%s/invocations",
            Stack.of(this).getRegion(), createSlotsHandler.getFunctionArn()));

    variables.put(
        "GetSlots",
        String.format(
            "arn:aws:apigateway:%s:lambda:path/2015-03-31/functions/%s/invocations",
            Stack.of(this).getRegion(), getSlotsHandler.getFunctionArn()));

    variables.put(
        "BookDelivery",
        String.format(
            "arn:aws:apigateway:%s:lambda:path/2015-03-31/functions/%s/invocations",
            Stack.of(this).getRegion(), bookDeliveryHandler.getFunctionArn()));

    variables.put("ApiRole", apiRole.getRoleArn());

    Writer writer = new StringWriter();
    MustacheFactory mf = new DefaultMustacheFactory();

    Object openapiSpecAsObject;
    try (Reader reader =
             new InputStreamReader(getClass().getClassLoader().getResourceAsStream("apiSchema.json"))) {
      Mustache mustache = mf.compile(reader, "OAS");
      mustache.execute(writer, variables);
      writer.flush();

      ObjectMapper JSONMapper = new ObjectMapper(new JsonFactory());
      openapiSpecAsObject = JSONMapper.readValue(writer.toString(), Object.class);
    }

    CfnApi apiGw =
        new CfnApi(
            this,
            "ILMLFDelivery",
            CfnApiProps.builder()
                .stageName(apiStageName)
                .definitionBody(openapiSpecAsObject)
                .accessLogSetting(
                    CfnApi.AccessLogSettingProperty.builder()
                        .destinationArn(accessLogGroup.getLogGroupArn())
                        .format(logFormat.toString())
                        .build())
                .cors(
                    // In production, limit this to only your domain name
                    CfnApi.CorsConfigurationProperty.builder()
                        .allowOrigin("'*'")
                        .allowHeaders("'*'")
                        .allowMethods("'*'")
                        .build())
                .build());

    /**
     * Enable API Gateway logging.
     * The logging requires a role with permissions to let API Gateway put logs in CloudWatch.
     * The permission is in the managed policy "AmazonAPIGatewayPushToCloudWatchLogs"
     *
     * Note that this needs to be done once per region.
     *
     * See https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-logging.html for details
     */
    Role accountApiCwRole =
        new Role(
            this,
            "AccountApiCwRole",
            RoleProps.builder()
                .assumedBy(new ServicePrincipal("apigateway.amazonaws.com"))
                .managedPolicies(
                    Collections.singletonList(
                        ManagedPolicy.fromAwsManagedPolicyName(
                            "service-role/AmazonAPIGatewayPushToCloudWatchLogs")))
                .build());

    // This construct is from aws-apigateway package. It is used specifically for enable logging.
    // See https://docs.aws.amazon.com/cdk/api/latest/docs/@aws-cdk_aws-apigateway.CfnAccount.html
    CfnAccount cfnAccount =
        new CfnAccount(
            this,
            "ApiGtwyAccountCwRole",
            CfnAccountProps.builder().cloudWatchRoleArn(accountApiCwRole.getRoleArn()).build());

    cfnAccount.getNode().addDependency(apiGw);

    new CfnOutput(this, "ApiUrl",
        CfnOutputProps.builder()
            .value(String.format("https://%s.execute-api.%s.amazonaws.com/%s",
                apiGw.getRef(),
                Stack.of(this).getRegion(),
                apiStageName))
            .build());
  }

  /**
   * Try to bundle the package locally. CDK can use this method to build locally (which is faster).
   * If the build doesn't work, it will build within a Docker image which should work regardless of
   * local environment.
   *
   * Note that CDK expects this function to return either true or false based on bundling result.
   *
   * @param outputPath
   * @return
   */
  private Boolean tryBundle(String outputPath) {
    try {
      ProcessBuilder pb =
          new ProcessBuilder(
              "bash",
              "-c",
              "cd ../ApiHandlers && ./gradlew build && cp build/distributions/lambda.zip "
                  + outputPath);

      Process p = pb.start(); // Start the process.
      p.waitFor(); // Wait for the process to finish.

      if (p.exitValue() == 0) {
        System.out.println("Script executed successfully");
        return true;
      } else {
        System.out.println("Script executed failed");
        return false;
      }

    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Create a Lambda function with configuration to connect to RDS Proxy.
   *
   * @param functionName
   * @param props
   * @param role
   * @return
   * @throws IOException
   */
  public Function DefaultLambdaRdsProxy(String functionName, ApiStackProps props, Role role)
      throws IOException {

    /**
     * Command for building Java handler inside a container
     */
    List<String> apiHandlersPackagingInstructions =
        Arrays.asList(
            "/bin/sh",
            "-c",
            "./gradlew build "
                + "&& ls /asset-output/"
                + "&& cp build/distributions/lambda.zip /asset-output/");


    BundlingOptions builderOptions =
        BundlingOptions.builder()
            // CDK will try to build resource locally with the `tryBundle()` first
            .local((s, bundlingOptions) -> this.tryBundle(s))
            // If `tryBundle()` fails (return false), it will use the instructions in `command`
            // to build inside Docker with the given image.
            .command(apiHandlersPackagingInstructions)
            .image(Runtime.JAVA_11.getBundlingImage())
            .user("root")
            .outputType(ARCHIVED)
            .build();

    Function function =
        new Function(
            this,
            functionName,
            FunctionProps.builder()
                .environment(
                    Map.of(
                        "DB_ENDPOINT",
                        functionName.equals("PopulateFarmDb")
                            ? props.getDbEndpoint()
                            : props.getDbProxyEndpoint(),
                        "DB_PORT", props.getDbPort().toString(),
                        "DB_REGION", props.getDbRegion(),
                        "DB_USER", props.getDbUser(),
                        "DB_ADMIN_SECRET", props.getDbAdminSecretName(),
                        "DB_USER_SECRET", props.getDbUserSecretName(),
                        "CORS_ALLOW_ORIGIN_HEADER", "*"))
                .runtime(Runtime.JAVA_11)
                .code(
                    Code.fromAsset(
                        "../ApiHandlers",
                        AssetOptions.builder()
                            .assetHashType(AssetHashType.CUSTOM)
                            .assetHash(Hashing.hashDirectory("../ApiHandlers/src", false))
                            .bundling(builderOptions)
                            .build()))
                .timeout(Duration.seconds(60))
                .memorySize(2048)
                .handler("com.ilmlf.delivery.api.handlers." + functionName)
                .vpc(this.dbVpc)
                .securityGroups(List.of(this.dbSg))
                .role(role)
                .build());

    return function;
  }
}
