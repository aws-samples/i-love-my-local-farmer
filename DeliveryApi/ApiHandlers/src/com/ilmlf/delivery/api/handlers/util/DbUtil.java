package com.ilmlf.delivery.api.handlers.util;

import com.ilmlf.delivery.api.handlers.CreateSlots;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;
import software.amazon.lambda.powertools.tracing.Tracing;


/**
 * Database Utility class. Used for connecting to the database and executing RDS commands.
 * It provides two authentication methods. One is via DB username and password.
 * Another is via IAM Auth. In the IAM Auth method, the client doesn't need to know the password
 * but it needs IAM permission to use that username. RDS Proxy will fetch the password from
 * a Secret Store.
 */
public class DbUtil {
  public static final String SSL_CERTIFICATE = "rds-ca-2019-root.pem";
  private static final String KEY_STORE_TYPE = "JKS";
  private static final String KEY_STORE_PROVIDER = "SUN";
  private static final String KEY_STORE_FILE_PREFIX = "sys-connect-via-ssl-test-cacerts";
  private static final String KEY_STORE_FILE_SUFFIX = ".jks";
  private static final String DEFAULT_KEY_STORE_PASSWORD = "delivery";
  private static final String JDBC_PREFIX = "jdbc:mysql://";
  private static final Logger logger = LogManager.getLogger(CreateSlots.class);

  /**
   * Creates a database connection via IAM Authentication.
   * The password will be generated via an Authentication Token using an RDS cert.
   *
   * @param username username that the Lambda has access in IAM permission (e.g lambda_iam)
   * @param dbEndpoint RDS proxy endpoint
   * @param region RDS region
   * @param port RDS endpoint port
   * @return a connection using IAM authentication
   */
  @Tracing(segmentName = "CreateDBConnection")
  public Connection createConnectionViaIamAuth(@NonNull String username,
                                               @NonNull String dbEndpoint,
                                               @NonNull String region,
                                               Integer port) {
    Connection connection;
    try {
      setSslProperties();
      connection = DriverManager.getConnection(
          JDBC_PREFIX + dbEndpoint,
          setMySqlConnectionProperties(username, dbEndpoint, region, port));
      return connection;

    } catch (Exception e) {
      logger.info("Connection FAILED");
      logger.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * Creates a database connection via IAM Authentication with MySQL default port (3306).
   *
   * @param username username that the Lambda has access in IAM permission (e.g lambda_iam)
   * @param dbEndpoint RDS proxy endpoint
   * @param region RDS region
   * @return a connection using IAM authentication
   */
  public Connection createConnectionViaIamAuth(
      @NonNull String username, @NonNull String dbEndpoint, @NonNull String region) {
    return this.createConnectionViaIamAuth(username, dbEndpoint, region, 3306);
  }

  /**
   * Creates a database connection via username + password authentication.
   *
   * @param username DB username
   * @param pwd DB password
   * @param dbEndpoint DB Instance (or proxy) endpoint
   * @return a Connection object
   */
  public static Connection createConnectionViaUserPwd(
      @NonNull String username, @NonNull String pwd, @NonNull String dbEndpoint) {
    Connection connection;

    try {
      connection = DriverManager.getConnection(JDBC_PREFIX + dbEndpoint, username, pwd);
      logger.info("Connection Established");
      return connection;

    } catch (SQLException e) {
      logger.info("Connection FAILED");
      logger.error(e.getMessage(), e);
    }

    return null;
  }


  /**
   * This method generates the IAM Authentication Token.
   * The token will be later used as the password for authenticating to the DB.
   *
   * @return the authentication token
   */
  public static String generateAuthToken(String username, String dbEndpoint, String region, Integer port) {
    RdsUtilities utilities = RdsUtilities.builder()
        .credentialsProvider(DefaultCredentialsProvider.create())
        .region(Region.of(region))
        .build();

    GenerateAuthenticationTokenRequest authTokenRequest = GenerateAuthenticationTokenRequest.builder()
        .username(username)
        .hostname(dbEndpoint)
        .port(port)
        .build();

    String authenticationToken = utilities.generateAuthenticationToken(authTokenRequest);

    return authenticationToken;
  }

  /**
   * This method sets the mysql connection properties, which includes the IAM Database Authentication token
   * as the password. It also specifies that SSL verification is required.
   *
   * @param username Username
   * @param dbEndpoint Database endpoint
   * @param region AWS Region of the database
   * @param port Port for connecting to the endpoint
   * @return MySQL connection property
   */
  private static Properties setMySqlConnectionProperties(String username,
                                                         String dbEndpoint,
                                                         String region,
                                                         Integer port) {
    Properties mysqlConnectionProperties = new Properties();
    mysqlConnectionProperties.setProperty("useSSL", "true");
    mysqlConnectionProperties.setProperty("user", username);
    mysqlConnectionProperties.setProperty("password", generateAuthToken(username, dbEndpoint, region, port));

    return mysqlConnectionProperties;
  }

  /**
   * Sets the System's SSL properties which specify the key store file, its type and password.
   *
   * @throws GeneralSecurityException when creating the key in the key store fails
   * @throws IOException when creating a temp file or reading a keystore file fails
   */
  private static void setSslProperties() throws GeneralSecurityException, IOException {
    File keyStoreFile = createKeyStoreFile(createCertificate(SSL_CERTIFICATE));
    System.setProperty("javax.net.ssl.trustStore", keyStoreFile.getPath());
    System.setProperty("javax.net.ssl.trustStoreType", KEY_STORE_TYPE);
    System.setProperty("javax.net.ssl.trustStorePassword", DEFAULT_KEY_STORE_PASSWORD);
  }

  /**
   * Creates the SSL certificate.
   *
   * @return X509Certificate certificate for SSL connection
   * @throws GeneralSecurityException when creating the key in the key store fails
   * @throws IOException when creating a temp file or reading a keystore file fails
   */
  public static X509Certificate createCertificate(String certFile) throws  GeneralSecurityException, IOException {
    CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

    try (InputStream certInputStream = DbUtil.class.getResourceAsStream("/" + certFile)) {
      return (X509Certificate) certFactory.generateCertificate(certInputStream);
    }
  }

  /**
   * This method creates the Key Store File needed for the SSL verification <br/>
   * during the IAM Database Authentication to the db instance.
   *
   * @param rootX509Certificate - the SSL certificate to be stored in the KeyStore
   * @return the keystore file
   * @throws GeneralSecurityException when creating the key in key store fails
   * @throws IOException when creating temp file or reading a keystore file fails
   */
  private static File createKeyStoreFile(X509Certificate rootX509Certificate)
      throws GeneralSecurityException, IOException {
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
