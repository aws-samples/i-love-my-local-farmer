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


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.customresources.Provider;
import software.amazon.awscdk.customresources.ProviderProps;
import software.amazon.awscdk.services.apigateway.CfnAccount;
import software.amazon.awscdk.services.apigateway.CfnAccountProps;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.LogGroupProps;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.sam.CfnApi;
import software.amazon.awscdk.services.sam.CfnApiProps;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.TopicProps;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static software.amazon.awscdk.core.BundlingOutput.ARCHIVED;

public class ApiStack extends Stack {

  private final IVpc dbVpc;
  private final ISecurityGroup dbSg;
  private final Role lambdaRdsProxyRoleWithIam;
  private final FunctionFactory functionFactory;

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
    private String alertEmail;
    private Boolean deployPackagingApi;

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
    this.functionFactory = new FunctionFactory(this, props);

    createApiGateway(props);

    if (props.deployPackagingApi) {
      new PackagingApi(this, "PackagingApi", props, lambdaRdsProxyRoleWithIam);
    }

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
   * See <a href="https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/template-custom-resources.html">https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/template-custom-resources.html</a> for details about custom resource
   */
  private void createCustomResourceToPopulateDb(ApiStackProps props, Role lambdaRdsProxyRoleWithPw) throws IOException {
    // See https://docs.aws.amazon.com/cdk/api/latest/java/software/amazon/awscdk/customresources/package-summary.html for details on writing a Lambda function
    // and providers
    Function dbPopulatorHandler =
        functionFactory.createDefaultLambdaRdsProxy("PopulateFarmDb", lambdaRdsProxyRoleWithPw);

    Provider dbPopulatorProvider =
        new Provider(
            this,
            "InvokePopulateDataProvider",
            ProviderProps.builder().onEventHandler(dbPopulatorHandler).build());

    // we will pass in the contents of the SQL File, so that any changes in the file
    // trigger an 'Update' and executes the Populator lambda (which executes the sql statement)
    String scriptFile = "../ApiHandlers/scripts/com/ilmlf/db/dbinit.sql";
    String sqlScript = new String(Files.readAllBytes(Paths.get(scriptFile)));
    
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

    Map<String, String> logFormat = new LinkedHashMap();
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

    Topic errorAlarmTopic = new Topic(this, "ErrorAlarmTopic", TopicProps.builder()
        .topicName("ErrorAlarmTopic")
        .build());

    // Need to check for null as the tryGetContext() call will be null if parameter is not passed in
    if (props.alertEmail != null && !props.alertEmail.isEmpty()) {
      errorAlarmTopic.addSubscription(new EmailSubscription(props.alertEmail));
    }

    ApiFunction createSlotsHandler = functionFactory.createDefaultLambdaRdsProxy("CreateSlots", this.lambdaRdsProxyRoleWithIam);

    ApiFunction getSlotsHandler =
            functionFactory.createDefaultLambdaRdsProxy("GetSlots", this.lambdaRdsProxyRoleWithIam);

    ApiFunction bookDeliveryHandler =
            functionFactory.createDefaultLambdaRdsProxy("BookDelivery", this.lambdaRdsProxyRoleWithIam);

    FunctionDashboard createSlotsDashboard = new FunctionDashboard(this, "FunctionDashboard",
        FunctionDashboard.FunctionDashboardProps.builder()
            .dashboardName("FunctionDashboard")
            .getSlotsApiMethodName(getSlotsHandler.getApiMethodName())
            .getSlotsFunctionName(getSlotsHandler.getFunctionName())
            .createSlotsApiMethodName(createSlotsHandler.getApiMethodName())
            .createSlotsFunctionName(createSlotsHandler.getFunctionName())
            .bookDeliveryApiMethodName(bookDeliveryHandler.getApiMethodName())
            .bookDeliveryFunctionName(bookDeliveryHandler.getFunctionName())
            .alarmTopic(errorAlarmTopic)
            .build());

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

      ObjectMapper jsonMapper = new ObjectMapper(new JsonFactory());
      openapiSpecAsObject = jsonMapper.readValue(writer.toString(), Object.class);
    }

    CfnApi apiGw =
        new CfnApi(
            this,
            "ILMLFDelivery",
            CfnApiProps.builder()
                .stageName(apiStageName)
                .definitionBody(openapiSpecAsObject)
                .tracingEnabled(true)
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

    /*
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



}
