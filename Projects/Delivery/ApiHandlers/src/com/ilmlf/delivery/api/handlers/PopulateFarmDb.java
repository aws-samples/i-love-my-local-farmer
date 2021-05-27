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
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import org.json.JSONObject;

/**
 * Seeds the database.
 */
public class PopulateFarmDb implements RequestHandler<CloudFormationCustomResourceEvent, String> {
  static String DB_ENDPOINT = System.getenv("DB_ENDPOINT");
  static String DB_REGION = System.getenv("DB_REGION");
  static JSONObject DB_ADMIN_SECRET = SecretsUtil.getSecret(DB_REGION, System.getenv("DB_ADMIN_SECRET"));
  static String DB_ADMIN_USER = (String) DB_ADMIN_SECRET.get("username");
  static String DB_ADMIN_PWD = (String) DB_ADMIN_SECRET.get("password");
  static JSONObject DB_USER_SECRET = SecretsUtil.getSecret(DB_REGION, System.getenv("DB_USER_SECRET"));
  static Connection con;

  static {
    try {
      con = DbUtil.createConnectionViaUserPwd(DB_ADMIN_USER, DB_ADMIN_PWD, DB_ENDPOINT);
    } catch (Exception e) {
      System.out.println("INIT connection FAILED");
      System.out.println(e.toString());
    }
  }

  @Override
  public String handleRequest(CloudFormationCustomResourceEvent event, Context context) {
    try {
      String script = "com/ilmlf/delivery/api/handlers/db/dbinit.sql";
      FileReader fr = new FileReader(script);
      BufferedReader br = new BufferedReader(fr);
      StringBuilder sb = new StringBuilder();
      String line;
      Statement stmt = con.createStatement();

      while ((line = br.readLine()) != null) {
        if (line.contains("{{password}}")) {
          // handle password line
          line = retrieveAndReplacePassword(line);
        }
        
        sb.append(line);
        if (line.indexOf(";") >= 0) {
          String query = sb.toString();
          try {
            stmt.execute(query);
          } catch(SQLSyntaxErrorException f) {
            System.out.println(f);
            // ignore, since the statements can run 
            // repeatedly and fail on already existing resources
          }
          sb = new StringBuilder(); // reinitialize, otherwise will keep appending
        }
      }
      stmt.close();
      br.close();

      return "Db Init script executed successfully";

    } catch (SQLException e) {
      e.printStackTrace();
      return "A SQL error occurred";
    } catch (Exception e) {
      e.printStackTrace();
      return "An error occurred";
    }
  }

  /**
   * Replaces {{password}} with the actual password retrieved from the Secret.
   * @param line the line to use
   * 
   * @return the line with the password 
   */
  private String retrieveAndReplacePassword(String line) {
    String dbUserPwd = (String) DB_USER_SECRET.get("password");
    line = line.replaceFirst("\\{\\{password\\}\\}", dbUserPwd);
    return line;
  }
}
