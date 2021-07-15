package com.ilmlf.delivery.api.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.ilmlf.delivery.api.handlers.service.SlotService;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.ilmlf.delivery.api.handlers.util.SlotParser;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

/**
 * Unit tests for CreateSlots handler.
 * It injects a mocked SlotService to the handler class and
 * checks that the handler returns correct responses for both success and failure scenarios.
 * It also checks that parsing and creating slots is performed correctly within the helper methods in CreateSlots
 */
@ExtendWith(MockitoExtension.class)
class CreateSlotsTest {
  private String defaultDate = "2020-01-01T10:00:00";
  private String singleSlotStr = "{numDeliveries: \"2\",from: \"2020-01-01T10:00:00\",to: \"2020-01-01T10:00:00\"}";
  private String jsonWrapperStart = "{slots: [";
  private String jsonWrapperEnd = "]}";

  private CreateSlots cs;
  private SlotService slotService;
  private MetricsLogger metricsLogger;
  private SlotParser slotParser;

  @BeforeEach
  public void setUp() {
    slotService = Mockito.mock(SlotService.class);
    this.metricsLogger = Mockito.mock(MetricsLogger.class);
    this.slotParser = new SlotParser();

    cs = new CreateSlots(slotService, this.metricsLogger, this.slotParser);
  }

  @Test
  public void testParseAndCreateSlotListGood() {
    List<Slot> slots = slotParser.parseAndCreateSlotList(jsonWrapperStart + singleSlotStr + jsonWrapperEnd, "2");
    assertNotNull(slots);
    assertEquals(slots.size(), 1);

    slots = slotParser.parseAndCreateSlotList(jsonWrapperStart + singleSlotStr + "," + singleSlotStr + jsonWrapperEnd, "2");
    assertNotNull(slots);
    assertEquals(slots.size(), 2);
  }

  @Test
  public void testParseAndCreateSlotListInvalid() {
    assertThrows(Exception.class, () -> slotParser.parseAndCreateSlotList("", "2"));
    assertThrows(Exception.class,
          () -> slotParser.parseAndCreateSlotList(jsonWrapperStart + "badjson" + singleSlotStr + jsonWrapperEnd, "2"));
  }

  @Test
  public void testParseAndCreateSlotGood() {
    try {
      JSONObject slotJson = constructSlotJson(2, defaultDate, defaultDate);
      Slot slot = SlotParser.parseAndCreateSlot(slotJson, "2");
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
          SlotParser.parseAndCreateSlot(constructSlotJson(null, defaultDate, defaultDate), "2"));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, null, defaultDate), "2"));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, defaultDate, null), "2"));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, defaultDate, defaultDate), ""));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, defaultDate, defaultDate), null));

    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  public void testParseAndCreateSlotInvalidArgs() {
    try {
      // nothing json to parse
      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(new JSONObject(), "2"));

      // null inputs
      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(null, defaultDate, defaultDate), "2"));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, null, defaultDate), "2"));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, defaultDate, null), "2"));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, defaultDate, defaultDate), null));

      // empty string inputs
      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson("", defaultDate, defaultDate), "2"));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, "", defaultDate), "2"));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, defaultDate, ""), "2"));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, defaultDate, ""), "2"));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, defaultDate, defaultDate), ""));

      // invalid data-type input
      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson("s", defaultDate, defaultDate), "2"));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, "s", defaultDate), "2"));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, defaultDate, "s"), "2"));

      assertThrows(RuntimeException.class, () ->
          SlotParser.parseAndCreateSlot(constructSlotJson(2, defaultDate, defaultDate), "s"));

    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  public void testHandlerMissingArguments() {
    this.metricsLogger = MockProvider.getMockMetricsLogger();
    this.cs = new CreateSlots(this.slotService, this.metricsLogger, this.slotParser);

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    APIGatewayProxyResponseEvent response = this.cs.handleRequest(request, Mockito.mock(Context.class));

    assertEquals(400, response.getStatusCode());
  }

  @Test
  public void testHandlerBadParsing() throws NullPointerException {
    this.slotParser = Mockito.mock(SlotParser.class);
    this.metricsLogger = MockProvider.getMockMetricsLogger();
    this.cs = new CreateSlots(this.slotService, this.metricsLogger, this.slotParser);

    Mockito.when(this.slotParser.parseAndCreateSlotList(Mockito.any(), Mockito.any()))
        .thenThrow(new NullPointerException());

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", "1"
        ))
        .withBody("anyinvalidbody");

    APIGatewayProxyResponseEvent response = cs.handleRequest(request, Mockito.mock(Context.class));

    assertEquals(400, response.getStatusCode());
  }

  @Test
  public void testHandlerSqlProblem() throws SQLException {
    this.metricsLogger = MockProvider.getMockMetricsLogger();
    this.cs = new CreateSlots(this.slotService, this.metricsLogger, this.slotParser);
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
    this.metricsLogger = MockProvider.getMockMetricsLogger();
    this.cs = new CreateSlots(this.slotService, this.metricsLogger, this.slotParser);
    Mockito.when(this.slotService.insertSlotList(Mockito.any())).thenReturn(1);

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", "1"
        ))
        .withBody(jsonWrapperStart + singleSlotStr + jsonWrapperEnd);

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
