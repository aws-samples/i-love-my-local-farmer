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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.Data;

/**
 * An object that represents a time slot for delivery.
 * The slot indicates a period where we could deliver to a customer's home.
 * Each slot has a limited number of available deliveries. ( field `availDeliveries`)
 * Once this number reaches 0, customers cannot book a delivery in this time slot anymore.
 */
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
