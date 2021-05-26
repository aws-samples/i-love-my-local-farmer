package com.ilmlf.delivery.api.handlers.db;

import com.ilmlf.delivery.api.handlers.Slot;
import com.ilmlf.delivery.api.handlers.util.SecretsUtil;
import lombok.Data;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Data
public class DbOperations {
  private String DB_ENDPOINT = System.getenv("DB_ENDPOINT");
  private String DB_REGION = System.getenv("DB_REGION");
  private JSONObject DB_JSON_SECRET = SecretsUtil.getSecret(DB_REGION, System.getenv("DB_SECRET_NAME"));
  private String USERNAME = (String) DB_JSON_SECRET.get("username");
  private String PWD = (String) DB_JSON_SECRET.get("password");
  private Connection con;

  /**
   * Insert a slot into the database
   * @param slot
   * @return number of rows inserted (1 = success, 0 = failure)
   * @throws SQLException
   */
  public int insertSlot(Slot slot) throws SQLException {
    String query = "Insert into deliverydb.delivery_slot "
      + " (delivery_date, slot_from, slot_to, avail_deliveries, booked_deliveries, farm_id)"
      + " values(?,?,?,?,?,?)";

    PreparedStatement prepStmt = con.prepareStatement(query);
    prepStmt.setObject(1, slot.getDeliveryDate());
    prepStmt.setObject(2, slot.getFrom());
    prepStmt.setObject(3, slot.getTo());
    prepStmt.setInt(4, slot.getAvailDeliveries());
    prepStmt.setInt(5, slot.getBookedDeliveries());
    prepStmt.setInt(6, slot.getFarmId());

    System.out.println("prepStmt: "+ prepStmt.toString());  // prints bind variables passed in

    int rowsUpdated = prepStmt.executeUpdate();
    prepStmt.close();
    return rowsUpdated;
  }

  public ArrayList<Slot> getSlots(Integer farmId, LocalDate availableSlotsBeginDate,
                                  LocalDate availableSlotsEndDate) throws SQLException {
    ArrayList<Slot> slotArray = new ArrayList<>();
    String query = "select * from deliverydb.delivery_slot "
        + "where farm_id = ?"
        + " and delivery_date >= ?"
        + " and delivery_date <= ?"
        + " and avail_deliveries > 0";

    PreparedStatement preparedStatement = con.prepareStatement(query);
    preparedStatement.setInt(1, farmId);
    preparedStatement.setObject(2, availableSlotsBeginDate);
    preparedStatement.setObject(3, availableSlotsEndDate);
    System.out.println("prepStmt: "+ preparedStatement.toString());

    ResultSet results = preparedStatement.executeQuery();

    while (results.next()) {
      LocalDate slotDate = (LocalDate) results.getObject("delivery_date");
      LocalDateTime slotFrom = (LocalDateTime) results.getObject("slot_from");
      LocalDateTime slotTo = (LocalDateTime) results.getObject("slot_to");
      Integer slotId = results.getInt("slot_id");
      Slot slot = Slot.builder()
          .deliveryDate(slotDate)
          .from(slotFrom)
          .to(slotTo)
          .slotId(slotId)
          .build();

      System.out.println(slot.toString());

      slotArray.add(slot);
    }

    return slotArray;
  }
}
