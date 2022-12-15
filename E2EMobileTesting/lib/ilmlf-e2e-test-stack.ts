import { Construct } from 'constructs';
import {
  Stack,
  StackProps,
  aws_iam,
  custom_resources,
  aws_codepipeline,
  aws_codecommit,
  aws_s3,
  aws_codebuild,
  Fn,
  aws_events_targets,
} from 'aws-cdk-lib';

export class IlmlfE2ETestStack extends Stack {
  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);
    const devicefarm = new custom_resources.AwsCustomResource(this, 'DeviceFarm', {
      onCreate: {
        service: 'DeviceFarm',
        action: 'createProject',
        parameters: {
          name: 'ilovemylocalfarmerWeb',
        },
        physicalResourceId: custom_resources.PhysicalResourceId.fromResponse('project.arn'),
      },
      onDelete: {
        service: 'DeviceFarm',
        action: 'deleteProject',
        parameters: {
          arn: new custom_resources.PhysicalResourceIdReference(),
        },
      },
      policy: custom_resources.AwsCustomResourcePolicy.fromSdkCalls({
        resources: custom_resources.AwsCustomResourcePolicy.ANY_RESOURCE,
      }),
    });
    const deviceFarmArn = devicefarm.getResponseField('project.arn');
    const deviceFarmProjectId = Fn.select(6, Fn.split(':', deviceFarmArn));

    const devicePool = new custom_resources.AwsCustomResource(this, 'DevicePool', {
      onCreate: {
        service: 'DeviceFarm',
        action: 'createDevicePool',
        parameters: {
          name: 'Old Androids',
          maxDevices: 2,
          projectArn: deviceFarmArn,
          rules: [
            { attribute: 'PLATFORM', operator: 'EQUALS', value: '"ANDROID"' },
            { attribute: 'OS_VERSION', operator: 'LESS_THAN', value: '"9.0.0"' },
          ],
        },
        physicalResourceId: custom_resources.PhysicalResourceId.fromResponse('devicePool.arn'),
      },
      onDelete: {
        service: 'DeviceFarm',
        action: 'deleteDevicePool',
        parameters: {
          arn: new custom_resources.PhysicalResourceIdReference(),
        },
      },
      policy: custom_resources.AwsCustomResourcePolicy.fromSdkCalls({
        resources: custom_resources.AwsCustomResourcePolicy.ANY_RESOURCE,
      }),
    });
    const devicePoolArn = devicePool.getResponseField('devicePool.arn');

    const repo = new aws_codecommit.Repository(this, 'RepoJava', {
      repositoryName: 'java-web',
      code: aws_codecommit.Code.fromDirectory('uiTest', 'main'),
    });

    const sourceOutput = new aws_codepipeline.Artifact('srcOutput');
    const buildOutput = new aws_codepipeline.Artifact('buildOutput');

    const pipelineRole = new aws_iam.Role(this, 'CodePipelineRole', {
      assumedBy: new aws_iam.ServicePrincipal('codepipeline.amazonaws.com'),
      managedPolicies: [
        aws_iam.ManagedPolicy.fromAwsManagedPolicyName('AWSCodeCommitFullAccess'),
        aws_iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonS3FullAccess'),
        aws_iam.ManagedPolicy.fromAwsManagedPolicyName('AWSCodeBuildDeveloperAccess'),
        aws_iam.ManagedPolicy.fromAwsManagedPolicyName('AWSDeviceFarmFullAccess'),
      ],
    });

    const buildFilePath = 'target/zip-with-dependencies.zip';
    const codebuild = new aws_codebuild.PipelineProject(this, 'MyProject', {
      buildSpec: aws_codebuild.BuildSpec.fromObject({
        version: '0.2',
        artifacts: {
          files: buildFilePath,
        },
        phases: {
          build: {
            commands: ['mvn clean package -DskipTests=true'],
          },
        },
      }),
    });
    codebuild.role?.addManagedPolicy(aws_iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonS3FullAccess'));

    const bucket = new aws_s3.Bucket(this, 'PipelineBucket');

    const codepipelineName = 'DeviceFarmPipeline';
    const codepipeline = new aws_codepipeline.CfnPipeline(this, 'Pipeline', {
      artifactStore: { type: 'S3', location: bucket.bucketName },
      name: codepipelineName,
      roleArn: pipelineRole.roleArn,
      stages: [
        {
          name: 'Source',
          actions: [
            {
              name: 'CodeCommit',
              actionTypeId: {
                category: 'Source',
                owner: 'AWS',
                provider: 'CodeCommit',
                version: '1',
              },
              configuration: {
                BranchName: 'main',
                PollForSourceChanges: 'false',
                RepositoryName: repo.repositoryName,
              },
              outputArtifacts: [
                {
                  name: sourceOutput.artifactName!,
                },
              ],
            },
          ],
        },
        {
          name: 'Build',
          actions: [
            {
              name: 'CodeBuild',
              actionTypeId: {
                category: 'Build',
                owner: 'AWS',
                provider: 'CodeBuild',
                version: '1',
              },
              configuration: {
                ProjectName: codebuild.projectName,
              },
              outputArtifacts: [
                {
                  name: buildOutput.artifactName!,
                },
              ],
              inputArtifacts: [
                {
                  name: sourceOutput.artifactName!,
                },
              ],
            },
          ],
        },
        {
          name: 'Test',
          actions: [
            {
              name: 'DeviceFarm',
              actionTypeId: {
                category: 'Test',
                owner: 'AWS',
                provider: 'DeviceFarm',
                version: '1',
              },
              configuration: {
                ProjectId: deviceFarmProjectId,
                DevicePoolArn: devicePoolArn,
                AppType: 'Web',
                Test: buildFilePath,
                TestType: 'APPIUM_WEB_JAVA_TESTNG',
              },
              inputArtifacts: [
                {
                  name: buildOutput.artifactName!,
                },
              ],
            },
          ],
        },
      ],
    });
    repo.onCommit('OnCommit', {
      target: new aws_events_targets.CodePipeline(
        aws_codepipeline.Pipeline.fromPipelineArn(this, 'CodePipeline', `arn:aws:codepipeline:${this.region}:${this.account}:${codepipelineName}`)
      ),
      branches: ['main'],
    });
  }
}
