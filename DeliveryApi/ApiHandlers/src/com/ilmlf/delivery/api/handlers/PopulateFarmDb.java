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
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.ilmlf.delivery.api.handlers.util.DbUtil;
import com.ilmlf.delivery.api.handlers.util.SecretsUtil;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 * Create required tables and a DB user for Delivery application.
 *
 * <p>
 * CloudFormation Custom Resource will call Lambda function with this code when
 * the stack is created. This handler will run `dbinit.sql`. The code will login
 * with admin user/pwd directly on the RDS MySql instance.
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
public class PopulateFarmDb implements RequestHandler<CloudFormationCustomResourceEvent, String> {
  /**
   * Database connection info.
   */
  static final String DB_ENDPOINT = System.getenv("DB_ENDPOINT");
  static final String DB_REGION = System.getenv("DB_REGION");

  /** SQL Script file.  */
  static final String SCRIPT_FILE = "./com/ilmlf/db/dbinit.sql";

  /**
   * Props to return from the handler (some for managing Custom resource).
   */
  static final String PHYS_RESOURCE_ID = "PhysicalResourceId";
  static final String REQUEST_UPDATE = "Update";
  static final String REQUEST_DELETE = "Delete";
  static final String REQUEST_CREATE = "Create";
  static final String SCRIPT_RUN = "scriptRun";

  private Connection con = null;
  private String dbRdsProxyUser;
  private String dbRdsProxyUserPwd;

  private static final Logger logger = LogManager.getLogger(PopulateFarmDb.class);

  /**
   * Constructor called by AWS Lambda.
   */
  public PopulateFarmDb() {
    // Admin credentials for RDS instance.
    JSONObject dbAdminSecret = SecretsUtil.getSecret(DB_REGION, System.getenv("DB_ADMIN_SECRET"));
    String dbAdminUser = (String) dbAdminSecret.get("username");
    String dbAdminPwd = (String) dbAdminSecret.get("password");
    
    JSONObject dbUserSecret = SecretsUtil.getSecret(DB_REGION, System.getenv("DB_USER_SECRET"));
    this.dbRdsProxyUser = (String) dbUserSecret.get("username");
    this.dbRdsProxyUserPwd = (String) dbUserSecret.get("password");

    try {
      this.con = DbUtil.createConnectionViaUserPwd(dbAdminUser, dbAdminPwd, DB_ENDPOINT);

    } catch (Exception e) {
      logger.info("INIT connection FAILED");
      logger.error(e.getMessage(), e);
    }

    logger.info("PopulateFarmDb empty constructor, called by AWS Lambda");
  }

  /**
   * Constructor for unit testing. Allow test code to inject mocked Connection.
   *
   * @param connection the mocked Connection object
   */
  public PopulateFarmDb(Connection connection) {
    this.con = connection;
    logger.info("PopulateFarmDb constructor for unit testing, allowing injection of mock Connection");
  }
  
  
  /**
   * Entry point for Custom Resource call. This function executes dbinit.sql, which 
   * creates a db user with username and password from DB_SECRET.
   * The Sql script will only execute on CREATE/UPDATE requestTypes 
   * See https://docs.aws.amazon.com/cdk/api/latest/java/software/amazon/awscdk/customresources/package-summary.html 
   *
   * @param customResourceEvent Event object from CloudFormation Custom Resource
   * @param context Context object
   * @return Result to Custom Resource
   */
  public String handleRequest(CloudFormationCustomResourceEvent customResourceEvent, Context context) {
    boolean runScript = this.isExecuteScript(customResourceEvent.getRequestType());
    
    if (runScript) {
      logger.info("Running SQL script");
      
      List<String> stmts = extractSqlStatementsFromFile(SCRIPT_FILE);
      stmts = replaceCredentialsArray(stmts, this.dbRdsProxyUser, this.dbRdsProxyUserPwd);
      executeSqlStatements(stmts);
    }

    String returnJson = 
        this.buildReturnJson(customResourceEvent.getRequestType(), 
            customResourceEvent.getPhysicalResourceId(), runScript);
    return returnJson;
  }

