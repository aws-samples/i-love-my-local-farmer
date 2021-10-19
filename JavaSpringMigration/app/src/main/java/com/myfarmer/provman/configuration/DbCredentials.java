package com.myfarmer.provman.configuration;

import lombok.Data;

/* Class representing the contents of an RDS credentials secret.
 * Used to test secret retrieval from Secrets Manager, with the /env endpoint.
 */
@Data
public class DbCredentials {
  private String password;
  private String dbname;
  private String engine;
  private String port;
  private String dbInstanceIdentifier;
  private String host;
  private String username;

}
