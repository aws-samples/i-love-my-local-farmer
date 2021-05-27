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
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.ilmlf.delivery.api.handlers.CreateSlots;
import com.ilmlf.delivery.api.handlers.Slot;
import com.ilmlf.delivery.api.handlers.service.SlotService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
@ExtendWith(MockitoExtension.class)
class CreateSlotsTest {
  private String defaultDate = "2020-01-01T10:00:00";
  private String singleSlotStr = "{numDeliveries: \"2\",from: \"2020-01-01T10:00:00\",to: \"2020-01-01T10:00:00\"}";
  private String jsonWrapperStart = "{slots: [";
  private String jsonWrapperEnd = "]}";

  private CreateSlots cs;
  private SlotService slotService;
  
  @BeforeEach
  public void setUp() throws Exception {
    slotService = Mockito.mock(SlotService.class);
    cs = new CreateSlots(slotService);
  }

  @Test
  public void testParseAndCreateSlotListGood() {
    List<Slot> slots = cs.parseAndCreateSlotList(jsonWrapperStart + singleSlotStr + jsonWrapperEnd, "2");
    assertNotNull(slots);
    assertEquals(slots.size(), 1);
    
    slots = cs.parseAndCreateSlotList(jsonWrapperStart + singleSlotStr + "," + singleSlotStr + jsonWrapperEnd, "2");
    assertNotNull(slots);
    assertEquals(slots.size(), 2);
  }
  
  @Test
  public void testParseAndCreateSlotListInvalid() {
    assertThrows(Exception.class, () -> cs.parseAndCreateSlotList("", "2"));
    assertThrows(Exception.class, () -> cs.parseAndCreateSlotList(jsonWrapperStart + "badjson" + singleSlotStr + jsonWrapperEnd, "2"));
  }
  