  /**
   * Extracts the list of SQL statements from a file.
   *
   * @param fileName of the SQL file
   */
  public List<String> extractSqlStatementsFromFile(String fileName) {
    try (FileReader fr = new FileReader(fileName);
        BufferedReader br = new BufferedReader(fr)) {

      return this.extractSqlStatements(br);
      
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Extracts the SQL statements from the BufferedReader into an array list.
   *
   * @param br the Buffered Reader
   * @return the list of SQL statements as an ArrayList String
   * @throws IOException on reading error
   */
  public List<String> extractSqlStatements(BufferedReader br) throws IOException {
    List<String> sqlStmts = new ArrayList<String>();
    StringBuilder sb = new StringBuilder();
    String line;

    while ((line = br.readLine()) != null) {
      sb.append(line);
      if (line.contains(";")) {
        String query = sb.toString();
        sqlStmts.add(query);
        sb = new StringBuilder(); // reinitialize, otherwise will keep appending
      }
    }
    return sqlStmts;
  }
  
  /**
   * Executes the list of SQL statements.
   *
   * @param sqlStmts is the list of sql statements to be executed
   */
  public void executeSqlStatements(List<String> sqlStmts) {
    try (Statement stmt = con.createStatement()) {
      for (String sqlStmt : sqlStmts) {
        stmt.execute(sqlStmt);
      }
    } catch (SQLException e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    } 
  }
  
  /**
   * Should the SQL script be executed or not. 
   *
   * @param requestType from the Custom resource
   * @return true if the request type is Update or Create
   */
  public boolean isExecuteScript(String requestType) {
    if (null != requestType) {
      if (REQUEST_UPDATE.equals(requestType) || REQUEST_CREATE.equals(requestType)) {
        return true;
      }
    } else {
      return true;
    }
    return false;
  }
  
  /**
   * Build the JSON string to return, which will hold <br/>
   * the request type, the physical resource ID (on RequestType=Update/Delete) <br/>
   * and whether the sql script was executed.
   *
   * @param requestType Custom resource request type
   * @param physicalResourceId from custom resource
   * @param runScript whether the script was run
   * @return the JSON string
   */
  public String buildReturnJson(String requestType, String physicalResourceId, boolean runScript) {
    JSONObject retJson = new JSONObject();
    if (requestType != null) {
      retJson.put("RequestType", requestType);
    }

    if (REQUEST_UPDATE.equals(requestType) || REQUEST_DELETE.equals(requestType)) {
      // Updates and Deletes need to return the same Physical Id they had
      retJson.put(PHYS_RESOURCE_ID, physicalResourceId);
    }

    retJson.put(SCRIPT_RUN, Boolean.toString(runScript));
    System.out.println(retJson.toString());
    return retJson.toString();
  }

  /**
   * Replaces the username and password credentials in the list of SQL statements.
   *
   * @param sqlStmts the statements to inspect
   * @param dbUsername the username to replace with
   * @param dbPassword the password to replace with
   * @return the list of sql statements with credentials replaced
   */
  public List<String> replaceCredentialsArray(@NonNull List<String> sqlStmts, 
        @NonNull String dbUsername, @NonNull String dbPassword) {
    List<String> replacedCredsList = new ArrayList<String>();
    for (String sqlStmt : sqlStmts) {
      replacedCredsList.add(replaceCredentials(sqlStmt, dbUsername, dbPassword));
    }
    
    return replacedCredsList;
  }
  
  /**
   * Replaces {{username}} and {{password}} with the actual values retrieved from the Secret.
   *
   * @param line line to be replaced
   * @return the line with the credentials replaced
   */
  public String replaceCredentials(@NonNull String line, @NonNull String dbUsername, @NonNull String dbPassword) {
    String l =  line.replaceAll("\\{\\{username\\}\\}", dbUsername)
        .replaceAll("\\{\\{password\\}\\}", dbPassword);
    return l;
  }
}
