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

package com.nike.cerberus.service;

import static com.nike.cerberus.service.EncryptionService.SDB_PATH_PROPERTY_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoAlgorithm;
import com.amazonaws.encryptionsdk.CryptoMaterialsManager;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.MasterKeyProvider;
import com.amazonaws.encryptionsdk.ParsedCiphertext;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.model.CiphertextType;
import com.amazonaws.encryptionsdk.model.ContentType;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.google.common.collect.Lists;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class EncryptionServiceTest {

  @Mock private Region currentRegion;
  @Mock private CryptoMaterialsManager decryptCryptoMaterialsManager;
  @Mock private CryptoMaterialsManager encryptCryptoMaterialsManager;
  @Mock private AwsCrypto awsCrypto;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testArnFormatIsWrong() {
    String exceptionMessage = "";
    try {
      new EncryptionService(
          awsCrypto,
          "cmk",
          decryptCryptoMaterialsManager,
          encryptCryptoMaterialsManager,
          Region.getRegion(Regions.US_WEST_2));

    } catch (IllegalArgumentException illegalArgumentException) {
      exceptionMessage = illegalArgumentException.getMessage();
    }
    Assert.assertEquals(
        "At least 2 CMK ARNs are required for high availability, size:1", exceptionMessage);
  }

  @Test
  public void testEncryptWithString() {
    String expectedEncryptedValue = "encryptedValue";
    EncryptionService encryptionService = getEncryptionService();
    CryptoResult<String, ?> cryptoResult = getCryptoResult(expectedEncryptedValue);
    Mockito.when(
            awsCrypto.encryptString(
                Mockito.eq(encryptCryptoMaterialsManager),
                Mockito.eq("plainText"),
                Mockito.anyMap()))
        .thenReturn(cryptoResult);
    String encryptedValue = encryptionService.encrypt("plainText", "sdbPath");

    Assert.assertEquals(expectedEncryptedValue, encryptedValue);
  }

  @Test
  public void testEncryptWithBytes() {
    String expectedEncryptedValue = "encryptedValue";
    EncryptionService encryptionService = getEncryptionService();
    CryptoResult<byte[], ?> cryptoResult = getCryptoResultBytes(expectedEncryptedValue);
    Mockito.when(
            awsCrypto.encryptData(
                Mockito.eq(encryptCryptoMaterialsManager),
                Mockito.eq("plainText".getBytes(StandardCharsets.UTF_8)),
                Mockito.anyMap()))
        .thenReturn(cryptoResult);
    byte[] encryptedBytes =
        encryptionService.encrypt("plainText".getBytes(StandardCharsets.UTF_8), "sdbPath");
    Assert.assertEquals(expectedEncryptedValue, new String(encryptedBytes, StandardCharsets.UTF_8));
  }

  @Test
  public void testDecryptWhenEncryptionContentDidNotHaveExpectedPath() {
    EncryptionService encryptionService = Mockito.spy(getEncryptionService());
    ParsedCiphertext parsedCiphertext = getParsedCipherText();
    Mockito.doReturn(parsedCiphertext)
        .when(encryptionService)
        .getParsedCipherText("encryptedPayload");
    Map<String, String> contextMap = new HashMap<>();
    Mockito.when(parsedCiphertext.getEncryptionContextMap()).thenReturn(contextMap);
    String exceptionMessage = "";
    try {
      encryptionService.decrypt("encryptedPayload", "sdbPath");
    } catch (IllegalArgumentException illegalArgumentException) {
      exceptionMessage = illegalArgumentException.getMessage();
    }
    assertEquals(
        "EncryptionContext did not have expected path, possible tampering: sdbPath",
        exceptionMessage);
  }

  @Test
  public void testDecryptWhenEncryptionContent() {
    EncryptionService encryptionService = Mockito.spy(getEncryptionService());
    ParsedCiphertext parsedCiphertext = getParsedCipherText();
    Mockito.doReturn(parsedCiphertext)
        .when(encryptionService)
        .getParsedCipherText("encryptedPayload");
    Map<String, String> contextMap = new HashMap<>();
    contextMap.put(SDB_PATH_PROPERTY_NAME, "sdbPath");
    Mockito.when(parsedCiphertext.getEncryptionContextMap()).thenReturn(contextMap);
    CryptoResult cryptoResult = getCryptoResultBytes("decryptedData");
    Mockito.when(
            awsCrypto.decryptData(
                Mockito.any(CryptoMaterialsManager.class), Mockito.any(ParsedCiphertext.class)))
        .thenReturn(cryptoResult);
    String decryptedData = encryptionService.decrypt("encryptedPayload", "sdbPath");
    Assert.assertEquals("decryptedData", decryptedData);
  }

  @Test
  public void testDecryptBytesWhenEncryptionContentDidNotHaveExpectedPath() {
    EncryptionService encryptionService = Mockito.spy(getEncryptionService());
    ParsedCiphertext parsedCiphertext = getParsedCipherText();
    Mockito.doReturn(parsedCiphertext)
        .when(encryptionService)
        .getParsedCipherText("encryptedPayload".getBytes(StandardCharsets.UTF_8));
    Map<String, String> contextMap = new HashMap<>();
    Mockito.when(parsedCiphertext.getEncryptionContextMap()).thenReturn(contextMap);
    String exceptionMessage = "";
    try {
      encryptionService.decrypt("encryptedPayload".getBytes(StandardCharsets.UTF_8), "sdbPath");
    } catch (IllegalArgumentException illegalArgumentException) {
      exceptionMessage = illegalArgumentException.getMessage();
    }
    assertEquals(
        "EncryptionContext did not have expected path, possible tampering: sdbPath",
        exceptionMessage);
  }

  @Test
  public void testDecryptBytesWhenEncryptionContent() {
    EncryptionService encryptionService = Mockito.spy(getEncryptionService());
    ParsedCiphertext parsedCiphertext = getParsedCipherText();
    Mockito.doReturn(parsedCiphertext)
        .when(encryptionService)
        .getParsedCipherText("encryptedPayload".getBytes(StandardCharsets.UTF_8));
    Map<String, String> contextMap = new HashMap<>();
    contextMap.put(SDB_PATH_PROPERTY_NAME, "sdbPath");
    Mockito.when(parsedCiphertext.getEncryptionContextMap()).thenReturn(contextMap);
    CryptoResult cryptoResult = getCryptoResultBytes("decryptedData");
    Mockito.when(
            awsCrypto.decryptData(
                Mockito.any(CryptoMaterialsManager.class), Mockito.any(ParsedCiphertext.class)))
        .thenReturn(cryptoResult);
    byte[] decryptedData =
        encryptionService.decrypt("encryptedPayload".getBytes(StandardCharsets.UTF_8), "sdbPath");
    Assert.assertEquals("decryptedData", new String(decryptedData, StandardCharsets.UTF_8));
  }

  @Test
  public void testReEncryptWhenEncryptionContentDidNotHaveExpectedPath() {
    EncryptionService encryptionService = Mockito.spy(getEncryptionService());
    ParsedCiphertext parsedCiphertext = getParsedCipherText();
    Mockito.doReturn(parsedCiphertext)
        .when(encryptionService)
        .getParsedCipherText("encryptedPayload");
    Map<String, String> contextMap = new HashMap<>();
    Mockito.when(parsedCiphertext.getEncryptionContextMap()).thenReturn(contextMap);
    String exceptionMessage = "";
    try {
      encryptionService.reencrypt("encryptedPayload", "sdbPath");
    } catch (IllegalArgumentException illegalArgumentException) {
      exceptionMessage = illegalArgumentException.getMessage();
    }
    assertEquals(
        "EncryptionContext did not have expected path, possible tampering: sdbPath",
        exceptionMessage);
  }

  @Test
  public void testReEncryptWithBytes() {
    EncryptionService encryptionService = Mockito.spy(getEncryptionService());
    ParsedCiphertext parsedCiphertext = getParsedCipherText();
    Mockito.doReturn(parsedCiphertext)
        .when(encryptionService)
        .getParsedCipherText("encryptedPayload".getBytes(StandardCharsets.UTF_8));
    Map<String, String> contextMap = new HashMap<>();
    contextMap.put(SDB_PATH_PROPERTY_NAME, "sdbPath");
    Mockito.when(parsedCiphertext.getEncryptionContextMap()).thenReturn(contextMap);
    CryptoResult cryptoResult = getCryptoResultBytes("decryptedData");
    Mockito.when(
            awsCrypto.decryptData(
                Mockito.any(CryptoMaterialsManager.class), Mockito.any(ParsedCiphertext.class)))
        .thenReturn(cryptoResult);
    CryptoResult<byte[], ?> encryptedCryptoResult = getCryptoResultBytes("encryptedData");
    Mockito.when(
            awsCrypto.encryptData(
                Mockito.eq(encryptCryptoMaterialsManager),
                Mockito.eq("decryptedData".getBytes(StandardCharsets.UTF_8)),
                Mockito.anyMap()))
        .thenReturn(encryptedCryptoResult);
    byte[] reEncryptedData =
        encryptionService.reencrypt("encryptedPayload".getBytes(StandardCharsets.UTF_8), "sdbPath");
    Assert.assertEquals("encryptedData", new String(reEncryptedData, StandardCharsets.UTF_8));
  }

  @Test
  public void testReEncryptWithString() {
    EncryptionService encryptionService = Mockito.spy(getEncryptionService());
    ParsedCiphertext parsedCiphertext = getParsedCipherText();
    Mockito.doReturn(parsedCiphertext)
        .when(encryptionService)
        .getParsedCipherText("encryptedPayload");
    Map<String, String> contextMap = new HashMap<>();
    contextMap.put(SDB_PATH_PROPERTY_NAME, "sdbPath");
    Mockito.when(parsedCiphertext.getEncryptionContextMap()).thenReturn(contextMap);
    CryptoResult cryptoResult = getCryptoResultBytes("decryptedData");
    Mockito.when(
            awsCrypto.decryptData(
                Mockito.any(CryptoMaterialsManager.class), Mockito.any(ParsedCiphertext.class)))
        .thenReturn(cryptoResult);
    CryptoResult<String, ?> encryptedCryptoResult = getCryptoResult("encryptedData");
    Mockito.when(
            awsCrypto.encryptString(
                Mockito.eq(encryptCryptoMaterialsManager),
                Mockito.eq("decryptedData"),
                Mockito.anyMap()))
        .thenReturn(encryptedCryptoResult);
    String reEncryptedData = encryptionService.reencrypt("encryptedPayload", "sdbPath");
    Assert.assertEquals("encryptedData", reEncryptedData);
  }

  @Test
  public void testReEncryptBytesWhenEncryptionContentDidNotHaveExpectedPath() {
    EncryptionService encryptionService = Mockito.spy(getEncryptionService());
    ParsedCiphertext parsedCiphertext = getParsedCipherText();
    Mockito.doReturn(parsedCiphertext)
        .when(encryptionService)
        .getParsedCipherText("encryptedPayload".getBytes(StandardCharsets.UTF_8));
    Map<String, String> contextMap = new HashMap<>();
    Mockito.when(parsedCiphertext.getEncryptionContextMap()).thenReturn(contextMap);
    String exceptionMessage = "";
    try {
      encryptionService.reencrypt("encryptedPayload".getBytes(StandardCharsets.UTF_8), "sdbPath");
    } catch (IllegalArgumentException illegalArgumentException) {
      exceptionMessage = illegalArgumentException.getMessage();
    }
    assertEquals(
        "EncryptionContext did not have expected path, possible tampering: sdbPath",
        exceptionMessage);
  }

  private ParsedCiphertext getParsedCipherText() {
    ParsedCiphertext parsedCiphertext = Mockito.mock(ParsedCiphertext.class);
    byte b = 0;
    Mockito.when(parsedCiphertext.getVersion()).thenReturn(b);
    Mockito.when(parsedCiphertext.getType())
        .thenReturn(CiphertextType.CUSTOMER_AUTHENTICATED_ENCRYPTED_DATA);
    Mockito.when(parsedCiphertext.getCryptoAlgoId())
        .thenReturn(CryptoAlgorithm.ALG_AES_256_GCM_IV12_TAG16_HKDF_SHA384_ECDSA_P384);
    Mockito.when(parsedCiphertext.getMessageId())
        .thenReturn("messageId".getBytes(StandardCharsets.UTF_8));
    Mockito.when(parsedCiphertext.getEncryptedKeyBlobCount()).thenReturn(1);
    Mockito.when(parsedCiphertext.getContentType()).thenReturn(ContentType.SINGLEBLOCK);
    return parsedCiphertext;
  }

  private EncryptionService getEncryptionService() {
    EncryptionService encryptionService =
        new EncryptionService(
            awsCrypto,
            "cmk,Arns,cmk",
            decryptCryptoMaterialsManager,
            encryptCryptoMaterialsManager,
            Region.getRegion(Regions.US_WEST_2));
    return encryptionService;
  }

  private CryptoResult<String, ?> getCryptoResult(String value) {
    CryptoResult<String, ?> cryptoResult = Mockito.mock(CryptoResult.class);
    Mockito.when(cryptoResult.getResult()).thenReturn(value);
    return cryptoResult;
  }

  private CryptoResult<byte[], ?> getCryptoResultBytes(String value) {
    CryptoResult<byte[], ?> cryptoResult = Mockito.mock(CryptoResult.class);
    Mockito.when(cryptoResult.getResult()).thenReturn(value.getBytes(StandardCharsets.UTF_8));
    return cryptoResult;
  }

  @Test
  public void test_that_provider_has_current_region_first() {
    String arns =
        "arn:aws:kms:us-east-1:11111111:key/1111111-1d89-43ce-957b-0f705990e9d0,arn:aws:kms:us-west-2:11111111:key/11111111-aecd-4089-85e0-18536efa5c90";
    List<String> list =
        EncryptionService.getSortedArnListByCurrentRegion(
            Lists.newArrayList(StringUtils.split(arns, ",")), Region.getRegion(Regions.US_WEST_2));
    assertTrue(list.get(0).contains(Regions.US_WEST_2.getName()));
  }

  @Test
  public void testInitializeKeyProvider() {
    String arns =
        "arn:aws:kms:us-east-1:11111111:key/1111111-1d89-43ce-957b-0f705990e9d0,arn:aws:kms:us-west-2:11111111:key/11111111-aecd-4089-85e0-18536efa5c90";
    MasterKeyProvider<KmsMasterKey> kmsMasterKeyMasterKeyProvider =
        EncryptionService.initializeKeyProvider(arns, Region.getRegion(Regions.US_WEST_2));
    Assert.assertNotNull(kmsMasterKeyMasterKeyProvider);
  }
}