  @Test
  public void testParseAndCreateSlotGood() {
    try { 
      JSONObject slotJson = constructSlotJson(2, defaultDate, defaultDate);
      Slot slot = cs.parseAndCreateSlot(slotJson, "2");
      assertNotNull(slot);
      assertEquals(slot.getAvailDeliveries(), 2);
      assertEquals(slot.getBookedDeliveries(), 0);
      
      LocalDateTime defaultDatetime = LocalDateTime.parse(defaultDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      assertEquals(slot.getFrom(), defaultDatetime);
      assertEquals(slot.getTo(), defaultDatetime);
      assertEquals(slot.getDeliveryDate(), defaultDatetime.toLocalDate());
      assertEquals(slot.getFarmId(), 2);
      
    } catch (Exception e) {
      fail(e);
    }
  }
  
  @Test
  public void testParseAndCreateSlotMissingArgs() {
    try { 
      assertThrows(RuntimeException.class, () -> 
          cs.parseAndCreateSlot(constructSlotJson(null, defaultDate, defaultDate), "2"));
      
      assertThrows(RuntimeException.class, () -> 
          cs.parseAndCreateSlot(constructSlotJson(2, null, defaultDate), "2"));

      assertThrows(RuntimeException.class, () -> 
          cs.parseAndCreateSlot(constructSlotJson(2, defaultDate, null), "2"));

      assertThrows(RuntimeException.class, () -> 
          cs.parseAndCreateSlot(constructSlotJson(2, defaultDate, defaultDate), ""));

      assertThrows(RuntimeException.class, () -> 
          cs.parseAndCreateSlot(constructSlotJson(2, defaultDate, defaultDate), null));

    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  public void testParseAndCreateSlotInvalidArgs() {
    try { 
      // nothing json to parse
      assertThrows(RuntimeException.class, () -> 
          cs.parseAndCreateSlot(new JSONObject(), "2"));
      
      // null inputs
      assertThrows(RuntimeException.class, () -> 
            cs.parseAndCreateSlot(constructSlotJson(null, defaultDate, defaultDate), "2"));

      assertThrows(RuntimeException.class, () -> 
            cs.parseAndCreateSlot(constructSlotJson(2, null, defaultDate), "2"));

      assertThrows(RuntimeException.class, () -> 
            cs.parseAndCreateSlot(constructSlotJson(2, defaultDate, null), "2"));
      
      assertThrows(RuntimeException.class, () -> 
            cs.parseAndCreateSlot(constructSlotJson(2, defaultDate, defaultDate), null));
      
      // empty string inputs
      assertThrows(RuntimeException.class, () -> 
            cs.parseAndCreateSlot(constructSlotJson("", defaultDate, defaultDate), "2"));

      assertThrows(RuntimeException.class, () -> 
            cs.parseAndCreateSlot(constructSlotJson(2, "", defaultDate), "2"));
      
      assertThrows(RuntimeException.class, () -> 
            cs.parseAndCreateSlot(constructSlotJson(2, defaultDate, ""), "2"));
      
      assertThrows(RuntimeException.class, () -> 
            cs.parseAndCreateSlot(constructSlotJson(2, defaultDate, ""), "2"));
      
      assertThrows(RuntimeException.class, () -> 
          cs.parseAndCreateSlot(constructSlotJson(2, defaultDate, defaultDate), ""));
      
      // invalid data-type input
      assertThrows(RuntimeException.class, () -> 
          cs.parseAndCreateSlot(constructSlotJson("s", defaultDate, defaultDate), "2"));
      
      assertThrows(RuntimeException.class, () -> 
          cs.parseAndCreateSlot(constructSlotJson(2, "s", defaultDate), "2"));
      
      assertThrows(RuntimeException.class, () -> 
          cs.parseAndCreateSlot(constructSlotJson(2, defaultDate, "s"), "2"));
      
      assertThrows(RuntimeException.class, () -> 
          cs.parseAndCreateSlot(constructSlotJson(2, defaultDate, defaultDate), "s"));

    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  public void testHandlerMissingArguments() {
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    APIGatewayProxyResponseEvent response = this.cs.handleRequest(request, Mockito.mock(Context.class));

    assertEquals(400, response.getStatusCode());
  }

  @Test
  public void testHandlerBadParsing() throws NullPointerException {
    CreateSlots mockCs = Mockito.mock(CreateSlots.class); 
    Mockito.when(mockCs.parseAndCreateSlotList(Mockito.any(), Mockito.any()))
        .thenThrow(new NullPointerException());
    Mockito.when(mockCs.handleRequest(Mockito.any(), Mockito.any()))
        .thenCallRealMethod();

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
                "farm-id", "1"
        ))
        .withBody("anyinvalidbody");
    APIGatewayProxyResponseEvent response = mockCs.handleRequest(request, Mockito.mock(Context.class));

    assertEquals(400, response.getStatusCode());
  }

  @Test
  public void testHandlerSqlProblem() throws SQLException {
    Mockito.when(this.slotService.insertSlotList(Mockito.any()))
        .thenThrow(new SQLException());

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
                "farm-id", "1"
        ))
        .withBody(jsonWrapperStart + singleSlotStr + jsonWrapperEnd);
    APIGatewayProxyResponseEvent response = this.cs.handleRequest(request, Mockito.mock(Context.class));

    assertEquals(500, response.getStatusCode());
  }

  @Test
  public void testHandlerSuccess() throws SQLException {
    Mockito.when(this.slotService.insertSlotList(Mockito.any())).thenReturn(1);

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
                "farm-id", "1"
        ))
        .withBody(jsonWrapperStart + singleSlotStr + jsonWrapperEnd);;
    APIGatewayProxyResponseEvent response = this.cs.handleRequest(request, Mockito.mock(Context.class));

    assertEquals(200, response.getStatusCode());
  }
  
  /**
   * Helper function to construct a Json object based on arguments.
   * 
   * @param numDeliveries Object with value to test for, if any
   * @param from Object with value to test for, if any
   * @param to Object with value to test for, if any
   * @return the JSONObject with the values set, if any were provided
   */
  public JSONObject constructSlotJson(Object numDeliveries, Object from, Object to) {
    JSONObject jsonObj = new JSONObject();
    if (numDeliveries != null) {
      jsonObj.put("numDeliveries", numDeliveries);
    }
    if (from != null) {
      jsonObj.put("from", from);
    }
    if (to != null) {
      jsonObj.put("to", to);
    }
    return jsonObj;
  }
}
