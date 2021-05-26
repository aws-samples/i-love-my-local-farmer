package com.ilmlf.delivery.api.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;


import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import com.ilmlf.delivery.api.handlers.service.SlotService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@ExtendWith(MockitoExtension.class)
class CreateSlotsTest {
  private String defaultDate = "2020-01-01T10:00:00";
  private String singleSlotStr = "{numDeliveries: \"2\",from: \"2020-01-01T10:00:00\",to: \"2020-01-01T10:00:00\"}";
  private String jsonWrapperStart = "{slots: [";
  private String jsonWrapperEnd = "]}";

  private CreateSlots cs;
  
  @BeforeEach
  public void setUp() throws Exception {
    SlotService slotService = Mockito.mock(SlotService.class);
    cs = new CreateSlots(slotService);
  }

  @Test
  void testParseAndCreateSlotListGood() {
    //System.out.println(jsonWrapperStart + singleSlotStr + jsonWrapperEnd);
    List<Slot> slots = cs.parseAndCreateSlotList(jsonWrapperStart + singleSlotStr + jsonWrapperEnd, "2");
    assertNotNull(slots);
    assertEquals(slots.size(), 1);
    
    slots = cs.parseAndCreateSlotList(jsonWrapperStart + singleSlotStr + "," + singleSlotStr + jsonWrapperEnd, "2");
    assertNotNull(slots);
    assertEquals(slots.size(), 2);
  }
  
  @Test
  void testParseAndCreateSlotListInvalid() {
    assertThrows(Exception.class, () -> cs.parseAndCreateSlotList("", "2"));
    assertThrows(Exception.class, () -> cs.parseAndCreateSlotList(jsonWrapperStart + "badjson" + singleSlotStr + jsonWrapperEnd, "2"));
  }
  
  @Test
  void testParseAndCreateSlotGood() {
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
      
    } catch ( Exception e) {
      fail(e);
    }
  }
  
  @Test
  void testParseAndCreateSlotMissingArgs() {
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

    } catch ( Exception e) {
      fail(e);
    }
  }

 // @Test
  void testParseAndCreateSlotInvalidArgs() {
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

    } catch ( Exception e) {
      fail(e);
    }
  }
  
  /**
   * Helper function to construct a Json object based on arguments
   * @param numDeliveries
   * @param from
   * @param to
   * @return
   */
  public JSONObject constructSlotJson(Object numDeliveries, Object from, Object to) {
    JSONObject jsonObj = new JSONObject();
    if (numDeliveries != null) jsonObj.put("numDeliveries", numDeliveries);
    if (from != null) jsonObj.put("from", from);
    if (to != null) jsonObj.put("to", to);

    return jsonObj;
  }
}
