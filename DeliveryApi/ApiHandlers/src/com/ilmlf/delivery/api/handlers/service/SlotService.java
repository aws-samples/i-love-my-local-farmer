package com.ilmlf.delivery.api.handlers.service;

import com.ilmlf.delivery.api.handlers.Delivery;
import com.ilmlf.delivery.api.handlers.Slot;
import com.ilmlf.delivery.api.handlers.util.DbUtil;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.tracing.TracingUtils;

/**
 * Provides methods to interact with Slots in the data layer.
 */
@Data
public class SlotService {
  private static final Logger logger = LogManager.getLogger(SlotService.class);

  private static final String DB_ENDPOINT = System.getenv("DB_ENDPOINT");
  private static final Integer DB_PORT;
  private static final String DB_REGION = System.getenv("DB_REGION");
  private static final String DB_USER = System.getenv("DB_USER");
  private static final long BACKOFF_TIME_MILLI = 1000; // One second

  static {
    DB_PORT = retrievePort("DB_PORT", 3306);
  }

  private Connection con;
  private DbUtil dbUtil;

  /**
   * Constructor used in actual environment (inside Lambda handler).
   */
  public SlotService() {
    this(new DbUtil());
  }

  /**
   * Constructor that takes a mocked DbUtil for testing purposes.
   *
   * @param dbUtil Injected DbUtil
   */
  SlotService(DbUtil dbUtil) {
    this.dbUtil = dbUtil;
    this.con = this.dbUtil.createConnectionViaIamAuth(DB_USER, DB_ENDPOINT, DB_REGION, DB_PORT);
  }

  /**
   * Inserts multiple slots into the database.
   * This is an all or nothing operation. If any of the slot insertions fail,
   * it will rollback all previously inserted slots.
   *
   * @param slots List of slots to be inserted.
   * @throws SQLException when SQL execution fails
   */
  @Tracing(segmentName = "Insert_Slot_Transac")
  public int insertSlotList(List<Slot> slots) throws SQLException {
    this.con = refreshDbConnection();
    int rowsUpdated = 0;

    try {
      this.con.setAutoCommit(false); // for transaction handling

      for (Slot slot : slots) {
        rowsUpdated += this.insertSlot(slot);
      }

      if (slots.size() == rowsUpdated) {
        this.con.commit();
      } else {
        logger.error("Rolling back transaction!");
        this.con.rollback();
        rowsUpdated = 0;
      }

    } catch (SQLException e) {
      logger.error(e.getMessage() + ", rolling back transaction!", e);
      this.con.rollback();
      rowsUpdated = 0;
    }

    return rowsUpdated;
  }

  /**
   * Inserts a slot into the database.
   *
   * @param slot Slot to be inserted
   * @return number of rows inserted (1 = success, 0 = failure)
   * @throws SQLException when SQL execution fails
   */
  @Tracing(segmentName = "Insert_Slot_RDS")
  public int insertSlot(Slot slot) throws SQLException {
    TracingUtils.putAnnotation("farm", slot.getFarmId());
    logger.info("Inserting slot: {}", slot);

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

    logger.debug("prepStmt: {}", prepStmt.toString());  // prints bind variables passed in

    int rowsUpdated = prepStmt.executeUpdate();

    prepStmt.close();
    return rowsUpdated;
  }

