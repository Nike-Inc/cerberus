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

package com.nike.cerberus.util;

import com.amazonaws.encryptionsdk.ParsedCiphertext;
import com.amazonaws.encryptionsdk.model.KeyBlob;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for working with the 'AWS Encryption SDK Message Format'.
 *
 * <p>http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/message-format.html
 */
public class CiphertextUtils {

  private static final String KMS_PROVIDER_ID = "aws-kms";

  /** Parse a Ciphertext in the 'AWS Encryption SDK Message Format' to an object. */
  public static ParsedCiphertext parse(String ciphertext) {
    byte[] bytes = Base64.getDecoder().decode(ciphertext);
    return new ParsedCiphertext(bytes);
  }

  /** Parse a Ciphertext in the 'AWS Encryption SDK Message Format' to an object. */
  public static ParsedCiphertext parse(byte[] ciphertext) {
    return new ParsedCiphertext(ciphertext);
  }

  /** Parse CMK ARNs out of the supplied ciphertext in the 'AWS Encryption SDK Message Format'. */
  public static List<String> getCustomerMasterKeyArns(ParsedCiphertext parsedCiphertext) {
    List<String> cmkArns = Lists.newArrayList();
    for (KeyBlob keyBlob : parsedCiphertext.getEncryptedKeyBlobs()) {
      if (KMS_PROVIDER_ID.equals(keyBlob.getProviderId())) {
        try {
          cmkArns.add(new String(keyBlob.getProviderInformation(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          LoggerFactory.getLogger("com.nike.cerberus.util.CiphertextUtils")
              .error("Failed to create new string from key blob provider information", e);
          throw new ApiException(DefaultApiError.INTERNAL_SERVER_ERROR);
        }
      }
    }
    return cmkArns;
  }

  /** Convert a ParsedCiphertext in the 'AWS Encryption SDK Message Format' to pretty usage JSON. */
  public static String toJson(ParsedCiphertext parsedCiphertext) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    return gson.toJson(toJsonObject(parsedCiphertext));
  }

  /** Convert a ParsedCiphertext in the 'AWS Encryption SDK Message Format' to a JsonObject. */
  public static JsonObject toJsonObject(ParsedCiphertext parsedCiphertext) {
    JsonObject o = new JsonObject();
    o.addProperty("version", parsedCiphertext.getVersion());
    o.addProperty("type", parsedCiphertext.getType().toString());
    o.addProperty("cryptoAlgoId", parsedCiphertext.getCryptoAlgoId().toString());
    o.addProperty("messageId", Base64.getEncoder().encodeToString(parsedCiphertext.getMessageId()));
    o.add("encryptionContext", new Gson().toJsonTree(parsedCiphertext.getEncryptionContextMap()));
    o.addProperty("keyBlobCount", parsedCiphertext.getEncryptedKeyBlobCount());
    JsonArray keyBlobs = new JsonArray();
    for (KeyBlob keyBlob : parsedCiphertext.getEncryptedKeyBlobs()) {
      JsonObject blob = new JsonObject();
      blob.addProperty("providerId", keyBlob.getProviderId());
      try {
        blob.addProperty("providerInfo", new String(keyBlob.getProviderInformation(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        LoggerFactory.getLogger("com.nike.cerberus.util.CiphertextUtils")
            .error("Failed to create new string from key blob provider information", e);
        throw new ApiException(DefaultApiError.INTERNAL_SERVER_ERROR);
      }
      blob.addProperty("isComplete:", keyBlob.isComplete());
      keyBlobs.add(blob);
    }
    o.add("keyBlobs", keyBlobs);
    o.addProperty("contentType", parsedCiphertext.getContentType().toString());
    return o;
  }
}
