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

package com.ilmlf.adconnector.customresource;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.ilmlf.customresource.utils.CloudFormationCustomResourceIsCompleteResponse;
import com.ilmlf.customresource.utils.CloudFormationCustomResourceOnEventResponse;
import software.amazon.awssdk.services.directory.DirectoryClient;
import software.amazon.awssdk.services.directory.model.DescribeDirectoriesRequest;
import software.amazon.awssdk.services.directory.model.DirectoryDescription;
import software.amazon.awssdk.services.directory.model.DirectoryStage;

import java.util.Collections;

public class IsCompleteHandler implements RequestHandler<CloudFormationCustomResourceOnEventResponse, CloudFormationCustomResourceIsCompleteResponse> {

    @Override
    public CloudFormationCustomResourceIsCompleteResponse handleRequest(CloudFormationCustomResourceOnEventResponse event, Context context) {

        DirectoryDescription directory = DirectoryClient.builder().build()
                .describeDirectories(DescribeDirectoriesRequest.builder()
                        .directoryIds(Collections.singletonList((String) event.getData().get("DirectoryId")))
                        .build()).directoryDescriptions().get(0);
        CloudFormationCustomResourceIsCompleteResponse response = new CloudFormationCustomResourceIsCompleteResponse();
        response.setIsComplete(directory.stage().equals(DirectoryStage.ACTIVE));
        return response;
    }
}
