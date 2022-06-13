package com.ilmlf.delivery.api;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.CfnOutputProps;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.lambda.Function;


public class PackagingApi extends Construct {

    public PackagingApi(software.constructs.@NotNull Construct scope, @NotNull String id, ApiStack.ApiStackProps apiStackProps, Role role) {
        super(scope, id);
        FunctionFactory functionFactory = new FunctionFactory(this, apiStackProps);

        Function uberJarFunction = functionFactory.createUberJarFunction("createSlotsUberJar", role);
        Function zipFunction = functionFactory.createUberJarFunction("createSlotsZipFile", role);
        Function customRuntimeFunction = functionFactory.createCustomRuntimeFunction("createSlotsCustomRuntime", role);
        Function containerFunction = functionFactory.createDockerImageFunction("createSlotsContainer", role, "LambdaBaseContainer");
        Function containerCustomFunction = functionFactory.createDockerImageFunction("createSlotsCustomContainer", role, "LambdaCustomContainer");

        RestApi restApi = RestApi.Builder.create(this, "PackagingSlotsApi").restApiName("PackagingSlotsApi").build();
        Resource slotResource = restApi.getRoot().addResource("farm").addResource("{farm-id}").addResource("slots");
        slotResource.addResource("uber").addMethod("POST", new LambdaIntegration(uberJarFunction));
        slotResource.addResource("zip").addMethod("POST", new LambdaIntegration(zipFunction));
        slotResource.addResource("custom").addMethod("POST", new LambdaIntegration(customRuntimeFunction));
        slotResource.addResource("container").addMethod("POST", new LambdaIntegration(containerFunction));
        slotResource.addResource("container-custom").addMethod("POST", new LambdaIntegration(containerCustomFunction));

        new CfnOutput(scope, "ApiPackagingUrl", CfnOutputProps.builder()
                .value(restApi.getUrl())
                .build());
    }
}
