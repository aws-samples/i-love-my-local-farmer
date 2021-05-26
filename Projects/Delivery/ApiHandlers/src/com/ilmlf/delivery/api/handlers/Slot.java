package com.ilmlf.delivery.api.handlers;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
public class Slot {
  private Integer slotId;
  private LocalDate deliveryDate;
  private LocalDateTime from;
  private LocalDateTime to;
  private Integer availDeliveries;
  private Integer bookedDeliveries;
  private Integer farmId;

  public static LocalDateTime getLocalDateTimeFromIso(String dateTimeString) {
    return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }

}
