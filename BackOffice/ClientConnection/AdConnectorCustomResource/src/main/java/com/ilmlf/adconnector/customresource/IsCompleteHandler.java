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
