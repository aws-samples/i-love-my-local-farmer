package com.ilmlf.delivery.api.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.ilmlf.delivery.api.handlers.util.DbUtil;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
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
  public void generateAuthTokenGood() {
    String authToken = DbUtil.generateAuthToken("a", "b", "c", 1);
    assertTrue(null != authToken && !authToken.isEmpty());
  }
  
  @Test
  public void createCertificateGood() throws GeneralSecurityException, IOException {
    assertNotNull(DbUtil.createCertificate("resources/" + DbUtil.SSL_CERTIFICATE));
  }
  
  @Test
  public void createCertificateBad() {  
    assertThrows(IOException.class, () -> DbUtil.createCertificate("badFileName"));
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
