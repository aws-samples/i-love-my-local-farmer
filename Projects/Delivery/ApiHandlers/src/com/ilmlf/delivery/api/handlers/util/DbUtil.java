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

package com.ilmlf.delivery.api.handlers.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import lombok.NonNull;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

public abstract class DbUtil {
  private static final String SSL_CERTIFICATE = "rds-ca-2019-root.pem";
  private static final String KEY_STORE_TYPE = "JKS";
  private static final String KEY_STORE_PROVIDER = "SUN";
  private static final String KEY_STORE_FILE_PREFIX = "sys-connect-via-ssl-test-cacerts";
  private static final String KEY_STORE_FILE_SUFFIX = ".jks";
  private static final String DEFAULT_KEY_STORE_PASSWORD = "delivery";
  private static final String JDBC_PREFIX = "jdbc:mysql://";

  /**
   * Create a database connection via a IAM Authentication. <br/>
   * The password will be generated via an Authentication Token using an rds cert.
   * @param username
   * @param dbEndpoint
   * @param region
   * @return
   */
  public static Connection createConnectionViaIamAuth(@NonNull String username, @NonNull String dbEndpoint, @NonNull String region) {
    Connection connection;
    try {
      setSslProperties();
      connection = DriverManager.getConnection(JDBC_PREFIX + dbEndpoint, setMySqlConnectionProperties(username, dbEndpoint, region));
      return connection;
      
    } catch (Exception e) {
      // TODO add retry-logic
      System.out.println("Connection FAILED");
      e.printStackTrace();
    }
    return null;
  }
  
  /**
   * Create a database connection via a username + password authentication
   * @param username
   * @param pwd
   * @param dbEndpoint
   * @return
   */
  public static Connection createConnectionViaUserPwd(@NonNull String username, @NonNull String pwd, @NonNull String dbEndpoint) {
    Connection connection;
    try {
      connection = DriverManager.getConnection(JDBC_PREFIX + dbEndpoint, username, pwd);
      System.out.println("Connection Established");
      return connection;
    } catch (SQLException e) {
      // TODO add retry-logic
      System.out.println("Connection FAILED");
      e.printStackTrace();
    }
    return null;
  }

  
  /**
   * This method generates the IAM Authentication Token, <br/>
   * which will be later used as the password for authenticating to the DB.
   * @return the authentication token
   */
  private static String generateAuthToken(String username, String dbEndpoint, String region){
    long start = System.currentTimeMillis();
    RdsUtilities utilities = RdsUtilities.builder()
      .credentialsProvider(DefaultCredentialsProvider.create())
      .region(Region.of(region))
      .build();
    GenerateAuthenticationTokenRequest authTokenRequest = GenerateAuthenticationTokenRequest.builder()
      .username(username)
      .hostname(dbEndpoint)
      .port(3306)
      .build();
    String authenticationToken = utilities.generateAuthenticationToken(authTokenRequest);

    return authenticationToken;
  }

  /**
   * This method sets the mysql connection properties which includes the IAM Database Authentication token
   * as the password. It also specifies that SSL verification is required.
   * @return
  */
  private static Properties setMySqlConnectionProperties(String username, String dbEndpoint, String region) {
    Properties mysqlConnectionProperties = new Properties();
    mysqlConnectionProperties.setProperty("useSSL", "true");
    mysqlConnectionProperties.setProperty("user", username);
    mysqlConnectionProperties.setProperty("password", generateAuthToken(username, dbEndpoint, region));
    return mysqlConnectionProperties;
  }
  
  /**
   * This method sets the SSL properties which specify the key store file, its type and password:
   * @throws Exception
  */
  private static void setSslProperties() throws Exception {
    File keyStoreFile = createKeyStoreFile(createCertificate());
    System.setProperty("javax.net.ssl.trustStore", keyStoreFile.getPath());
    System.setProperty("javax.net.ssl.trustStoreType", KEY_STORE_TYPE);
    System.setProperty("javax.net.ssl.trustStorePassword", DEFAULT_KEY_STORE_PASSWORD);
   }

  /**
   *  This method generates the SSL certificate
   * @return
   * @throws Exception
  */
  private static X509Certificate createCertificate() throws Exception {
    CertificateFactory certFactory = CertificateFactory.getInstance("X.509"); 
    URL url = new File(SSL_CERTIFICATE).toURI().toURL();
    try (InputStream certInputStream = url.openStream()) {
        return (X509Certificate) certFactory.generateCertificate(certInputStream);
    }
  }

  /**
   * This method creates the Key Store File needed for the SSL verification <br/>
   * during the IAM Database Authentication to the db instance
   * @param rootX509Certificate - the SSL certificate to be stored in the KeyStore
   * @return
   * @throws Exception
  */
  private static File createKeyStoreFile(X509Certificate rootX509Certificate) throws Exception {
    File keyStoreFile = File.createTempFile(KEY_STORE_FILE_PREFIX, KEY_STORE_FILE_SUFFIX);
    try (FileOutputStream fos = new FileOutputStream(keyStoreFile.getPath())) {
        KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE, KEY_STORE_PROVIDER);
        ks.load(null);
        ks.setCertificateEntry("rootCaCertificate", rootX509Certificate);
        ks.store(fos, DEFAULT_KEY_STORE_PASSWORD.toCharArray());
    }
    return keyStoreFile;
  }
}
