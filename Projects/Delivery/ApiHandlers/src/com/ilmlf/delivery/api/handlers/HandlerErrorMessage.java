package com.ilmlf.delivery.api.handlers;

public enum HandlerErrorMessage {
  FARM_AND_SLOT_INVALID(10001, "Farm id and Slot Id must not be blank, and must be a valid integer"),
  USER_INVALID(10002, "JSON in the body must be valid and userId field must be a valid integer "), 
  SQL_FAILED(10003, "Cannot record a new delivery in the database"),
  NO_AVAILABLE_DELIVERY(10004, "No available delivery left in this slot");

  private final int code;
  private final String description;

  private HandlerErrorMessage(int code, String description) {
    this.code = code;
    this.description = description;
  }

  @Override
  public String toString() {
    return code + ": " + description;
  }
}
