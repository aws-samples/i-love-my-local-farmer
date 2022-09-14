# Proof of Address Application

The Proof of Address application is the companion app of this post from *I Love My Local Farmer* Medium publication. It demonstrates how to build and deploy a Blazor WebAssembly application that leverages AWS services for authentication, authorization, file storage, hosting and content delivery.

## Pre-requisites

In order, to build and deploy this application sample, you need:

1. [Docker Engine](https://docs.docker.com/engine/install/) up and running
1. [git](https://git-scm.com/doc)
1. [.NET 6 SDK](https://docs.microsoft.com/en-us/dotnet/core/install/)
1. [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
1. Configured AWS credentials using the `aws configure` command
1. [AWS CDK](https://docs.aws.amazon.com/cdk/v2/guide/getting_started.html#getting_started_prerequisites)


You can open the .NET solution in your preferred IDE (Visual Studio, VS Code or JetBrains Rider) but it is not required.

## Regions

At the time of writing, AWS App Runner is deployed in US East (Ohio), US East (N. Virginia), US West (Oregon), Asia Pacific (Tokyo) and Europe (Ireland).

When you want to deploy this sample application, be sure to select an AWS Regions where AWS App Runner is deployed. You can check [here](https://docs.aws.amazon.com/general/latest/gr/apprunner.html ). 

## Architecture

This sample application leverages the Backend-For-Frontend (BFF) pattern to keep token management and interactions with AWS services on the backend side. 

![architecture diagram](./doc/architecture-diagram.png)

It leverages Amazon S3 to host the single-page application (SPA) and Amazon CloudFront to deliver it. When a user navigates to the application URL, Amazon CloudFront servers the SPA (*1* on the diagram). If the SPA is not already cached, it retrieves it from an Amazon S3 origin (*2*).

Once loaded, users can log in. When they click on the *login* button, they are redirected to an endpoint of the BFF API (*3*). Then, the API redirects them to an Amazon Cognito Hosted UI to log in (*4*). Once logged in, Amazon Cognito redirects users to the BFF API callback endpoint with an authorization code (*5*). The BFF API exchanges the authorization code for id and access tokens (*6*).

Here, the tokens are stored in an authentication cookie which is HTTP-only (*7*). Thus, they cannot be accessed from any JavaScript code running in the browser to avoid attack. The BFF API redirects users to the Blazor WebAssembly application with the authentication cookie (*8*).

The BFF API wraps any request to AWS services. For example, to get a presigned URL from Amazon S3, the Blazor WebAssembly application calls the BFF API adding the authentication cookie to the request (*9*). The BFF API decodes the authentication cookie and uses Amazon Cognito to exchange the id token for user temporary credentials (*10*). The user temporary credentials allow the BFF API to call Amazon S3 on behalf of the logged in user to get a presigned URL (*11*). It only works if an Amazon Cognito is configured to grant permissions to authenticated users to request Amazon S3 a presigned URL. This requires a setup Amazon Cognition Identity Pool.

When the Amazon S3 presigned URL is returned to the Blazor WebAssembly application, it can leverage it to directly upload a picture to Amazon S3 (*12*).

## Code Structure

This repository is divided in two main parts:

* the *doc* folder containing the architecture diagram ppt file and related images
* the *src* folder containing the source code of this sample

At the root of the *src* folder you find the .NET solution project file *MyLocalFarmer.ProofOfAddress.sln*. It contains also five subfolders:

* the *Web* folder containing the Blazor WebAssembly project
* the *API* folder containing the ASP.NET Core BFF API project
* the *Shared* folder containing a Shared Code library
* the *ConfigFunction* folder containing a .NET AWS Lambda function that serves Blazor WebAssembly application configuration dynamically based on the deployed environment  
* the *Infra* folder containing an AWS Cloud Development Kit (AWS CDK) application written in C# that deploys the required AWS resources and the application.

## Build and deployment steps

Before running these steps, check that your environment matches all pre-requisites.

On Linux, if your docker daemon runs with privileges, you may need to prefix `cdk` commands with `sudo`. 

To build and deploy the sample, run the following commands:

```bash
git clone https://github.com/aws-samples/i-love-my-local-farmer.git
cd proof-of-address/src
dotnet build
cd Infra
cdk synth
cdk deploy
```

Confirm that you agree to create the displayed IAM policies and roles.

The above steps have been tested on Windows 11 and Amazon Linux 2 with .NET 6, PowerShell, Mono, and MATE Desktop Environment AMI.

## How to clean your environment

Once you've tested the sample, we recommend to delete the deployed stack as costs may occur.

From the root folder of your local git repository clone, run the following commands:

```bash
cd  proof-of-address/src/Infra
cdk destroy
```

Confirm that you agree to delete all creates resources. 

**Important note:** as it is a sample, we also destroy the S3 buckets and the Amazon Cognito User Pool and Identity Pool. 


## How to contribute

This code is a sample code. We don't plan any updates on it. However, if you can still open an issue or pull request and we will be happy to handle it.