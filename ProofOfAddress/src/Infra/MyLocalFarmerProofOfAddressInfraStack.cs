using Amazon.CDK;
using Amazon.CDK.AWS.APIGateway;
using Amazon.CDK.AWS.AppRunner;
using Amazon.CDK.AWS.CloudFront;
using Amazon.CDK.AWS.CloudFront.Origins;
using Amazon.CDK.AWS.Cognito;
using Amazon.CDK.AWS.Cognito.IdentityPool.Alpha;
using Amazon.CDK.AWS.Ecr.Assets;
using Amazon.CDK.AWS.IAM;
using Amazon.CDK.AWS.Lambda;
using Amazon.CDK.AWS.S3;
using Amazon.CDK.AWS.S3.Assets;
using Amazon.CDK.AWS.S3.Deployment;
using Amazon.CDK.CustomResources;
using Constructs;
using System;
using System.Collections.Generic;
using System.IO;
using AssetOptions = Amazon.CDK.AWS.S3.Assets.AssetOptions;
using Function = Amazon.CDK.AWS.Lambda.Function;
using FunctionProps = Amazon.CDK.AWS.Lambda.FunctionProps;

namespace MyLocalFarmer.ProofOfAddress.Infra
{
    public class MyLocalFarmerProofOfAddressInfraStack : Stack
    {
        internal MyLocalFarmerProofOfAddressInfraStack(Construct scope, string id, IStackProps props = null) : base(scope, id, props)
        {

            #region AMAZON CLOUDFRONT DISTRIBUTION
            // Create an Amazon S3 bucket to store the single-page application content.
            var contentS3Bucket = new Bucket(this, "ContentS3Bucket", new BucketProps
            {
                AutoDeleteObjects = true,
                RemovalPolicy = RemovalPolicy.DESTROY,
                BlockPublicAccess = BlockPublicAccess.BLOCK_ALL
            });

            // Create an Amazon CloudFront distribution with the content S3 Bucket as origin.
            var cloudFrontDistribution = new Distribution(this, "CloudFrontDistribution", new DistributionProps
            {
                DefaultBehavior = new BehaviorOptions
                {
                    Origin = new S3Origin(contentS3Bucket),
                    CachePolicy = CachePolicy.CACHING_DISABLED
                },
                DefaultRootObject = "index.html",
                // use the lowest price class to deploy only in edge locations in North America, Europe and Israel to reduce costs and update time
                PriceClass = PriceClass.PRICE_CLASS_100
            });


            // Bundle the single-page application content and upload it to the contentS3Bucket
            IEnumerable<string> publishCommands = new[]
            {
                "export DOTNET_CLI_HOME=\"/tmp/DOTNET_CLI_HOME\"",
                "export PATH=\"$PATH:/tmp/DOTNET_CLI_HOME/.dotnet/tools\"",
                $"cd {nameof(MyLocalFarmer.ProofOfAddress.Web)}",
                "dotnet build -c Release",
                "dotnet publish -c Release",
                "cp -t /asset-output -R ./bin/Release/net6.0/publish/wwwroot/*"
            };
            BucketDeployment contentS3Deployment = new BucketDeployment(this, nameof(contentS3Deployment), new BucketDeploymentProps
            {
                Sources = new[] {
                    Source.Asset(
                        Directory.GetParent(Directory.GetCurrentDirectory()).FullName,
                        new AssetOptions()
                        {
                            Bundling = new BundlingOptions
                            {
                                Image = DockerImage.FromRegistry("mcr.microsoft.com/dotnet/sdk:6.0"),
                                User = "root",
                                Command = new string[] { "bash", "-c", string.Join(" && ", publishCommands) },

                            }
                        }
                    )
                },
                DestinationBucket = contentS3Bucket,
                MemoryLimit = 4096,
                Distribution = cloudFrontDistribution,
                DistributionPaths = new[] { "/*" },

            });

            // Create an output to get the url when the deployment is completed.
            new CfnOutput(this, "ApplicationUrl", new CfnOutputProps()
            {
                Description = "Url to access the application",
                Value = $"https://{cloudFrontDistribution.DomainName}/"
            });

            #endregion

            #region AWS APP RUNNER SERVICE
            // create a container image asset that CDK automatically publishes to Amazon Elastic Container Registry
            var asset = new DockerImageAsset(this, "ProofOfAddressApiImage", new DockerImageAssetProps
            {
                Directory = Directory.GetParent(Directory.GetCurrentDirectory()).FullName,
                File = Path.Join(nameof(MyLocalFarmer.ProofOfAddress.API), "Dockerfile"),
                Exclude = new[] { Path.Join(nameof(MyLocalFarmer.ProofOfAddress.Infra), "cdk.out") },
            });

            // create an IAM role with the a managed policy allowing an AWS App Runner service to access Amazon ECR
            var appRunnerAccessRole = new Role(this, "AppRunnerAccessRole", new RoleProps
            {
                AssumedBy = new ServicePrincipal("build.apprunner.amazonaws.com"),
                ManagedPolicies = new[]
                {
                    ManagedPolicy.FromAwsManagedPolicyName("service-role/AWSAppRunnerServicePolicyForECRAccess")
                }
            });

            // create an AWS App Runner service using the container image published to Amazon ECR and the IAM role
            var appRunnerService = new CfnService(this, "ProofOfAddressApi", new CfnServiceProps
            {
                SourceConfiguration = new CfnService.SourceConfigurationProperty
                {
                    AuthenticationConfiguration = new CfnService.AuthenticationConfigurationProperty
                    {
                        AccessRoleArn = appRunnerAccessRole.RoleArn
                    },
                    ImageRepository = new CfnService.ImageRepositoryProperty
                    {
                        ImageConfiguration = new CfnService.ImageConfigurationProperty
                        {
                            Port = "80"
                        },
                        ImageRepositoryType = "ECR",
                        ImageIdentifier = asset.ImageUri
                    }
                }
            });
            #endregion

            #region AMAZON COGNITO

            // Create an Amazon Cognito User Pool allowing self sign-up and using email as sign-in alias.
            var userPool = new UserPool(this, "UserPool", new UserPoolProps()
            {
                SelfSignUpEnabled = true,
                SignInAliases = new SignInAliases() { Email = true },
                RemovalPolicy = RemovalPolicy.DESTROY
            });

            // Create a client application supporting the authorization code flow.
            // The callback URL relies on the AWS App Runner service URL.
            // The logout URL relies on the Amazon CloudFront distribution domain name.
            var clientApp = userPool.AddClient("proof-of-address-api", new UserPoolClientOptions()
            {
                GenerateSecret = true,
                OAuth = new OAuthSettings()
                {
                    Flows = new OAuthFlows()
                    {
                        AuthorizationCodeGrant = true,
                        ClientCredentials = false,
                        ImplicitCodeGrant = false,
                    },
                    CallbackUrls = new string[]
                    {
                        $"https://{appRunnerService.AttrServiceUrl}/callback"
                    },
                    LogoutUrls = new string[]
                    {
                        $"https://{cloudFrontDistribution.DomainName}"
                    }
                }
            });

            // Create a User Pool domain so that user can log in throught the Amazon Cognito Hosted UI
            var userPoolDomain = userPool.AddDomain("UserPoolDomain", new UserPoolDomainOptions()
            {
                CognitoDomain = new CognitoDomainOptions()
                {
                    DomainPrefix = Names.UniqueResourceName(userPool, new UniqueResourceNameOptions()).ToLower()
                }
            });

            // Create an AWS Custom Resource to retrieve the client app secret as it is required for configuring the Backend-For-Frontend API.
            var userPoolClientCustomResource = new AwsCustomResource(this, "UserPoolClientSecret", new AwsCustomResourceProps()
            {
                InstallLatestAwsSdk = false,
                Policy = AwsCustomResourcePolicy.FromSdkCalls(new SdkCallsPolicyOptions()
                {
                    Resources = new string[] { userPool.UserPoolArn }
                }),
                OnCreate = new AwsSdkCall()
                {
                    Service = "CognitoIdentityServiceProvider",
                    Action = "describeUserPoolClient",
                    Parameters = new Dictionary<string, object>
                    {
                        { "ClientId", clientApp.UserPoolClientId },
                        { "UserPoolId", userPool.UserPoolId }
                    },
                    PhysicalResourceId = PhysicalResourceId.Of(clientApp.UserPoolClientId + "Secret")
                },
                OnUpdate = new AwsSdkCall()
                {
                    Service = "CognitoIdentityServiceProvider",
                    Action = "describeUserPoolClient",
                    Parameters = new Dictionary<string, object>
                    {
                        { "ClientId", clientApp.UserPoolClientId },
                        { "UserPoolId", userPool.UserPoolId }
                    },
                    PhysicalResourceId = PhysicalResourceId.Of(clientApp.UserPoolClientId + "Secret")
                }
            });
            userPoolClientCustomResource.Node.AddDependency(clientApp);

            // Create an Amazon Cognito Identity Pool that serves user temporary credentials for users from the Amazon Cognito User Pool
            // to access AWS resources
            var identityPool = new IdentityPool(this, "IdentityPool", new IdentityPoolProps()
            {
                AuthenticationProviders = new IdentityPoolAuthenticationProviders()
                {
                    UserPools = new IUserPoolAuthenticationProvider[]
                     {
                       new UserPoolAuthenticationProvider(new UserPoolAuthenticationProviderProps
                       {
                          UserPool = userPool,
                          UserPoolClient = clientApp
                       })
                    }
                }
            });

            #endregion

            #region AMAZON S3 BUCKET FOR UPLOADED FILES
            // Create a Amazon S3 Bucket to store uploaded file. It requires a CORS rule allowing PUT requests
            // from the CloudFront distribution domain
            var fileStorage = new Bucket(this, "FileStorage", new BucketProps
            {
                AutoDeleteObjects = true,
                RemovalPolicy = RemovalPolicy.DESTROY,
                BlockPublicAccess = BlockPublicAccess.BLOCK_ALL,
                Cors = new CorsRule[]
                {
                    new CorsRule()
                    {
                        AllowedHeaders = new string[]{"*"},
                        AllowedMethods = new HttpMethods[]{HttpMethods.PUT},
                        AllowedOrigins = new string[]{$"https://{cloudFrontDistribution.DomainName}"},
                        ExposedHeaders = new string[]{},
                    }
                }
            });
            // Grant write permissions to authenticated users from the Amazon Cognito Identity Pool
            fileStorage.GrantWrite(identityPool.AuthenticatedRole);
            #endregion

            #region UPDATE AWS APP RUNNER SERVICE ENVIRONMENT VARIABLE
            var appRunnerCustomResource = new AwsCustomResource(this, "AppRunnerServiceEnvironmentVariables", new AwsCustomResourceProps()
            {
                InstallLatestAwsSdk = true,
                Policy = AwsCustomResourcePolicy.FromSdkCalls(new SdkCallsPolicyOptions()
                {
                    Resources = new string[] { appRunnerService.AttrServiceArn }
                }),
                OnCreate = new AwsSdkCall()
                {
                    Service = "AppRunner",
                    Action = "updateService",
                    Parameters = new Dictionary<string, object>
                    {
                        { "ServiceArn", appRunnerService.AttrServiceArn },
                        { "SourceConfiguration", new Dictionary<string, object>()
                            {
                                { "ImageRepository", new Dictionary<string, object>()
                                    {
                                        { "ImageIdentifier", asset.ImageUri },
                                        { "ImageRepositoryType", "ECR"},
                                        { "ImageConfiguration", new Dictionary<string, object>()
                                            {
                                                { "Port", "80" },
                                                { "RuntimeEnvironmentVariables", new Dictionary<string, string>()
                                                    {
                                                        { "Authority", userPool.UserPoolProviderUrl },
                                                        { "LogoutUri", $"{userPoolDomain.BaseUrl()}/logout" },
                                                        { "ClientOrigin", $"https://{cloudFrontDistribution.DomainName}"},
                                                        { "BucketName", fileStorage.BucketName},
                                                        { "IdentityPoolId", identityPool.IdentityPoolId},
                                                        { "IdentityPoolRegion", identityPool.Env.Region},
                                                        { "ClientId", clientApp.UserPoolClientId},
                                                        { "ClientSecret", userPoolClientCustomResource.GetResponseField("UserPoolClient.ClientSecret")}
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    PhysicalResourceId = PhysicalResourceId.Of(appRunnerService.AttrServiceArn + "EnvironmentVariable")
                },
                OnUpdate = new AwsSdkCall()
                {
                    Service = "AppRunner",
                    Action = "updateService",
                    Parameters = new Dictionary<string, object>
                    {
                        { "ServiceArn", appRunnerService.AttrServiceArn },
                        { "SourceConfiguration", new Dictionary<string, object>()
                            {
                                { "ImageRepository", new Dictionary<string, object>()
                                    {
                                        { "ImageIdentifier", asset.ImageUri },
                                        { "ImageRepositoryType", "ECR"},
                                        { "ImageConfiguration", new Dictionary<string, object>()
                                            {
                                                { "Port", "80" },
                                                { "RuntimeEnvironmentVariables", new Dictionary<string, string>()
                                                    {
                                                        { "Authority", userPool.UserPoolProviderUrl },
                                                        { "LogoutUri", $"{userPoolDomain.BaseUrl()}/logout" },
                                                        { "ClientOrigin", $"https://{cloudFrontDistribution.DomainName}"},
                                                        { "BucketName", fileStorage.BucketName},
                                                        { "IdentityPoolId", identityPool.IdentityPoolId},
                                                        { "IdentityPoolRegion", identityPool.Env.Region},
                                                        { "ClientId", clientApp.UserPoolClientId},
                                                        { "ClientSecret", userPoolClientCustomResource.GetResponseField("UserPoolClient.ClientSecret")}
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    PhysicalResourceId = PhysicalResourceId.Of(appRunnerService.AttrServiceArn + "EnvironmentVariable")
                }
            });
            #endregion

            #region AWS LAMBDA FOR SERVING DYNAMIC CONFIGURATION
            IEnumerable<string> commands = new[]
            {
                "export DOTNET_CLI_HOME=\"/tmp/DOTNET_CLI_HOME\"",
                "export PATH=\"$PATH:/tmp/DOTNET_CLI_HOME/.dotnet/tools\"",
                "dotnet tool install -g Amazon.Lambda.Tools",
                "dotnet lambda package -o /asset-output/output.zip"
            };

            Function configFunction = new Function(this, "ConfigFunction", new FunctionProps
            {
                Runtime = Runtime.DOTNET_6,
                Code = Code.FromAsset(
                    Path.Join(Directory.GetParent(Directory.GetCurrentDirectory()).FullName,
                        nameof(ConfigFunction)
                    ),
                    new AssetOptions()
                    {
                        Bundling = new BundlingOptions
                        {
                            Image = Runtime.DOTNET_6.BundlingImage,
                            Command = new string[] { "bash", "-c", string.Join(" && ", commands) }
                        }
                    }
                    ),
                Handler = String.Join(".", nameof(MyLocalFarmer), nameof(ProofOfAddress), nameof(ConfigFunction))
            });

            LambdaRestApi configApi = new LambdaRestApi(this, "ConfigApi", new LambdaRestApiProps()
            {
                Handler = configFunction,
            });

            cloudFrontDistribution.AddBehavior("config", new RestApiOrigin(configApi), new BehaviorOptions()
            {
                AllowedMethods = AllowedMethods.ALLOW_GET_HEAD,
                CachePolicy = CachePolicy.CACHING_DISABLED
            });

            AwsCustomResource awsCustomResource = new AwsCustomResource(this, "ConfigApiEnvironmentVariables", new AwsCustomResourceProps()
            {
                InstallLatestAwsSdk = false,
                Policy = AwsCustomResourcePolicy.FromSdkCalls(new SdkCallsPolicyOptions()
                {
                    Resources = new string[] { configFunction.FunctionArn }
                }),
                OnCreate = new AwsSdkCall()
                {
                    Service = "Lambda",
                    Action = "updateFunctionConfiguration",
                    Parameters = new Dictionary<string, object>
                    {
                        { "FunctionName", configFunction.FunctionArn },
                        { "Environment", new Dictionary<string, object>()
                            {
                                { "Variables", new Dictionary<string, string>()
                                    {
                                        { MyLocalFarmer.ProofOfAddress.ConfigFunction.Config.GET_PRESIGNED_URL.ToString(), $"https://{appRunnerService.AttrServiceUrl}/presigned" },
                                        { MyLocalFarmer.ProofOfAddress.ConfigFunction.Config.LOGIN_URL.ToString(), $"https://{appRunnerService.AttrServiceUrl}/auth/login" },
                                        { MyLocalFarmer.ProofOfAddress.ConfigFunction.Config.LOGOUT_URL.ToString(), $"https://{appRunnerService.AttrServiceUrl}/auth/logout" },
                                        { MyLocalFarmer.ProofOfAddress.ConfigFunction.Config.GET_CURRENT_USER_URL.ToString(), $"https://{appRunnerService.AttrServiceUrl}/auth/getcurrentuser" }
                                    }
                                }
                            }
                        }
                    },
                    PhysicalResourceId = PhysicalResourceId.Of(configFunction.FunctionName + "EnvironmentVariables")
                },
                OnUpdate = new AwsSdkCall()
                {
                    Service = "Lambda",
                    Action = "updateFunctionConfiguration",
                    Parameters = new Dictionary<string, object>
                    {
                        { "FunctionName", configFunction.FunctionArn },
                        { "Environment", new Dictionary<string, object>()
                            {
                                { "Variables", new Dictionary<string, string>()
                                    {
                                        { MyLocalFarmer.ProofOfAddress.ConfigFunction.Config.GET_PRESIGNED_URL.ToString(), $"https://{appRunnerService.AttrServiceUrl}/presigned" },
                                        { MyLocalFarmer.ProofOfAddress.ConfigFunction.Config.LOGIN_URL.ToString(), $"https://{appRunnerService.AttrServiceUrl}/auth/login" },
                                        { MyLocalFarmer.ProofOfAddress.ConfigFunction.Config.LOGOUT_URL.ToString(), $"https://{appRunnerService.AttrServiceUrl}/auth/logout" },
                                        { MyLocalFarmer.ProofOfAddress.ConfigFunction.Config.GET_CURRENT_USER_URL.ToString(), $"https://{appRunnerService.AttrServiceUrl}/auth/getcurrentuser" }
                                    }
                                }
                            }
                        }
                    },
                    PhysicalResourceId = PhysicalResourceId.Of(configFunction.FunctionName + "EnvironmentVariables")
                }
            });
            awsCustomResource.Node.AddDependency(appRunnerService);
            #endregion
        }

    }
}
