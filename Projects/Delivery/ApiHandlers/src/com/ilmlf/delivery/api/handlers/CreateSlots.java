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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Create Slots Api call.
 */
public class CreateSlots implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private SlotService slotService;

  /**
   * Constructor called through AWS Lambda.
   */
  public CreateSlots() {
    this.slotService = new SlotService();
    System.out.println("CreateSlots empty constructor, called by AWS Lambda");
  }

  /**
   * Constructor for unit testing.
   * 
   * @param slotService the mocked SlotService instance
   */
  public CreateSlots(SlotService slotService) {
    this.slotService = slotService;
    System.out.println("CreateSlots constructor for unit testing, allowing injection of mock SlotService");
  }

  /**
   * Handle create-slots POST via Api Gateway
   * pathParameters expected: {farm-id=Integer}
   * POST Body expected: {
      slots: [
          {
              numDeliveries: "2",
              from: "2020-01-01T10:00:00",
              to: "2020-01-01T10:00:00"
          }
      ]}

   * @return 200: success<br/>
   *        4xx: if request doesn't come from authenticated client app<br/>
   *        5xx: if slot can't be persisted
   */
  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    String returnVal = "";
    Integer httpStatus = 200;
    List<Slot> slotList = new ArrayList<Slot>();
    
    try {
      String farmIdStr = input.getPathParameters().get("farm-id");
      String body = input.getBody();
      slotList = parseAndCreateSlotList(body, farmIdStr);
      
    } catch (Exception e) {
      returnVal = "The data received is incomplete or invalid";
      httpStatus = 400;
      e.printStackTrace();
    }
    
    if (!slotList.isEmpty()) {
      try {
        int rowsUpdated = slotService.insertSlotList(slotList);

        if (rowsUpdated == 0) {
          returnVal = "There was an error and the data could not be saved";
          httpStatus = 500;
        } else {
          returnVal = "Slot data (" + rowsUpdated + ") was saved successfully"; 
        }

      } catch (Exception e) {
        returnVal = "Error encountered while inserting the slot list";
        httpStatus = 500;
        e.printStackTrace();
      }
    }
    return ApiUtil.generateReturnData(httpStatus, returnVal);
  }

  /**
   * Parses the POST body and creates a list of Slot objects from it.
   * 
   * @param body the Json formatted body of the request
   * @param farmIdStr the farmId as a String
   * 
   * @return the List of Slot objects
   */
  public List<Slot> parseAndCreateSlotList(String body, String farmIdStr) {
    JSONObject bodyJson = new JSONObject(body);
    JSONArray slots = bodyJson.getJSONArray("slots");
    Stream<Object> slotsStream = StreamSupport.stream(slots.spliterator(), false);
    
    List<Slot> slotList = slotsStream
        .map(slot -> (Slot) parseAndCreateSlot((JSONObject) slot, farmIdStr))
        .collect(Collectors.toList());
    
    return slotList;
  }
  
  /**
   * Parses Json data and appends farmId to create a Slot object. <br/>
   * If any errors/exceptions encountered, will throw a RuntimeException
   * (in order for the function to be called via a lambda)
   * 
   * @param slotJson the Json format slot data
   * @param farmIdStr the farmId as a String
   *
   * @return the Slot object
   */
  public Slot parseAndCreateSlot(JSONObject slotJson, String farmIdStr) {
    String fromStr = slotJson.getString("from");
    LocalDateTime slotFrom = Slot.getLocalDateTimeFromIso(fromStr);
    Slot slot = Slot.builder()
        .farmId(Integer.parseInt(farmIdStr))
        .from(slotFrom)
        .to(Slot.getLocalDateTimeFromIso(slotJson.getString("to")))
        .availDeliveries(slotJson.getInt("numDeliveries"))
        .bookedDeliveries(0)
        .deliveryDate(slotFrom.toLocalDate())
        .build();

    return slot;
  }
}
