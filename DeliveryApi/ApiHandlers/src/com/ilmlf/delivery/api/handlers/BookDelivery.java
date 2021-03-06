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

import static software.amazon.lambda.powertools.logging.CorrelationIdPathConstants.API_GATEWAY_REST;

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
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.logging.LoggingUtils;
import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsUtils;
import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.tracing.TracingUtils;

/**
 * A Lambda handler for BookDelivery API Call.
 */
public class BookDelivery implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private static final MetricsLogger metricsLogger = MetricsUtils.metricsLogger();
  private static final Logger logger = LogManager.getLogger(CreateSlots.class);
  private final SlotService slotService;

  /**
   * Constructor called by AWS Lambda.
   */
  @SuppressWarnings("unused")
  public BookDelivery() {
    this(new SlotService());
    metricsLogger.putDimensions(DimensionSet.of("FunctionName", "BookDelivery"));
  }

  /**
   * Constructor for unit testing. Allows test code to inject mocked SlotService.
   *
   * @param slotService Injected SlotService object.
   */
  BookDelivery(SlotService slotService) {
    this.slotService = slotService;
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
  @Logging(correlationIdPath = API_GATEWAY_REST)
  @Tracing
  @Metrics(captureColdStart = true)
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    int httpStatus;
    String returnVal;
    int farmId;
    int slotId;
    Integer userId;
    JSONObject jsonObjDelivery;

    try {
      String farm = event.getPathParameters().get("farm-id");
      String slot = event.getPathParameters().get("slot-id");
      farmId = Integer.parseInt(farm);
      slotId = Integer.parseInt(slot);

      String body = event.getBody();
      JSONObject bodyJson = new JSONObject(body);
      Object userIdInJson = bodyJson.get("userId");
      if (!(userIdInJson instanceof Integer)) {
        metricsLogger.putMetric("InvalidUserId", 1, Unit.COUNT);
        throw new JSONException("userId must be an integer");
      }
      userId = (Integer) userIdInJson;

      LoggingUtils.appendKey("farmId", farm);
      LoggingUtils.appendKey("slotId", slot);
      LoggingUtils.appendKey("userId", String.valueOf(userId));
      TracingUtils.putAnnotation("farmId", farm);
      TracingUtils.putAnnotation("userId", userId);
      TracingUtils.putAnnotation("slotId", slot);

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

    return ApiUtil.generateReturnData(httpStatus, returnVal);
  }

}


