package com.ilmlf.delivery.api.handlers.util;

import org.json.JSONObject;
import lombok.NonNull;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

public abstract class SecretsUtil {

  public static JSONObject getSecret(@NonNull String region, @NonNull String dbSecretName) {
    SecretsManagerClient secretsClient = SecretsManagerClient.builder()
            .region(Region.of(region))
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build();

    try {
      GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
              .secretId(dbSecretName)
              .build();


      GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest);
      String secret = valueResponse.secretString();
      JSONObject jo = new JSONObject(secret);
      secretsClient.close();
      return jo;
    } catch (SecretsManagerException e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      throw new RuntimeException(e);
    }
  }
}
