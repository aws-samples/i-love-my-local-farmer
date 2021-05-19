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

import static com.ilmlf.adconnector.customresource.SecretsUtil.getSecretValue;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.directory.DirectoryClient;
import software.amazon.awssdk.services.directory.model.ConnectDirectoryRequest;
import software.amazon.awssdk.services.directory.model.DeleteDirectoryRequest;
import software.amazon.awssdk.services.directory.model.DirectoryConnectSettings;

/** CRUD AD Connector Async custom resource handler. */
public class OnEventHandler implements RequestHandler<CloudFormationCustomResourceEvent, Map> {

  DirectoryClient directoryClient = DirectoryClient.builder().build();

  @Override
  public HashMap<String, Object> handleRequest(
      CloudFormationCustomResourceEvent event, Context context) {

    System.out.println("onEventHandler event: " + event.toString());
    Map<String, Object> properties = event.getResourceProperties();
    String secret = getSecretValue(properties.get("secretId").toString());

    String directoryId = event.getPhysicalResourceId();
    System.out.println("action:" + event.getRequestType() + " AD Connector");
    String requestType = event.getRequestType();
    switch (requestType) {
      case "Create":
        directoryId =
            directoryClient
                .connectDirectory(
                    ConnectDirectoryRequest.builder()
                        .connectSettings(
                            DirectoryConnectSettings.builder()
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
        directoryClient.deleteDirectory(
            DeleteDirectoryRequest.builder().directoryId(directoryId).build());
        break;
      default:
        throw new InvalidParameterException("Invalid RequestType " + requestType);
    }

    HashMap<String, Object> response = new HashMap<>();
    response.put("PhysicalResourceId", directoryId);
    response.put("PhysicalResourceId", directoryId);
    response.put("RequestType", event.getRequestType());

    System.out.println("result: Successfully " + event.getRequestType() + " AD Connector.");
    System.out.println("response: " + response);

    return response;
  }
}
