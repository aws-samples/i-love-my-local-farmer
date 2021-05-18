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

package com.ilmlf.customresource.utils;

import org.json.JSONObject;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

public abstract class SecretsUtil {

  public static JSONObject getSecret(String dbSecretName) {
    SecretsManagerClient secretsClient = SecretsManagerClient.builder()
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

  public static String getSecretValue(String dbSecretName) {
    SecretsManagerClient secretsClient = SecretsManagerClient.builder()
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .build();

    try {
      GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
          .secretId(dbSecretName)
          .build();

      GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest);
      String secret = valueResponse.secretString();
      secretsClient.close();
      return secret;
    } catch (SecretsManagerException e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      throw new RuntimeException(e);
    }
  }
}