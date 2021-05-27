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
