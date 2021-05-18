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

package com.ilmlf.customresource.utils;

import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloudFormationCustomResourceOnEventResponse extends CloudFormationCustomResourceEvent {


  @JsonProperty("Data")
  private Map<String, String> data;

  public static CloudFormationCustomResourceOnEventResponse fromEvent(
      CloudFormationCustomResourceEvent evt) {
    CloudFormationCustomResourceOnEventResponse result =
        new CloudFormationCustomResourceOnEventResponse();
    result.setStackId(evt.getStackId());
    result.setRequestId(evt.getRequestId());
    result.setRequestType(evt.getRequestType());
    result.setLogicalResourceId(evt.getLogicalResourceId());

    result.setPhysicalResourceId(evt.getPhysicalResourceId());

    return result;
  }
}