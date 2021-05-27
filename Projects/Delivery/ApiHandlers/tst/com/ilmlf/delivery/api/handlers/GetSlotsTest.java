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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.ilmlf.delivery.api.handlers.GetSlots;
import com.ilmlf.delivery.api.handlers.Slot;
import com.ilmlf.delivery.api.handlers.service.SlotService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GetSlotsTest {
  private GetSlots getSlots;
  private SlotService slotService;
  private static Gson GSON;

  @BeforeAll
  public static void createGson() {
    GSON = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class,
            (JsonDeserializer<LocalDate>) (json, type, jsonDeserializationContext) -> LocalDate.parse(json.getAsString()))
        .registerTypeAdapter(LocalDateTime.class,
            (JsonDeserializer<LocalDateTime>) (json, type, jsonDeserializationContext) -> LocalDateTime.parse(json.getAsString()))
        .create();
  }

  @BeforeEach
  public void setup() {
    this.slotService = Mockito.mock(SlotService.class);
    this.getSlots = new GetSlots(this.slotService);
  }

  @Test
  public void validRequestWithSlots() throws SQLException {
    Slot testSlot = Slot.builder()
        .slotId(1)
        .from(LocalDateTime.now())
        .to(LocalDateTime.now())
        .deliveryDate(LocalDate.now())
        .build();

    Mockito.when(this.slotService.getSlots(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new ArrayList<>(List.of(testSlot)));

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", "1"
        ));

    APIGatewayProxyResponseEvent response = this.getSlots.handleRequest(request, Mockito.mock(Context.class));

    JsonArray slots = GSON.fromJson(response.getBody(), JsonArray.class);
    Slot returnedSlot = GSON.fromJson(slots.get(0), Slot.class);

    assertEquals(200, response.getStatusCode());
    assertEquals(testSlot, returnedSlot);
  }

  @Test
  public void emptyFarmId() throws SQLException {
    Slot testSlot = Slot.builder()
        .slotId(1)
        .from(LocalDateTime.now())
        .to(LocalDateTime.now())
        .deliveryDate(LocalDate.now())
        .build();

    Mockito.when(this.slotService.getSlots(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new ArrayList<>(List.of(testSlot)));

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", ""
        ));

    assertThrows(RuntimeException.class, () -> this.getSlots.handleRequest(request, Mockito.mock(Context.class)));
  }

  @Test
  public void nullPathParameters() throws SQLException {
    Slot testSlot = Slot.builder()
        .slotId(1)
        .from(LocalDateTime.now())
        .to(LocalDateTime.now())
        .deliveryDate(LocalDate.now())
        .build();

    Mockito.when(this.slotService.getSlots(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new ArrayList<>(List.of(testSlot)));

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(null);

    assertThrows(RuntimeException.class, () -> this.getSlots.handleRequest(request, Mockito.mock(Context.class)));
  }

  @Test
  public void invalidFarmId() throws SQLException {
    Slot testSlot = Slot.builder()
        .slotId(1)
        .from(LocalDateTime.now())
        .to(LocalDateTime.now())
        .deliveryDate(LocalDate.now())
        .build();

    Mockito.when(this.slotService.getSlots(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new ArrayList<>(List.of(testSlot)));

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", "asdf"
        ));

    assertThrows(RuntimeException.class, () -> this.getSlots.handleRequest(request, Mockito.mock(Context.class)));
  }

  @Test
  public void noSlotsFound() throws SQLException {
    Mockito.when(this.slotService.getSlots(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new ArrayList<>());

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", "1"
        ));

    APIGatewayProxyResponseEvent response = this.getSlots.handleRequest(request, Mockito.mock(Context.class));

    assertEquals(400, response.getStatusCode());
  }

  @Test
  public void sqlThrowsException() throws SQLException {
    Mockito.when(this.slotService.getSlots(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenThrow(new SQLException());

    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
        .withPathParameters(Map.of(
            "farm-id", "1"
        ));

    APIGatewayProxyResponseEvent response = this.getSlots.handleRequest(request, Mockito.mock(Context.class));

    assertEquals(500, response.getStatusCode());
  }
}
