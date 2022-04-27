package com.ilmlf.delivery.api.handlers.util;

import com.ilmlf.delivery.api.handlers.Slot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.json.JSONArray;
import org.json.JSONObject;

public class SlotParser {

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
   *
   * @param slotJson the Json format slot data
   * @param farmIdStr the farmId as a String
   *
   * @return the Slot object
   */
  public static Slot parseAndCreateSlot(JSONObject slotJson, String farmIdStr) {
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
