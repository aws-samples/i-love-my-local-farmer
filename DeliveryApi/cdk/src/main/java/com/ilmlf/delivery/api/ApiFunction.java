package com.ilmlf.delivery.api;

import lombok.Getter;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;

/**
 * Class that represents a function that is integrated with Api Gateway.
 * The reason for creating this wrapper class is so that a plain apiMethodName
 * can be associated with the Function, that does not include the hashes created by the CDK
 * when using the regular function name as part of the logical identifier.
 * Without this, when creating dashboards, we will run into the error of not being able to name the dashboards
 * using the as-of-yet unresolved tokens representing the function names
 *
 * See the following for more details,
 *
 * https://docs.aws.amazon.com/cdk/latest/guide/identifiers.html
 * https://docs.aws.amazon.com/cdk/latest/guide/tokens.html
 */
@Getter
public class ApiFunction extends Function {
  private final String apiMethodName;

  public ApiFunction(Construct scope, String id, FunctionProps props) {
    super(scope, id, FunctionProps.builder()
        .environment(props.getEnvironment())
        .runtime(props.getRuntime())
        .code(props.getCode())
        .timeout(props.getTimeout())
        .memorySize(props.getMemorySize())
        .handler(props.getHandler())
        .vpc(props.getVpc())
        .securityGroups(props.getSecurityGroups())
        .role(props.getRole())
        .tracing(props.getTracing())
        .build());

    this.apiMethodName = props.getFunctionName();
  }
}
