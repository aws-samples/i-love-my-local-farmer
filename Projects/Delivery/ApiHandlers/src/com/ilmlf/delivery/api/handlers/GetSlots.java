package com.ilmlf.delivery.api.handlers;

import com.ilmlf.delivery.api.handlers.service.SlotService;
import com.ilmlf.delivery.api.handlers.util.ApiUtil;
import java.sql.SQLException;
import java.time.Period;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;

import org.json.JSONArray;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class GetSlots implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private SlotService slotService;

  public GetSlots() {
    this.slotService = new SlotService();
  }

  public GetSlots(SlotService slotService) {
    this.slotService = slotService;
  }

  /**
   * Handle get-slots GET via Api Gateway
   * pathParameters expected: {farm-id=Integer}
   * @return 200: success<br/>
   *        4xx: thrown if the farm-id is invalid or not found in the database<br/>
   *        5xx: if slots cannot be retrieved for the given farm
   */
  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    String returnVal = "";
    Integer httpStatus = 200;
    LocalDate availableSlotsBeginDate = LocalDate.now(ZoneId.of("UTC"));
    LocalDate availableSlotsEndDate = availableSlotsBeginDate.plus(Period.ofDays(14));
    Integer farmId;
    ArrayList<Slot> slotArray;

    try {
      farmId = Integer.parseInt(input.getPathParameters().get("farm-id"));
    } catch (NumberFormatException exception) {
      throw new RuntimeException("Farm id must not be blank, and must be a valid integer");
    }

    try {
      slotArray = slotService.getSlots(farmId, availableSlotsBeginDate, availableSlotsEndDate);

      if (slotArray.isEmpty()) {
        httpStatus = 400;
        returnVal = "No slots found matching the farm id";
      } else {
        JSONArray slotJsonArray = new JSONArray(slotArray);
        returnVal = slotJsonArray.toString();
      }

    } catch (SQLException exception) {
      httpStatus = 500;
      returnVal = "Error encountered while retrieving slots from database";
      exception.printStackTrace();
    }

    return ApiUtil.generateReturnData(httpStatus, returnVal);
  }
}