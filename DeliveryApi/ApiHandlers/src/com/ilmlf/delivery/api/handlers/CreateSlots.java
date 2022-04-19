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

package com.ilmlf.delivery.api.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.ilmlf.delivery.api.handlers.service.SlotService;
import com.ilmlf.delivery.api.handlers.util.ApiUtil;
import com.ilmlf.delivery.api.handlers.util.SlotParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.lambda.powertools.tracing.Tracing;

import java.util.ArrayList;
import java.util.List;

/**
 * A Lambda handler for CreateSlot API Call.
 */
public class CreateSlots implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private SlotService slotService;
  private MetricsLogger metricsLogger;
  private SlotParser slotParser;
  private static final Logger logger = LogManager.getLogger(CreateSlots.class);

  /**
   * Constructor called by AWS Lambda.
   */
  public CreateSlots() {
    this.slotService = new SlotService();
    this.metricsLogger = new MetricsLogger();
    this.slotParser = new SlotParser();
    this.metricsLogger.setNamespace("DeliveryApi");
    metricsLogger.putDimensions(DimensionSet.of("FunctionName", "CreateSlots"));
    logger.info("CreateSlots empty constructor, called by AWS Lambda");
  }

  /**
   * Constructor for unit testing. Allow test code to inject mocked SlotService.
   *
   * @param slotService the mocked SlotService instance
   */
  public CreateSlots(SlotService slotService, MetricsLogger metricsLogger, SlotParser slotParser) {
    this.slotService = slotService;
    this.metricsLogger = metricsLogger;
    this.slotParser = slotParser;
    logger.info("CreateSlots constructor for unit testing, allowing injection of mock SlotService");
  }

  /**
   * Handle create-slots POST via Api Gateway.
   * pathParameters expected: {farm-id=Integer}
   * <pre>
   * POST Body expected: {
      slots: [
          {
              numDeliveries: "2",
              from: "2020-01-01T10:00:00",
              to: "2020-01-01T10:00:00"
          }
      ]
   }
   * </pre>
   *
   * @return 200: success<br/>
   *        4xx: if request doesn't come from authenticated client app<br/>
   *        5xx: if slot can't be persisted
   */
  @Tracing
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    String returnVal = "";
    Integer httpStatus = 200;
    List<Slot> slotList = new ArrayList<>();

    try {
      String farmIdStr = input.getPathParameters().get("farm-id");
      String body = input.getBody();
      slotList = slotParser.parseAndCreateSlotList(body, farmIdStr);

    } catch (Exception e) {
      returnVal = "The data received is incomplete or invalid";
      httpStatus = 400;
      logger.error(e.getMessage(), e);

      metricsLogger.putMetric("InvalidSlotList", 1, Unit.COUNT);
    }

    if (!slotList.isEmpty()) {
      try {
        int rowsUpdated = slotService.insertSlotList(slotList);

        if (rowsUpdated == 0) {
          returnVal = "There was an error and the data could not be saved";
          httpStatus = 500;

          metricsLogger.putMetric("FailedToSaveSlots", 1, Unit.COUNT);
        } else {
          returnVal = "Slot data (" + rowsUpdated + ") was saved successfully";
        }

        metricsLogger.putMetric("SlotsCreated", rowsUpdated, Unit.COUNT);
      } catch (Exception e) {
        returnVal = "Error encountered while inserting the slot list";
        httpStatus = 500;
        logger.error(e.getMessage(), e);

        metricsLogger.putMetric("CreateSlotsException", 1, Unit.COUNT);
      }
    }

    metricsLogger.flush();

    return ApiUtil.generateReturnData(httpStatus, returnVal);
  }
}
