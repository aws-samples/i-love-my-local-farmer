package com.ilmlf.delivery.api;

import lombok.SneakyThrows;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.s3.assets.AssetOptions;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static software.amazon.awscdk.core.BundlingOutput.ARCHIVED;


/**
 * Class that provides a single access point to create the DeliveryAPI Lambda functions
 * You can create the function in different types (ApiFunction, Function, DockerImageFunction)
 * The class also contains the automated bundling of the functions so that you do not have
 * to build the application locally before cdk deploy
 */
public class FunctionFactory {

    private final Construct construct;
    private final ApiStack.ApiStackProps apiStackProps;

    public FunctionFactory(Construct creatingConstruct, ApiStack.ApiStackProps apiStackProps) {
        this.construct = creatingConstruct;
        this.apiStackProps = apiStackProps;
    }

    public DockerImageFunction createDockerImageFunction(String functionName, Role role, String imageName) {
        return new DockerImageFunction(
                construct,
                functionName,
                DockerImageFunctionProps.builder()
                        .environment(getEnvironmentVariables(functionName))
                        .code(DockerImageCode.fromImageAsset(Paths.get("../").toAbsolutePath().toString(), AssetImageCodeProps.builder().file(imageName).build()))
                        .timeout(Duration.seconds(60))
                        .memorySize(2048)
                        .vpc(apiStackProps.getDbVpc())
                        .securityGroups(List.of(apiStackProps.getDbSg()))
                        .functionName(functionName)
                        .role(role)
                        .build());
    }


    public Function createCustomRuntimeFunction(String functionName, Role role) {
        BundlingOptions something = BundlingOptions.builder()
                .image(DockerImage.fromBuild(Paths.get("../").toAbsolutePath().toString(),
                        DockerBuildOptions.builder().file("LambdaCustomRuntimeBuilder").build()))
                .command(List.of("sh", "-c", "cp runtime.zip /asset-output"))
                .workingDirectory("/")
                .user("root")
                .outputType(ARCHIVED)
                .build();

            return new Function(
                    construct,
                    functionName,
                    FunctionProps.builder()
                            .environment(getEnvironmentVariables(functionName))
                            .runtime(Runtime.PROVIDED_AL2)
                            .code(Code.fromAsset("../ApiHandlers", AssetOptions.builder().assetHash("test").assetHashType(AssetHashType.CUSTOM).bundling(something).build()))
                            .timeout(Duration.seconds(60))
                            .memorySize(2048)
                            .handler("com.ilmlf.delivery.api.handlers.CreateSlots")
                            .vpc(apiStackProps.getDbVpc())
                            .securityGroups(List.of(apiStackProps.getDbSg()))
                            .functionName(functionName)
                            .role(role)
                            .build());

    }

    @SneakyThrows
    public Function createUberJarFunction(String functionName, Role role) {
        return new Function(
                construct,
                functionName,
                FunctionProps.builder()
                        .environment(getEnvironmentVariables(functionName))
                        .runtime(Runtime.JAVA_11)
                        .code(
                                Code.fromAsset(
                                        "../ApiHandlers",
                                        AssetOptions.builder()
                                                .assetHashType(AssetHashType.CUSTOM)
                                                .assetHash(Hashing.hashDirectory("../ApiHandlers/src", false))
                                                .bundling(getBundlingOptions("build/libs/lambda-uber-all.jar"))
                                                .build()))
                        .timeout(Duration.seconds(60))
                        .memorySize(2048)
                        .handler("com.ilmlf.delivery.api.handlers.CreateSlots")
                        .vpc(apiStackProps.getDbVpc())
                        .securityGroups(List.of(apiStackProps.getDbSg()))
                        .functionName(functionName)
                        .role(role)
                        .build());
    }

