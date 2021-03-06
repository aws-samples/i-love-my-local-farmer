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

package com.ilmlf.delivery.api.handlers.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

/**
 * Utility class for API Gateway handlers.
 */
public abstract class ApiUtil {

  /**
   * Generate return data for API Gateway.
   *
   * @param httpStatus Returned HTTP code.
   * @param message Message to be returned.
   * @return a response to API Gateway
   */
  public static APIGatewayProxyResponseEvent generateReturnData(Integer httpStatus, String message) {
    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
    response.setStatusCode(httpStatus);
    response.setBody(message);

    return response;
  }
}
