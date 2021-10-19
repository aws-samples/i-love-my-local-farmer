/*
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.ilmlf.product.cicd;

import lombok.Data;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.SecretValue;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.core.StageProps;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.pipelines.GitHubSourceOptions;
import software.amazon.awscdk.pipelines.ShellStep;

import java.util.Arrays;
import java.util.Set;

/**
 * The main class for the pipeline stack.
 */
public class PipelineStack extends Stack {


    @lombok.Builder
    @Data
    public static class ProvmanEnv implements Environment {
        private String name;
        private String region;
        private String account;
    }

    @lombok.Builder
    @Data
    public static class PipelineStackProps implements StackProps {
        private Set<ProvmanEnv> stageEnvironments;
        private Environment env;
    }

    /**
     * Constructs a new pipeline stack.
     * @param scope
     * @param id
     * @param options specify the stages environment details (account and region) and pipeline environement
     * @throws Exception thrown in case of war build failure
     */
    public PipelineStack(Construct scope, String id, PipelineStackProps options) throws Exception {
        super(scope, id, options);
        CodePipeline pipeline = CodePipeline.Builder.create(this, "Pipeline")
                .crossAccountKeys(true)
                .pipelineName("ProvMan")
                .selfMutation(true)
                .synth(
                        ShellStep.Builder.create("Synth")
                                .input(CodePipelineSource.gitHub(
                                        "aws-samples/i-love-my-local-farmer",
                                        "main",
                                        GitHubSourceOptions.builder().authentication(
                                                SecretValue.secretsManager("GITHUB_TOKEN")).build()
                                        )
                                )
                                .commands(Arrays.asList("npm install -g aws-cdk", "cd cdk", "cdk synth"))
                                .primaryOutputDirectory("cdk/cdk.out")
                                .build())
                .build();

        for (ProvmanEnv stageEnvironment : options.stageEnvironments) {
            pipeline.addStage(new ProvmanStage(this, stageEnvironment.getName(), StageProps.builder().env(stageEnvironment).build()));
        }
    }
}


