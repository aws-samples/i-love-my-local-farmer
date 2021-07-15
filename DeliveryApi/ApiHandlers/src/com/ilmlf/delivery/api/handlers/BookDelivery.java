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
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;


/**
 * A Lambda handler for BookDelivery API Call.
 */
public class BookDelivery implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private SlotService slotService;
  private MetricsLogger metricsLogger;
  private static final Logger logger = LogManager.getLogger(CreateSlots.class);

  /**
   * Constructor called by AWS Lambda.
   */
  public BookDelivery() {
    this.slotService = new SlotService();
    this.metricsLogger = new MetricsLogger();
  }

  /**
   * Constructor for unit testing. Allows test code to inject mocked SlotService.
   *
   * @param slotService Injected SlotService object.
   */
  public BookDelivery(SlotService slotService, MetricsLogger metricsLogger) {
    this.slotService = slotService;
    this.metricsLogger = metricsLogger;
  }

  /**
   * Reserve a delivery in the given slot.
   * pathParameters : {farm-id=Integer, slot-id=Integer}
   * bodyParameter : {user-id=Integer}
   *
   * @return 200: success<br/>
   *         4xx: thrown if any expected parameter is invalid or not found in the
   *         database<br/>
   *         5xx: if the slot isn't reserved (e.g. runs out of availability) OR
   *         internal error
   */
  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    Integer httpStatus;
    String returnVal;
    Integer farmId;
    Integer slotId;
    Integer userId;
    JSONObject jsonObjDelivery;

    metricsLogger.putDimensions(DimensionSet.of("FunctionName", "BookDelivery"));

    try {
      farmId = Integer.parseInt(event.getPathParameters().get("farm-id"));
      slotId = Integer.parseInt(event.getPathParameters().get("slot-id"));

      String body = event.getBody();
      JSONObject bodyJson = new JSONObject(body);
      Object userIdInJson = bodyJson.get("userId");
      if (!(userIdInJson instanceof Integer)) {
        metricsLogger.putMetric("InvalidUserId", 1, Unit.COUNT);
        throw new JSONException("userId must be an integer");
      }
      userId = (Integer) userIdInJson;

      Delivery delivery = slotService.bookDelivery(farmId, slotId, userId);
      jsonObjDelivery = new JSONObject(delivery);
      httpStatus = 200;
      returnVal = jsonObjDelivery.toString();
      metricsLogger.putMetric("DeliveryBooked", 1, Unit.COUNT);

    } catch (NumberFormatException exception) {
      logger.error(exception.getMessage(), exception);
      httpStatus = 400;
      returnVal = HandlerErrorMessage.FARM_AND_SLOT_INVALID.toString();
      metricsLogger.putMetric("FarmAndSlotInvalid", 1, Unit.COUNT);
    } catch (JSONException exception) {
      logger.error(exception.getMessage(), exception);
      httpStatus = 400;
      returnVal = HandlerErrorMessage.USER_INVALID.toString();
      metricsLogger.putMetric("InvalidUserId", 1, Unit.COUNT);
    } catch (SQLException exception) {
      logger.error(exception.getMessage(), exception);
      httpStatus = 500;
      returnVal = HandlerErrorMessage.SQL_FAILED.toString();
      metricsLogger.putMetric("SqlException", 1, Unit.COUNT);
    } catch (IllegalStateException exception) {
      logger.error(exception.getMessage(), exception);
      httpStatus = 500;
      returnVal = HandlerErrorMessage.NO_AVAILABLE_DELIVERY.toString();
      metricsLogger.putMetric("NoAvailableDelivery", 1, Unit.COUNT);
    }

    metricsLogger.flush();

    return ApiUtil.generateReturnData(httpStatus, returnVal);
  }

}


