package com.ilmlf.delivery.api.handlers.service;

import com.ilmlf.delivery.api.handlers.Delivery;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ilmlf.delivery.api.handlers.Slot;
import com.ilmlf.delivery.api.handlers.util.DbUtil;

public class SlotService {
  private Connection con;
  
  public SlotService() {
    String DB_ENDPOINT = System.getenv("DB_ENDPOINT");
    String DB_REGION = System.getenv("DB_REGION");
    String DB_USER = System.getenv("DB_USER");
    
    this.con = DbUtil.createConnectionViaIamAuth(DB_USER, DB_ENDPOINT, DB_REGION);
    System.out.println("SlotService Empty constructor, reading from env vars");
  }
  
  public SlotService(Connection con) {
    this.con = con;
    System.out.println("SlotService constructor, taking in mock Connection");
  }
  
  public int insertSlotList(List<Slot> slots) throws SQLException{
    Integer rowsUpdated = 0;
    try{
      this.con.setAutoCommit(false); // for transaction handling
      for (Slot slot : slots) {
        rowsUpdated += this.insertSlot(slot);
      }
      if (slots.size() == rowsUpdated) {
        this.con.commit();
      } else {
        this.con.rollback();
        rowsUpdated = 0;
      }
    } catch( SQLException e){
      this.con.rollback();
      rowsUpdated = 0;
    }

    return rowsUpdated;
  }
  
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
      LocalDate slotDate = ((Date) results.getObject("delivery_date")).toLocalDate();
      LocalDateTime slotFrom = ((Timestamp) results.getObject("slot_from")).toLocalDateTime();
      LocalDateTime slotTo = ((Timestamp) results.getObject("slot_to")).toLocalDateTime();
      Integer slotId = results.getInt("slot_id");
      Slot slot = Slot.builder()
          .deliveryDate(slotDate)
          .from(slotFrom)
          .to(slotTo)
          .slotId(slotId)
          .build();

      slotArray.add(slot);
    }

    return slotArray;
  }

  /**
   * Book a new delivery with the given parameters. 
   * Decrement number of available slot and insert a new record into the `delivery` table.
   *
   * @param farmId farm to delivery products
   * @param slotId time slot for delivery
   * @param userId user who books this delivery
   * @return Delivery object that contains the id of created delivery
   * @throws SQLException when update to the database fails
   * @throws IllegalStateException when there is no available delivery in the slot
   */
  public Delivery bookDelivery(Integer farmId, Integer slotId, Integer userId) throws SQLException {
    Delivery delivery;
    try {
      this.con.setAutoCommit(false);

      Boolean decreaseSucceeded = decreaseAvailableDeliveries(farmId, slotId);
      if (decreaseSucceeded) {
        delivery = insertNewDelivery(farmId, slotId, userId);
        this.con.commit();
        return delivery;
      } else {
        throw new IllegalStateException("No delivery available in this slot");
      }
    } catch (SQLException exception) {
      // If any update fails, we need to rollback to the original state.
      // Else, the data between `delivery_slot` and `delivery` tables will be left inconsistent
      this.con.rollback();
      throw exception;
    } finally {
      this.con.setAutoCommit(true);
    }
  }

  private boolean decreaseAvailableDeliveries(Integer farmId, Integer slotId) throws SQLException {
    String updateDeliverySlotQuery = "UPDATE deliverydb.delivery_slot "
        + "SET avail_deliveries = avail_deliveries - 1,  booked_deliveries =  booked_deliveries + 1 "
        + "WHERE avail_deliveries > 0 AND slot_id = ? AND farm_id = ?";
    PreparedStatement updateStmt = this.con.prepareStatement(updateDeliverySlotQuery);
    updateStmt.setInt(1, slotId);
    updateStmt.setInt(2, farmId);

    System.out.println("updateStmt: " + updateStmt.toString());
    return updateStmt.executeUpdate() == 1;
  }

  private Delivery insertNewDelivery(Integer farmId, Integer slotId, Integer userId) throws SQLException {
    String insertDeliveryQuery = "INSERT INTO deliverydb.delivery "
        + "(farm_id, slot_id, user_id) "
        + "values(?, ?, ?)";
    PreparedStatement insertStmt = this.con.prepareStatement(insertDeliveryQuery, Statement.RETURN_GENERATED_KEYS);
    insertStmt.setInt(1, farmId);
    insertStmt.setInt(2, slotId);
    insertStmt.setInt(3, userId);
    System.out.println("insertStmt: " + insertStmt.toString());
    insertStmt.executeUpdate();
    ResultSet rs = insertStmt.getGeneratedKeys();

    if (rs.next()) {
      Integer deliveryId = rs.getInt(1);
      return new Delivery(deliveryId);
    } else {
      System.out.println("No Result Set was returned!");
      throw new RuntimeException("Fail to insert a new record into delivery table");
    }
  }
}
