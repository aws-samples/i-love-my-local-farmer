package com.ilmlf.adconnector.customresource;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

class OnEventHandlerTest {

  @org.junit.jupiter.api.Test
  void handleRequestTest() {
    Context context =
        new Context() {
          @Override
          public String getAwsRequestId() {
            return null;
          }

          @Override
          public String getLogGroupName() {
            return null;
          }

          @Override
          public String getLogStreamName() {
            return null;
          }

          @Override
          public String getFunctionName() {
            return null;
          }

          @Override
          public String getFunctionVersion() {
            return null;
          }

          @Override
          public String getInvokedFunctionArn() {
            return null;
          }

          @Override
          public CognitoIdentity getIdentity() {
            return null;
          }

          @Override
          public ClientContext getClientContext() {
            return null;
          }

          @Override
          public int getRemainingTimeInMillis() {
            return 0;
          }

          @Override
          public int getMemoryLimitInMB() {
            return 0;
          }

          @Override
          public LambdaLogger getLogger() {
            return null;
          }
        };

    OnEventHandler handler = new OnEventHandler();

    CloudFormationCustomResourceEvent event = new CloudFormationCustomResourceEvent();
    event.setRequestId("test");
    event.setRequestType("Create");

    handler.handleRequest(event, context);
  }
}
