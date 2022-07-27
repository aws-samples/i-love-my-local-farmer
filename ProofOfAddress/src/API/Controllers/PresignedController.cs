using Amazon.CognitoIdentity;
using Amazon.S3;
using Amazon.S3.Model;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace MyLocalFarmer.ProofOfAddress.API.Controllers
{
    [Route("[controller]")]
    [ApiController]
    public class PresignedController : ControllerBase
    {
        private readonly IConfiguration _configuration;
        private readonly ILogger<PresignedController> _logger;

        public PresignedController(IConfiguration configuration, ILogger<PresignedController> logger) : base()
        {
            _configuration = configuration;
            _logger = logger;
        }

        //[Authorize]
        public async Task<string> Get()
        {
            _logger.LogInformation("Start processing get presigned request");

            // Retrieve the ID token from the HttpContexte and use it to get user temporary credentials from Amazon Cognito
            var idToken = await HttpContext.GetTokenAsync("Cognito", "id_token");
            CognitoAWSCredentials credentials = new CognitoAWSCredentials(_configuration["IdentityPoolId"], Amazon.RegionEndpoint.GetBySystemName(_configuration["IdentityPoolRegion"]));
            _logger.LogInformation($"Authority: {_configuration["Authority"]}");
            _logger.LogInformation($"TokenProviderName: {_configuration["Authority"].Replace("https://", "")}");
            credentials.AddLogin(_configuration["Authority"].Replace("https://", ""), idToken);
            await credentials.GetCredentialsAsync();

            // Use the user temporary credentials to get a presigned URL from Amazon S3
            return GeneratePresignedUrl(credentials, 600, _configuration["BucketName"], $"{Guid.NewGuid()}.jpg");
        }

        private string GeneratePresignedUrl(CognitoAWSCredentials credentials, int duration, string bucket, string key)
        {
            IAmazonS3 s3Client = new AmazonS3Client(credentials);

            var request = new GetPreSignedUrlRequest
            {
                BucketName = bucket,
                Key = $"upload/{key}",
                Verb = HttpVerb.PUT,
                Expires = DateTime.UtcNow.AddSeconds(duration),
                ContentType = "image/jpeg"
            };


            return s3Client.GetPreSignedURL(request);
        }
    }
}
