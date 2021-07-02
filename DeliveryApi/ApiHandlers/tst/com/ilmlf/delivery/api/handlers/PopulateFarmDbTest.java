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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

/**
 * Unit tests for PopulateFarmDb handler.
 * It injects a mocked Connection to the handler class and
 * checks that the handler returns correct responses for both success and failure scenarios
 */
public class PopulateFarmDbTest {
  private Connection connectionMock;
  private PopulateFarmDb populateFarmDb;
  
  @BeforeEach
  public void setUp() {
    this.connectionMock = Mockito.mock(Connection.class);
    this.populateFarmDb = new PopulateFarmDb(connectionMock);
  }

  @Test
  public void replaceCredsInLineGood() {
    assertEquals("a", this.populateFarmDb.replaceCredentials("{{username}}", "a", "b"));
    assertEquals("b", this.populateFarmDb.replaceCredentials("{{password}}", "a", "b"));
    assertEquals("ab", this.populateFarmDb.replaceCredentials("{{username}}{{password}}", "a", "b"));
    assertEquals("xxxayyybzzz", 
        this.populateFarmDb.replaceCredentials("xxx{{username}}yyy{{password}}zzz", "a", "b"));
    assertEquals("xxxayyybzzzyyybxxxa", this.populateFarmDb
        .replaceCredentials("xxx{{username}}yyy{{password}}zzzyyy{{password}}xxx{{username}}", "a", "b"));
  }
  
  /**
   * Test that bad keywords don't cause a replacement.
   */
  @ParameterizedTest
  @ValueSource(strings = {"xxxusernameyyypasswordzzz", "{username}{password}",
      "{{Username}}{{Password}}", "xyz", ""})
  public void replaceCredsInLineBad(String noReplacementStr) {
    assertEquals(noReplacementStr, this.populateFarmDb.replaceCredentials(noReplacementStr, "a", "b"));
    assertThrows(NullPointerException.class, () -> 
        this.populateFarmDb.replaceCredentials(null, "a", "b"));
    assertThrows(NullPointerException.class, () -> 
        this.populateFarmDb.replaceCredentials(noReplacementStr, null, "b"));
    assertThrows(NullPointerException.class, () -> 
        this.populateFarmDb.replaceCredentials(noReplacementStr, "a", null));
  }
  
  @Test
  public void replaceCredsInArrayGood() {
    List<String> stmts = this.populateFarmDb.replaceCredentialsArray(new ArrayList<String>(), "a", "b");
    assertTrue(stmts.size() == 0);
    
    stmts = this.populateFarmDb.replaceCredentialsArray(Arrays.asList("{{username}}"), "a", "b");
    assertTrue(stmts.size() == 1 && "a".equals(stmts.get(0)));
  }
  
  @Test
  public void replaceCredsInArrayBad() {
    assertThrows(NullPointerException.class, () -> 
        this.populateFarmDb.replaceCredentialsArray(null, "a", "b"));
    assertThrows(NullPointerException.class, () -> 
        this.populateFarmDb.replaceCredentialsArray(new ArrayList<String>(), null, "b"));
    assertThrows(NullPointerException.class, () -> 
        this.populateFarmDb.replaceCredentialsArray(new ArrayList<String>(), "a", null));
  }

  @Test
  public void isExecuteScriptTrue() {
    assertTrue(this.populateFarmDb.isExecuteScript(PopulateFarmDb.REQUEST_CREATE));
    assertTrue(this.populateFarmDb.isExecuteScript(PopulateFarmDb.REQUEST_UPDATE));
    assertTrue(this.populateFarmDb.isExecuteScript(null));
  }
  
  @Test
  public void isExecuteScriptFalse() {
    assertFalse(this.populateFarmDb.isExecuteScript(PopulateFarmDb.REQUEST_DELETE));
  }
  
  @Test
  public void buildReturnJsonGood() {
    assertTrue(helperCompareJsonElement(this.populateFarmDb.buildReturnJson(null, null, true), 
        PopulateFarmDb.SCRIPT_RUN, "true"));
    
    assertTrue(helperCompareJsonElement(this.populateFarmDb.buildReturnJson(PopulateFarmDb.REQUEST_UPDATE, "1", true), 
        PopulateFarmDb.PHYS_RESOURCE_ID, "1"));
    
    assertTrue(helperCompareJsonElement(this.populateFarmDb.buildReturnJson(PopulateFarmDb.REQUEST_DELETE, "1", true), 
        PopulateFarmDb.PHYS_RESOURCE_ID, "1"));
    
    assertFalse(this.populateFarmDb.buildReturnJson("notupdatenordelete", "1", true)
        .contains(PopulateFarmDb.PHYS_RESOURCE_ID));
  }
  
  /**
   * Check that the Json string includes the key and value.
   *
   * @param json is the JSON string to check
   * @param key to check for
   * @param value to check for
   * @return whether the key/value was found
   */
  public boolean helperCompareJsonElement(String json, String key, String value) {
    JSONObject jsonObj = new JSONObject(json);
    return jsonObj.has(key) && value.equals(jsonObj.get(key));
  }
  
  @Test
  public void extractSqlStatementsGood() throws IOException {
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    
    Mockito.when(bufferedReader.readLine()).thenReturn("a", "b;", null);
    List<String> stmts = this.populateFarmDb.extractSqlStatements(bufferedReader);
    assertTrue(stmts.size() == 1);
    assertTrue("ab;".equals(stmts.get(0)));
    
    Mockito.when(bufferedReader.readLine()).thenReturn("a;", "b;", null);
    stmts = this.populateFarmDb.extractSqlStatements(bufferedReader);
    assertTrue(stmts.size() == 2);
    assertTrue("a;".equals(stmts.get(0)) && "b;".equals(stmts.get(1)));
    
    Mockito.when(bufferedReader.readLine()).thenReturn("a;", "", "b;", null);
    stmts = this.populateFarmDb.extractSqlStatements(bufferedReader);
    assertTrue(stmts.size() == 2);
    assertTrue("a;".equals(stmts.get(0)) && "b;".equals(stmts.get(1)));
  }
  
  @Test
  public void extractSqlStatementsBad() throws IOException {
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    
    Mockito.when(bufferedReader.readLine()).thenReturn("a", "b", null);
    List<String> stmts = this.populateFarmDb.extractSqlStatements(bufferedReader);
    assertTrue(stmts.size() == 0);
  }
}
