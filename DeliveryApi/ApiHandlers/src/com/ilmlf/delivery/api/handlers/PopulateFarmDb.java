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
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.ilmlf.delivery.api.handlers.util.DbUtil;
import com.ilmlf.delivery.api.handlers.util.SecretsUtil;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 * Create required tables and a DB user for Delivery application.
 *
 * <p>
 * CloudFormation Custom Resource will call Lambda function with this code when
 * the stack is created. This handler will run `dbinit.sql`.
 * </p>
 *
 * <p>
 * Apart from creating tables, it will create a user in the DB with the credential from
 * DB_USER_SECRET. API Lambda functions and RDS proxy use this credential to connect to DB.
 * </p>
 *
 * <p>
 * The Lambda functions will only know the username. They have a Role which allows them
 * to connect to RDS proxy via this user. (e.g. Action: "rds-db:connect", Resources:
 * "arn:aws:rds-db:REGION:ACCOUNT_ID:dbuser:*\/DB_USERNAME")
 * </p>
 *
 * <p>
 * RDS proxy will retrieve the password from DB_USER_SECRET and handle the connection to
 * the DB instance.
 * </p>
 */
public class PopulateFarmDb implements RequestHandler<Object, String> {
  /**
   * Database connection info.
   */
  static String DB_ENDPOINT = System.getenv("DB_ENDPOINT");
  static String DB_REGION = System.getenv("DB_REGION");

  /**
   * Admin credentials for RDS instance.
   */
  static final JSONObject DB_ADMIN_SECRET = SecretsUtil.getSecret(DB_REGION, System.getenv("DB_ADMIN_SECRET"));
  static final String DB_ADMIN_USER = (String) DB_ADMIN_SECRET.get("username");
  static final String DB_ADMIN_PWD = (String) DB_ADMIN_SECRET.get("password");

  /**
   * User credentials for IAM Authorization via RDS Proxy.
   */
  static final JSONObject DB_USER_SECRET = SecretsUtil.getSecret(DB_REGION, System.getenv("DB_USER_SECRET"));
  static final String DB_USERNAME = (String) DB_USER_SECRET.get("username");
  static final String DB_PASSWORD = (String) DB_USER_SECRET.get("password");

  static Connection con = null;
  private static final Logger logger = LogManager.getLogger(CreateSlots.class);

  static {
    try {
      con = DbUtil.createConnectionViaUserPwd(DB_ADMIN_USER, DB_ADMIN_PWD, DB_ENDPOINT);

    } catch (Exception e) {
      logger.info("INIT connection FAILED");
      logger.error(e.getMessage(), e);
    }
  }

  /**
   * Entry point for Custom Resource call. This function executes dbinit.sql and
   * creates a user with the username and password from DB_SECRET.
   *
   * @param event Event object from CloudFormation Custom Resource
   * @param context Context object
   * @return Result to Custom Resource
   */
  @Override
  public String handleRequest(Object event, Context context) {
    String script = "./com/ilmlf/db/dbinit.sql";
    try (FileReader fr = new FileReader(script);
         BufferedReader br = new BufferedReader(fr);
         Statement stmt = con.createStatement()){

      StringBuilder sb = new StringBuilder();
      String line;

      while ((line = br.readLine()) != null) {
        if (line.contains("{{username}}") || line.contains("{{password}}")) {
          line = replaceUserDbProxyCredentials(line);
        }

        sb.append(line);
        if (line.contains(";")) {
          String query = sb.toString();
          stmt.execute(query);
          sb = new StringBuilder(); // reinitialize, otherwise will keep appending
        }
      }

      return "Db Init script executed successfully";

      // Rethrow the checked exception as a runtime exception as
      // the overridden method signature doesn't throw any exceptions
    } catch (SQLException e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Replaces {{username}} and {{password}} with the actual values retrieved from the Secret.
   *
   * @param line line to be replaced
   * @return the line with the credentials replaced
   */
  private String replaceUserDbProxyCredentials(String line) {

    String l =  line.replaceAll("\\{\\{username\\}\\}", DB_USERNAME)
        .replaceAll("\\{\\{password\\}\\}", DB_PASSWORD);
    return l;
  }
}
