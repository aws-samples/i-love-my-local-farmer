package com.ilmlf.delivery.api.handlers.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public abstract class ApiUtil {

  public static APIGatewayProxyResponseEvent generateReturnData(Integer httpStatus, String message) {
    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
    response.setStatusCode(httpStatus);
    response.setBody(message);
    return response;
  }
}