    /**
     * Create a Lambda function with configuration to connect to RDS Proxy.
     *
     * @param functionName
     * @param role
     * @throws IOException
     */
    public ApiFunction createDefaultLambdaRdsProxy(String functionName, Role role)
            throws IOException {

        return new ApiFunction(
                construct,
                functionName,
                FunctionProps.builder()
                        .environment(getEnvironmentVariables(functionName))
                        .runtime(Runtime.JAVA_11)
                        .code(
                                Code.fromAsset(
                                        "../ApiHandlers",
                                        AssetOptions.builder()
                                                .assetHashType(AssetHashType.CUSTOM)
                                                .assetHash(Hashing.hashDirectory("../ApiHandlers/src", false))
                                                .bundling(getBundlingOptions("build/distributions/lambda.zip"))
                                                .build()))
                        .timeout(Duration.seconds(60))
                        .memorySize(2048)
                        .handler("com.ilmlf.delivery.api.handlers." + functionName)
                        .vpc(apiStackProps.getDbVpc())
                        .securityGroups(List.of(apiStackProps.getDbSg()))
                        .functionName(functionName)
                        .role(role)
                        .build());
    }

    private BundlingOptions getBundlingOptions(String artifactBuildPath) {
        /*
         * Command for building Java handler inside a container
         */
        List<String> apiHandlersPackagingInstructions =
                Arrays.asList(
                        "/bin/sh",
                        "-c",
                        "./gradlew build "
                                + "&& ls /asset-output/"
                                + "&& cp " + artifactBuildPath + " /asset-output/");


        return BundlingOptions.builder()
                // CDK will try to build resource locally with the `tryBundle()` first
                .local((s, bundlingOptions) -> this.tryBundle(s, artifactBuildPath))
                // If `tryBundle()` fails (return false), it will use the instructions in `command`
                // to build inside Docker with the given image.
                .command(apiHandlersPackagingInstructions)
                .image(Runtime.JAVA_11.getBundlingImage())
                .user("root")
                .outputType(ARCHIVED)
                .build();


    }

    /**
     * Try to bundle the package locally. CDK can use this method to build locally (which is faster).
     * If the build doesn't work, it will build within a Docker image which should work regardless of
     * local environment.
     * <p>
     * Note that CDK expects this function to return either true or false based on bundling result.
     *
     * @param outputPath
     * @return whether the bundling script was successfully executed
     */
    private Boolean tryBundle(String outputPath, String artifactBuildPath) {
        try {
            ProcessBuilder pb =
                    new ProcessBuilder(
                            "bash",
                            "-c",
                            "cd ../ApiHandlers && ./gradlew build && cp " + artifactBuildPath + " "
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

    private Map<String, String> getEnvironmentVariables(String functionName) {
        Map<String, String> env = new HashMap<>(Map.of(
                "DB_ENDPOINT",
                functionName.equals("PopulateFarmDb")
                        ? this.apiStackProps.getDbEndpoint()
                        : this.apiStackProps.getDbProxyEndpoint(),
                "DB_PORT", this.apiStackProps.getDbPort().toString(),
                "DB_REGION", this.apiStackProps.getDbRegion(),
                "DB_USER", this.apiStackProps.getDbUser(),
                "DB_ADMIN_SECRET", this.apiStackProps.getDbAdminSecretName(),
                "DB_USER_SECRET", this.apiStackProps.getDbUserSecretName(),
                "CORS_ALLOW_ORIGIN_HEADER", "*"));

        env.put("POWERTOOLS_METRICS_NAMESPACE", "DeliveryApi");
        env.put("POWERTOOLS_SERVICE_NAME", "DeliveryApi");
        env.put("POWERTOOLS_TRACER_CAPTURE_ERROR", "true");
        env.put("POWERTOOLS_TRACER_CAPTURE_RESPONSE", "false");
        env.put("POWERTOOLS_LOG_LEVEL", "INFO");
        env.put("JAVA_TOOL_OPTIONS", "-XX:+TieredCompilation -XX:TieredStopAtLevel=1");

        return env;
    }


}
