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
