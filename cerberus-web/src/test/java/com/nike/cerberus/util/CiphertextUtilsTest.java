package com.nike.cerberus.util;

import com.amazonaws.encryptionsdk.CryptoAlgorithm;
import com.amazonaws.encryptionsdk.ParsedCiphertext;
import com.amazonaws.encryptionsdk.exception.BadCiphertextException;
import com.amazonaws.encryptionsdk.model.CiphertextType;
import com.amazonaws.encryptionsdk.model.ContentType;
import com.amazonaws.encryptionsdk.model.KeyBlob;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class CiphertextUtilsTest {

  @Test
  public void testParseCipherTextsAsBytes() {
    String exceptionMessage = "";
    try {
      String parsedText = "sampleParsedText";
      byte[] encodedParsedText =
          Base64.getEncoder().encode(parsedText.getBytes(StandardCharsets.UTF_8));
      ParsedCiphertext parsedCiphertext = CiphertextUtils.parse(encodedParsedText);
      Assert.assertNotNull(parsedCiphertext);
    } catch (BadCiphertextException e) {
      exceptionMessage = e.getMessage();
    }
    Assert.assertEquals("Invalid version", exceptionMessage.trim());
  }

  @Test
  public void testParseCipherTextsAsString() {
    String exceptionMessage = "";
    try {
      String parsedText = "sampleParsedText";
      byte[] encodedParsedText =
          Base64.getEncoder().encode(parsedText.getBytes(StandardCharsets.UTF_8));
      ParsedCiphertext parsedCiphertext =
          CiphertextUtils.parse(new String(encodedParsedText, StandardCharsets.UTF_8));
      Assert.assertNotNull(parsedCiphertext);
    } catch (BadCiphertextException e) {
      exceptionMessage = e.getMessage();
    }
    Assert.assertEquals("Invalid version", exceptionMessage.trim());
  }

  @Test
  public void testGetCustomerMasterKeyArns() {
    KeyBlob keyBlob =
        new KeyBlob(
            "aws-kms",
            "providerArnInfo".getBytes(StandardCharsets.UTF_8),
            "key".getBytes(StandardCharsets.UTF_8));
    KeyBlob keyBlob2 =
        new KeyBlob(
            "aws-km1",
            "providerArnInfo".getBytes(StandardCharsets.UTF_8),
            "key".getBytes(StandardCharsets.UTF_8));
    List<KeyBlob> keyBlobs = new ArrayList<>();
    keyBlobs.add(keyBlob);
    keyBlobs.add(keyBlob2);
    ParsedCiphertext parsedCiphertext = getParseCipherTextWithKeyBlobs(keyBlobs);
    List<String> customerMasterKeyArns = CiphertextUtils.getCustomerMasterKeyArns(parsedCiphertext);
    Assert.assertEquals(1, customerMasterKeyArns.size());
    Assert.assertEquals("providerArnInfo", customerMasterKeyArns.get(0));
  }

  private ParsedCiphertext getParseCipherTextWithKeyBlobs(List<KeyBlob> keyBlobs) {
    ParsedCiphertext parsedCiphertext = Mockito.mock(ParsedCiphertext.class);
    Mockito.when(parsedCiphertext.getEncryptedKeyBlobs()).thenReturn(keyBlobs);
    return parsedCiphertext;
  }

  @Test
  public void testToJsonObjectForParsedCipherText() {
    ParsedCiphertext parsedCiphertext = getParsedCiphertextWithMockValues();
    JsonObject jsonObject = CiphertextUtils.toJsonObject(parsedCiphertext);
    Assert.assertTrue(
        checkAllKeysPresentInJsonNode(
            jsonObject,
            "version",
            "type",
            "cryptoAlgoId",
            "messageId",
            "encryptionContext",
            "keyBlobCount",
            "keyBlobs",
            "contentType"));
  }

  @Test
  public void testToJson() {
    ParsedCiphertext parsedCiphertext = getParsedCiphertextWithMockValues();
    String json = CiphertextUtils.toJson(parsedCiphertext);
    JsonElement jsonElement = JsonParser.parseString(json);
    Assert.assertTrue(
        checkAllKeysPresentInJsonNode(
            jsonElement.getAsJsonObject(),
            "version",
            "type",
            "cryptoAlgoId",
            "messageId",
            "encryptionContext",
            "keyBlobCount",
            "keyBlobs",
            "contentType"));
  }

  private boolean checkAllKeysPresentInJsonNode(JsonObject jsonObject, String... keys) {
    return Arrays.stream(keys).allMatch(key -> jsonObject.has(key));
  }

  private ParsedCiphertext getParsedCiphertextWithMockValues() {
    ParsedCiphertext parsedCiphertext = Mockito.mock(ParsedCiphertext.class);
    KeyBlob keyBlob =
        new KeyBlob(
            "aws-kms",
            "providerArnInfo".getBytes(StandardCharsets.UTF_8),
            "key".getBytes(StandardCharsets.UTF_8));
    List<KeyBlob> keyBlobs = new ArrayList<>();
    keyBlobs.add(keyBlob);
    Mockito.when(parsedCiphertext.getEncryptedKeyBlobs()).thenReturn(keyBlobs);
    Mockito.when(parsedCiphertext.getType())
        .thenReturn(CiphertextType.CUSTOMER_AUTHENTICATED_ENCRYPTED_DATA);
    Mockito.when(parsedCiphertext.getVersion()).thenReturn(Byte.valueOf("0"));
    Mockito.when(parsedCiphertext.getCryptoAlgoId())
        .thenReturn(CryptoAlgorithm.ALG_AES_128_GCM_IV12_TAG16_HKDF_SHA256);
    byte[] messageId = Base64.getEncoder().encode("messageId".getBytes(StandardCharsets.UTF_8));
    Mockito.when(parsedCiphertext.getMessageId()).thenReturn(messageId);
    Mockito.when(parsedCiphertext.getEncryptionContextMap()).thenReturn(new HashMap<>());
    Mockito.when(parsedCiphertext.getEncryptedKeyBlobCount()).thenReturn(2);
    Mockito.when(parsedCiphertext.getContentType()).thenReturn(ContentType.FRAME);
    return parsedCiphertext;
  }
}
