/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.config;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.netflix.spinnaker.kork.secrets.EncryptedSecret;
import com.netflix.spinnaker.kork.secrets.InvalidSecretFormatException;
import com.netflix.spinnaker.kork.secrets.SecretEngine;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecretsManagerSecretEngine implements SecretEngine {
  protected static final String SECRET_NAME = "s";
  protected static final String SECRET_REGION = "r";
  protected static final String SECRET_KEY = "k";

  private static String IDENTIFIER = "secrets-manager";

  private Map<String, Map<String, Object>> cache = new HashMap<>();

  @Override
  public String identifier() {
    return SecretsManagerSecretEngine.IDENTIFIER;
  }

  @Override
  public byte[] decrypt(EncryptedSecret encryptedSecret) {
    String secretName = encryptedSecret.getParams().get(SECRET_NAME);
    String secretRegion = encryptedSecret.getParams().get(SECRET_REGION);
    String secretKey = encryptedSecret.getParams().get(SECRET_KEY);

    AWSSecretsManager client =
        AWSSecretsManagerClientBuilder.standard().withRegion(secretRegion).build();

    byte[] binarySecret = null;
    GetSecretValueRequest getSecretValueRequest =
        new GetSecretValueRequest().withSecretId(secretName);
    GetSecretValueResult getSecretValueResult = null;

    try {
      getSecretValueResult = client.getSecretValue(getSecretValueRequest);
    } catch (Exception e) {
      log.error(
          "An error occurred when trying to use AWS Secrets Manager to fetch: [secretName: {}, secretRegion: {}, secretKey: {}]",
          secretName,
          secretRegion,
          secretKey,
          e);
      throw new RuntimeException("Failed to fetch secret from AWS Secrets Manager", e);
    }

    if (getSecretValueResult.getSecretString() != null) {
      String secret = getSecretValueResult.getSecretString();
      Gson gson = new Gson();
      Type type = new TypeToken<Map<String, String>>() {}.getType();
      Map<String, String> myMap = gson.fromJson(secret, type);
      binarySecret = myMap.get(secretKey).getBytes(StandardCharsets.UTF_8);
    } else {
      binarySecret = getSecretValueResult.getSecretBinary().array();
    }
    return binarySecret;
  }

  @Override
  public void validate(EncryptedSecret encryptedSecret) {
    Set<String> paramNames = encryptedSecret.getParams().keySet();
    if (!paramNames.contains(SECRET_NAME)) {
      throw new InvalidSecretFormatException(
          "Secret name parameter is missing (" + SECRET_NAME + "=...)");
    }
    if (!paramNames.contains(SECRET_REGION)) {
      throw new InvalidSecretFormatException(
          "Secret region parameter is missing (" + SECRET_REGION + "=...)");
    }
  }

  @Override
  public void clearCache() {
    cache.clear();
  }
}
