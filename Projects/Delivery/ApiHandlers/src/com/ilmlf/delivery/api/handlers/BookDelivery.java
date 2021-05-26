package com.ilmlf.delivery.api.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.ilmlf.delivery.api.handlers.service.SlotService;
import com.ilmlf.delivery.api.handlers.util.ApiUtil;
import java.sql.SQLException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A Lambda handler for BookDelivery API Call.
 */
public class BookDelivery implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private SlotService slotService;

  public BookDelivery() {
    this.slotService = new SlotService();
  }

  public BookDelivery(SlotService slotService) {
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
  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    Integer httpStatus = 200;
    Integer farmId; 
    Integer slotId; 
    Integer userId;
    String returnVal;

    try {
      farmId = Integer.parseInt(event.getPathParameters().get("farm-id"));
      slotId = Integer.parseInt(event.getPathParameters().get("slot-id"));
    } catch (NumberFormatException exception) {
      return ApiUtil.generateReturnData(400, HandlerErrorMessage.FARM_AND_SLOT_INVALID.toString());
    }
    try {
      String body = event.getBody();
      JSONObject bodyJson = new JSONObject(body);
      Object userIdInJson = bodyJson.get("userId");
      if (!(userIdInJson instanceof Integer)) {
        return ApiUtil.generateReturnData(400, HandlerErrorMessage.USER_INVALID.toString());
      }
      userId = (Integer) userIdInJson;
    } catch (JSONException exception) {
      exception.printStackTrace();
      return ApiUtil.generateReturnData(400, HandlerErrorMessage.USER_INVALID.toString());
    }
    JSONObject jsonObjDelivery;

    try {
      Delivery delivery = slotService.bookDelivery(farmId, slotId, userId);
      jsonObjDelivery = new JSONObject(delivery);
      returnVal = jsonObjDelivery.toString();
    } catch (SQLException exception) {
      exception.printStackTrace();
      httpStatus = 500;
      returnVal = HandlerErrorMessage.SQL_FAILED.toString();
    } catch (IllegalStateException exception) {
      exception.printStackTrace();
      httpStatus = 500;
      returnVal = HandlerErrorMessage.NO_AVAILABLE_DELIVERY.toString();
    }

    return ApiUtil.generateReturnData(httpStatus, returnVal);
  }

}


