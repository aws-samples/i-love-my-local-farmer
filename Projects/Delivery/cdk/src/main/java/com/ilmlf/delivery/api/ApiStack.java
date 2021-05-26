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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.AssetHashType;
import software.amazon.awscdk.core.BundlingOptions;
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

    private String dbProxyEndpoint;
    private String dbProxyArn;
    private String dbEndpoint;
    private String dbAdminSecretName;
    private String dbAdminSecretArn;
    private String dbUserSecretName;
    private String dbUserSecretArn;
    private String dbUser;
    private Vpc dbVpc;
    private SecurityGroup dbSg;
    private String dbRegion;
    private Environment env;
  }

  public ApiStack(final Construct scope, final String id) throws IOException {
    this(scope, id, null);
  }

  public ApiStack(final Construct scope, final String id, final ApiStackProps props)
      throws IOException {
    super(scope, id, props);

    // Create lambdaRdsProxyRoles

    lambdaRdsProxyRoleWithIam = createLambdaRdsProxyRoleWithIam(props);

    Role lambdaRdsProxyRoleWithPw = createLambdaRdsProxyRoleWithPw(props);

    // Get Db ref VPC and SG

    this.dbVpc = props.dbVpc;

    this.dbSg = props.dbSg;

    createApiGateway(props);

    populateDb(props, lambdaRdsProxyRoleWithPw);
  }

  private void populateDb(ApiStackProps props, Role lambdaRdsProxyRoleWithPw) throws IOException {
    Function dbPopulatorHandler =
        DefaultLambdaRdsProxy("PopulateFarmDb", props, lambdaRdsProxyRoleWithPw);

    Provider dbPopulatorProvider =
        new Provider(
            this,
            "InvokePopulateDataProvider",
            ProviderProps.builder().onEventHandler(dbPopulatorHandler).build());

    //    // "...vxxxx" to trigger a new update in CI/CD environments
    new CustomResource(
        this,
        "PopulateDataProviderv1",
        CustomResourceProps.builder()
            .serviceToken(dbPopulatorProvider.getServiceToken())
            .resourceType("Custom::PopulateDataProvider")
            .build());
  }

  @NotNull
  private Role createLambdaRdsProxyRoleWithPw(ApiStackProps props) {
    Role lambdaRdsProxyRoleWithPw =
        new Role(
            this,
            "FarmLambdaRdsProxyRoleWithPw",
            RoleProps.builder()
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(
                    List.of(
                        ManagedPolicy.fromAwsManagedPolicyName(
                            "service-role/AWSLambdaVPCAccessExecutionRole")))
                .build());

    lambdaRdsProxyRoleWithPw.addToPolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .actions(
                    List.of(
                        "secretsmanager:GetSecretValue",
                        "secretsmanager:DescribeSecret")) // needed for lambda to connect to
                // RDS Proxy
                .resources(List.of(props.dbAdminSecretArn, props.dbUserSecretArn))
                .build()));

    lambdaRdsProxyRoleWithPw.addToPolicy(
        new PolicyStatement(
            PolicyStatementProps.builder()
                .effect(Effect.ALLOW)
                .actions(List.of("rds-db:connect")) // needed for lambda to connect to RDS Proxy
                .resources(List.of(props.dbProxyArn))
                .build()));

    lambdaRdsProxyRoleWithPw.addToPolicy(
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
    return lambdaRdsProxyRoleWithPw;
  }

  @NotNull
  private Role createLambdaRdsProxyRoleWithIam(ApiStackProps props) {
    final Role lambdaRdsProxyRoleWithIam;
    lambdaRdsProxyRoleWithIam =
        new Role(
            this,
            "FarmLambdaRdsProxyRoleWithIam",
            RoleProps.builder()
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(
                    List.of(
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
                            + "/lambda_iam"))
                .build()));
    return lambdaRdsProxyRoleWithIam;
  }

  private void createApiGateway(ApiStackProps props) throws IOException {
    // Create API Gateway

    LogGroup accessLogGroup =
        new LogGroup(
            this,
            "ILMLFDeliveryAccess",
            LogGroupProps.builder().retention(RetentionDays.TWO_MONTHS).build());
    Map logFormat =
        Map.of(
            "requestId", "$context.requestId",
            "ip", "$context.identity.sourceIp",
            "requestTime", "$context.requestTime",
            "httpMethod", "$context.httpMethod",
            "resourcePath", "$context.resourcePath",
            "status", "$context.status",
            "protocol", "$context.protocol",
            "responseLength", "$context.responseLength",
            "profile", "$context.authorizer.claims.profile",
            "username", "$context.authorizer.claims['cognito:username']");

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
    ((CfnRole) apiRole.getNode().getDefaultChild()).overrideLogicalId("apiRole");

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
            props.getEnv().getRegion(), createSlotsHandler.getFunctionArn()));
    variables.put(
        "GetSlots",
        String.format(
            "arn:aws:apigateway:%s:lambda:path/2015-03-31/functions/%s/invocations",
            props.getEnv().getRegion(), getSlotsHandler.getFunctionArn()));
    variables.put(
        "BookDelivery",
        String.format(
            "arn:aws:apigateway:%s:lambda:path/2015-03-31/functions/%s/invocations",
            props.getEnv().getRegion(), bookDeliveryHandler.getFunctionArn()));
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
                .stageName("ILMLFDeliveryStage")
                .definitionBody(openapiSpecAsObject)
                .accessLogSetting(
                    CfnApi.AccessLogSettingProperty.builder()
                        .destinationArn(accessLogGroup.getLogGroupArn())
                        .format(logFormat.toString())
                        .build())
                .cors(
                    CfnApi.CorsConfigurationProperty.builder()
                        .allowOrigin("'*'")
                        .allowHeaders("'*'")
                        .allowMethods("'*'")
                        .build())
                .build());
    // add account-level CW-logs access to ApiGtwy
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
    CfnAccount cfnAccount =
        new CfnAccount(
            this,
            "ApiGtwyAccountCwRole",
            CfnAccountProps.builder().cloudWatchRoleArn(accountApiCwRole.getRoleArn()).build());
    cfnAccount.getNode().addDependency(apiGw);
  }

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
      if(p.exitValue() == 0) {
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

  public Function DefaultLambdaRdsProxy(String functionName, ApiStackProps props, Role role)
      throws IOException {

    List<String> apiHandlersPackagingInstructions =
        Arrays.asList(
            "/bin/sh",
            "-c",
            "./gradlew build "
                + "&& ls /asset-output/"
                + "&& cp build/distributions/lambda.zip /asset-output/");

    BundlingOptions.Builder builderOptions =
        BundlingOptions.builder()
            .local((s, bundlingOptions) -> this.tryBundle(s))
            .command(apiHandlersPackagingInstructions)
            .image(Runtime.JAVA_11.getBundlingImage())
            .user("root")
            .outputType(ARCHIVED);

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
                        "DB_REGION", props.getDbRegion(),
                        "DB_USER", props.getDbUser(),
                        "DB_ADMIN_SECRET", props.getDbAdminSecretName(),
                        "DB_USER_SECRET", props.getDbUserSecretName(),
                        "CORS_ALLOW_ORIGIN_HEADER", "*")) // TODO: avoid '*'
                .runtime(Runtime.JAVA_11)
                .code(
                    Code.fromAsset(
                        "../ApiHandlers",
                        AssetOptions.builder()
                            .assetHashType(AssetHashType.CUSTOM)
                            .assetHash(Hashing.hashDirectory("../ApiHandlers/src", false))
                            .bundling(
                                builderOptions.command(apiHandlersPackagingInstructions).build())
                            .build()))
                .timeout(Duration.seconds(60))
                .memorySize(2048)
                .handler("com.ilmlf.delivery.api.handlers." + functionName)
                .vpc(this.dbVpc)
                .securityGroups(List.of(this.dbSg))
                .role(role)
                .build());
    ((CfnFunction) function.getNode().getDefaultChild()).overrideLogicalId(functionName);
    return function;
  }
}
