using Amazon.Lambda.APIGatewayEvents;
using Amazon.Lambda.Core;
using Amazon.Lambda.RuntimeSupport;
using Amazon.Lambda.Serialization.SystemTextJson;
using MyLocalFarmer.ProofOfAddress.ConfigFunction;

// The function handler that will be called for each Lambda event
var handler = (APIGatewayProxyRequest request, ILambdaContext context) =>
{

    return new
    APIGatewayProxyResponse()
    {
        StatusCode = 200,
        Body = System.Text.Json.JsonSerializer.Serialize(new {
            GetPresignedUrl = Environment.GetEnvironmentVariable(Config.GET_PRESIGNED_URL.ToString()),
            LoginUrl = Environment.GetEnvironmentVariable(Config.LOGIN_URL.ToString()),
            LogoutUrl = Environment.GetEnvironmentVariable(Config.LOGOUT_URL.ToString()),
            GetCurrentUserUrl = Environment.GetEnvironmentVariable(Config.GET_CURRENT_USER_URL.ToString())
        })
    };
};

// Build the Lambda runtime client passing in the handler to call for each
// event and the JSON serializer to use for translating Lambda JSON documents
// to .NET types.
await LambdaBootstrapBuilder.Create(handler, new DefaultLambdaJsonSerializer())
        .Build()
        .RunAsync();