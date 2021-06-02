# SiteToSiteConnection CDK App

Here is our CDK deployment for our work-from-home base infrastructure. It is the 'base infrastructure' because it is a prerequisite step to the client VPN set up. This is because it creates the VPC in which the Client VPN is later created.

This CDK app deploys an AWS VPC, and the Site-to-Site VPN connection between it and our on-premise environment.


## Using CDK

The `cdk.json` file tells the CDK Toolkit how to execute your app.

It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.

### Useful commands

 * `mvn package`     compile and run tests
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation
 
