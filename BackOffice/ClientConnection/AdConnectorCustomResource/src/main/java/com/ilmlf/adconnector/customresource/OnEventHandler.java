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
import com.ilmlf.customresource.utils.CloudFormationCustomResourceOnEventResponse;
import software.amazon.awssdk.services.directory.DirectoryClient;
import software.amazon.awssdk.services.directory.model.ConnectDirectoryRequest;
import software.amazon.awssdk.services.directory.model.DeleteDirectoryRequest;
import software.amazon.awssdk.services.directory.model.DirectoryConnectSettings;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.ilmlf.customresource.utils.SecretsUtil.getSecretValue;


public class OnEventHandler implements RequestHandler<CloudFormationCustomResourceEvent, CloudFormationCustomResourceOnEventResponse> {

    DirectoryClient directoryClient = DirectoryClient.builder().build();

    @Override
    public CloudFormationCustomResourceOnEventResponse handleRequest(CloudFormationCustomResourceEvent event, Context context) {
        Map<String, Object> properties = event.getResourceProperties();
        String secret = getSecretValue(properties.get("secretId").toString());

        String directoryId = event.getPhysicalResourceId();
        switch(event.getRequestType()) {
            case "Create":
                directoryId = directoryClient
                        .connectDirectory(ConnectDirectoryRequest.builder()
                                .connectSettings(DirectoryConnectSettings.builder()
                                        .customerUserName("Admin")
                                        .customerDnsIps((Collection<String>) properties.get("dnsIps"))
                                        .subnetIds((Collection<String>) properties.get("subnetIds"))
                                        .vpcId((String) properties.get("vpcId"))
                                        .build())
                                .name(properties.get("domainName").toString())
                                .password(secret)
                                .size("Small")
                                .build())
                        .directoryId();
                break;
            case "Update":
                break;
            case "Delete":
                directoryClient.deleteDirectory(DeleteDirectoryRequest.builder().directoryId(directoryId).build());
                break;
        }

            CloudFormationCustomResourceOnEventResponse response = CloudFormationCustomResourceOnEventResponse.fromEvent(event);

            response.setPhysicalResourceId(directoryId);
            response.setData(Collections.singletonMap("DirectoryId", directoryId));
            return response;
    }
}
