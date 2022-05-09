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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ilmlf.delivery.api.handlers.service.SlotService;
import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

/**
 * Unit tests for BookDelivery handler.
 * It injects a mocked SlotService to the handler class and
 * checks that the handler returns correct responses for both success and failure scenarios
 */
public class BookDeliveryTest {
  private BookDelivery bookDelivery;
  private SlotService slotService;
  private static Gson GSON;

  /**
   * Initialize GSON to deserialize the response body.
   */
  @BeforeAll
  public static void createGson() {
    GSON = new GsonBuilder().create();
  }

  @BeforeEach
  public void setup() {
    this.slotService = Mockito.mock(SlotService.class);

    this.bookDelivery = new BookDelivery(this.slotService);
  }

  @Test
  public void validRequestWithAvailableDelivery() throws SQLException {
    Integer deliveryId = 3;
    Delivery expectedDelivery = new Delivery(deliveryId);
    
    Mockito.when(this.slotService.bookDelivery(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(expectedDelivery);

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", "1",
            "slot-id", "2"
        ))
        .withBody("{\"userId\": 3}");

    APIGatewayProxyResponseEvent response = this.bookDelivery.handleRequest(request, Mockito.mock(Context.class));

    Delivery returnedDelivery = GSON.fromJson(response.getBody(), Delivery.class);

    assertEquals(200, response.getStatusCode());
    assertEquals(expectedDelivery, returnedDelivery);
  } 

  @Test
  public void validRequestWithNoAvailableDelivery() throws SQLException {
    String errMsg = "No delivery available in this slot";

    Mockito.when(this.slotService.bookDelivery(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenThrow(new IllegalStateException(errMsg));

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", "1",
            "slot-id", "2"
        ))
        .withBody("{\"userId\": 3}");

    APIGatewayProxyResponseEvent response = this.bookDelivery.handleRequest(request, Mockito.mock(Context.class));

    assertEquals(500, response.getStatusCode());
    assertEquals(HandlerErrorMessage.NO_AVAILABLE_DELIVERY.toString(), response.getBody());
  }

  /**
   * Should return 400 with correct error message when farm-id is invalid.
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "0.3", "shouldNotBeText"})
  public void invalidFarmId(String farmId) throws SQLException {

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", farmId,
            "slot-id", "2"
        ))
        .withBody("{\"userId\": 3}");

    APIGatewayProxyResponseEvent response = this.bookDelivery.handleRequest(request, Mockito.mock(Context.class));

    assertEquals(400, response.getStatusCode());
    assertEquals(HandlerErrorMessage.FARM_AND_SLOT_INVALID.toString(), response.getBody());
  }

  /**
   * Should return 400 with correct error message when slot-id is invalid.
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "0.3", "shouldNotBeText"})
  public void invalidSlotId(String slotId) throws SQLException {

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", "1",
            "slot-id", slotId
        ))
        .withBody("{\"userId\": 3}");

    APIGatewayProxyResponseEvent response = this.bookDelivery.handleRequest(request, Mockito.mock(Context.class));
    assertEquals(400, response.getStatusCode());
    assertEquals(HandlerErrorMessage.FARM_AND_SLOT_INVALID.toString(), response.getBody());
  }

  /**
   * Should return 400 with correct error message when user-id is invalid.
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "0.3", "shouldNotBeText"})
  public void invalidUserId(String userId) throws SQLException {

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", "1",
            "slot-id", "2"
        ))
        .withBody("{\"userId\": " + userId + "}");

    APIGatewayProxyResponseEvent response = this.bookDelivery.handleRequest(request, Mockito.mock(Context.class));
    assertEquals(400, response.getStatusCode());
    assertEquals(HandlerErrorMessage.USER_INVALID.toString(), response.getBody());
  }

  @Test
  public void noUserId() throws SQLException {

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", "1",
            "slot-id", "2"
        ))
        .withBody("{\"otherFieldThatIsNotUserId\": 3}");

    APIGatewayProxyResponseEvent response = this.bookDelivery.handleRequest(request, Mockito.mock(Context.class));
    assertEquals(400, response.getStatusCode());
    assertEquals(HandlerErrorMessage.USER_INVALID.toString(), response.getBody());
  }

  @Test
  public void nullPathParameters() throws SQLException {
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(null);

    assertThrows(RuntimeException.class, () -> this.bookDelivery.handleRequest(request, Mockito.mock(Context.class)));
  }

  @Test
  public void sqlThrowsException() throws SQLException {
    Mockito.when(this.slotService.bookDelivery(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenThrow(new SQLException());

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", "1",
            "slot-id", "2"
        ))
        .withBody("{\"userId\": 3}");

    APIGatewayProxyResponseEvent response = this.bookDelivery.handleRequest(request, Mockito.mock(Context.class));

    assertEquals(500, response.getStatusCode());
    assertEquals(HandlerErrorMessage.SQL_FAILED.toString(), response.getBody());
  }
}