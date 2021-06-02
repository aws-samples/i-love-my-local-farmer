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
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import java.util.List;
import software.amazon.awssdk.services.directory.DirectoryClient;
import software.amazon.awssdk.services.directory.model.DescribeDirectoriesRequest;
import software.amazon.awssdk.services.directory.model.DirectoryDescription;
import software.amazon.awssdk.services.directory.model.DirectoryStage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IsCompleteHandler implements RequestHandler<CloudFormationCustomResourceEvent, Map> {

  private DirectoryDescription directory;

  @Override
  public HashMap<String, Object> handleRequest(
      CloudFormationCustomResourceEvent event, Context context) {
    System.out.println("isCompleteHandler event: " + event.toString());

    HashMap<String, Object> response = new HashMap();

    /*
      Check for the completion of OnEventHandler actions requires that we
        check the currently listed directories in the Directory Service,
        hence fetch them here.
    */
    String physicalResourceId = event.getPhysicalResourceId();
    List<DirectoryDescription> directoryDescriptions =
        DirectoryClient.builder()
            .build()
            .describeDirectories(
                DescribeDirectoriesRequest.builder()
                    .directoryIds(Collections.singletonList(physicalResourceId))
                    .build())
            .directoryDescriptions();
    Map<String, String> data = Map.of("DirectoryId", physicalResourceId);

    Boolean isComplete;

    /*
      For deletion requests, check that the list of returned directories is empty.
    */
    if (event.getRequestType().equals("Delete")) {
      isComplete = directoryDescriptions.isEmpty();
    /*
      For "Create" requests, check that the first (and only) directory in the list
      has stage 'Active', i.e. it is an Active Directory directory.
    */
    } else {
      isComplete = directoryDescriptions.get(0).stage().equals(DirectoryStage.ACTIVE);
    }
    response.put("IsComplete", isComplete);

    if (isComplete) {
      response.put("Data", data);
    }

    System.out.println("response: " + response);
    return response;
  }
}
