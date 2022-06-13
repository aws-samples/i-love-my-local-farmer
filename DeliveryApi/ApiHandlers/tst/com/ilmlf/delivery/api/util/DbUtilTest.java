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

package com.ilmlf.delivery.api.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.ilmlf.delivery.api.handlers.util.DbUtil;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for DBUtil class.
 * Includes checks that the SSL certificate can be used, 
 * and the auth token can be generated. *
 */
public class DbUtilTest {
  private Connection connectionMock;

  @Test
  @Disabled //Disabled as this requires local credential configuration and breaks inside a docker build
  public void generateAuthTokenGood() {
    String authToken = DbUtil.generateAuthToken("a", "b", "c", 1);
    assertTrue(null != authToken && !authToken.isEmpty());
  }
  
  @Test
  public void createCertificateGood() throws GeneralSecurityException, IOException {
    assertNotNull(DbUtil.createCertificate(DbUtil.SSL_CERTIFICATE));
  }
  
  @Test
  public void createCertificateBad() {  
    assertThrows(CertificateException.class, () -> DbUtil.createCertificate("badFileName"));
  }
  
  @Test
  public void createConnectionViaUserPwdGood() {
    Connection connectionMock = Mockito.mock(Connection.class);
    try (MockedStatic<DriverManager> driverMgr = Mockito.mockStatic(DriverManager.class)) {
      driverMgr.when(() -> DriverManager.getConnection(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
          .thenReturn(connectionMock);

      Connection conn = DbUtil.createConnectionViaUserPwd("a", "b", "c");
      assertNotNull(conn);
    }
  }
  
  @Test
  public void createConnectionViaUserPwdBad() {
    assertThrows(NullPointerException.class, () -> DbUtil.createConnectionViaUserPwd(null, "b", "c"));
    assertThrows(NullPointerException.class, () -> DbUtil.createConnectionViaUserPwd("a", null, "c"));
    assertThrows(NullPointerException.class, () -> DbUtil.createConnectionViaUserPwd("a", "b", null));
  }
  
  @Test
  public void createConnectionViaUserPwdSqlException() {
    try (MockedStatic<DriverManager> driverMgr = Mockito.mockStatic(DriverManager.class)) {
      driverMgr.when(() -> DriverManager.getConnection(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
          .thenThrow(SQLException.class);

      Connection conn = DbUtil.createConnectionViaUserPwd("a", "b", "c");
      assertNull(conn);
    }
  }
}