  /**
   * Gets Slots from given farm id within the given time range.
   *
   * @param farmId Farm to retrieve slots
   * @param availableSlotsBeginDate Begin date
   * @param availableSlotsEndDate End date
   * @return an ArrayList of Slot objects
   * @throws SQLException if an error occurs during preparing the statement
   */
  @Tracing(segmentName = "Get_Slot_RDS")
  public ArrayList<Slot> getSlots(Integer farmId, LocalDate availableSlotsBeginDate,
                                  LocalDate availableSlotsEndDate) throws SQLException {
    TracingUtils.putAnnotation("farm", farmId);
    this.con = refreshDbConnection();
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
    logger.debug("prepStmt: {}", preparedStatement.toString());

    ResultSet results = preparedStatement.executeQuery();

    while (results.next()) {
      LocalDate slotDate = ((Date) results.getObject("delivery_date")).toLocalDate();
      LocalDateTime slotFrom = (LocalDateTime) results.getObject("slot_from");
      LocalDateTime slotTo = (LocalDateTime) results.getObject("slot_to");
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
   * Books a new delivery with the given parameters.
   * Decrements number of available slots and inserts a new record into the `delivery` table.
   *
   * @param farmId farm from which products will be delivered
   * @param slotId time slot for the delivery
   * @param userId user who booked this delivery
   * @return Delivery object that contains the id of the created delivery
   * @throws SQLException when update to the database fails
   * @throws IllegalStateException when there is no available delivery in the slot
   */
  @Tracing(segmentName = "Book_Delivery_Transac")
  public Delivery bookDelivery(Integer farmId, Integer slotId, Integer userId) throws SQLException {
    TracingUtils.putAnnotation("farm", farmId);
    logger.info("Booking slot {} for user {} on farm {}", slotId, userId, farmId);

    this.con = refreshDbConnection();
    Delivery delivery;

    try {
      this.con.setAutoCommit(false);

      boolean decreaseSucceeded = decreaseAvailableDeliveries(farmId, slotId);
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
      logger.error(exception.getMessage()+ ", rolling back transaction!", exception);
      this.con.rollback();
      throw exception;

    } finally {
      this.con.setAutoCommit(true);
    }
  }

  @Tracing(segmentName = "Update_Slot_RDS")
  private boolean decreaseAvailableDeliveries(Integer farmId, Integer slotId) throws SQLException {
    TracingUtils.putAnnotation("farm", farmId);
    this.con = refreshDbConnection();
    String updateDeliverySlotQuery = "UPDATE deliverydb.delivery_slot "
        + "SET avail_deliveries = avail_deliveries - 1,  booked_deliveries =  booked_deliveries + 1 "
        + "WHERE avail_deliveries > 0 AND slot_id = ? AND farm_id = ?";
    PreparedStatement updateStmt = this.con.prepareStatement(updateDeliverySlotQuery);
    updateStmt.setInt(1, slotId);
    updateStmt.setInt(2, farmId);

    logger.debug("updateStmt: {}", updateStmt.toString());
    return updateStmt.executeUpdate() == 1;
  }

  @Tracing(segmentName = "Book_Delivery_RDS")
  private Delivery insertNewDelivery(Integer farmId, Integer slotId, Integer userId) throws SQLException {
    TracingUtils.putAnnotation("farm", farmId);
    logger.info("Inserting delivery for slot {}, user {} on farm {}", slotId, userId, farmId);

    String insertDeliveryQuery = "INSERT INTO deliverydb.delivery "
        + "(farm_id, slot_id, user_id) "
        + "values(?, ?, ?)";
    PreparedStatement insertStmt = this.con.prepareStatement(insertDeliveryQuery, Statement.RETURN_GENERATED_KEYS);
    insertStmt.setInt(1, farmId);
    insertStmt.setInt(2, slotId);
    insertStmt.setInt(3, userId);
    logger.debug("insertStmt: {}", insertStmt.toString());

    insertStmt.executeUpdate();
    ResultSet rs = insertStmt.getGeneratedKeys();

    if (rs.next()) {
      Integer deliveryId = rs.getInt(1);
      return new Delivery(deliveryId);
    } else {
      logger.warn("No Result Set was returned!");
      throw new RuntimeException("Fail to insert a new record into delivery table");
    }
  }

  /**
   * Refreshes the database connection in case there is a warm Lambda that has a connection that has either closed or
   * failed to connect.
   *
   * @return the existing Connection or a new one in the case it needs to be refreshed
   */
  protected Connection refreshDbConnection() {
    Connection connection = this.con;

    try {
      if (connection == null || !connection.isValid(1)) {
        logger.info("Retrying database connection");
        try {
          Thread.sleep(BACKOFF_TIME_MILLI);
          connection = this.dbUtil.createConnectionViaIamAuth(DB_USER, DB_ENDPOINT, DB_REGION, DB_PORT);
        } catch (InterruptedException e) {
          logger.error(e.getMessage(), e);
          throw new RuntimeException("There was a problem sleeping the thread while creating a connection to the DB");
        }
      }

    } catch (SQLException e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException("There was a problem refreshing the database connection "
        + "due to an error while checking validity");
    }

    return connection;
  }

  private static Integer retrievePort(String envVarName, Integer defaultPort) {
    Integer port = defaultPort;
    try {
      port = Integer.valueOf(System.getenv(envVarName));
    } catch (NumberFormatException nfe) {
      logger.warn("DB_PORT is not in environment variables or not an integer");
    }
    return port;
  }
}
