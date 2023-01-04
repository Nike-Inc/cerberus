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

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoMaterialsManager;
import com.amazonaws.encryptionsdk.DefaultCryptoMaterialsManager;
import com.amazonaws.encryptionsdk.MasterKeyProvider;
import com.amazonaws.encryptionsdk.ParsedCiphertext;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.amazonaws.encryptionsdk.multi.MultipleProviderFactory;
import com.amazonaws.regions.Region;
import com.google.common.collect.Lists;
import com.nike.cerberus.util.CiphertextUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Service for performing encryption and decryption of secrets using the 'AWS Encryption SDK'. */
@Component
public class EncryptionService {

  /** Property name for current SDB path in the EncryptionContext */
  public static final String SDB_PATH_PROPERTY_NAME = "sdb_path";

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final AwsCrypto awsCrypto;
  private List<String> cmkArnList;
  private final Region currentRegion;
  private final CryptoMaterialsManager decryptCryptoMaterialsManager;
  private final CryptoMaterialsManager encryptCryptoMaterialsManager;

  @Autowired
  public EncryptionService(
      AwsCrypto awsCrypto,
      @Value("${cerberus.encryption.cmk.arns}") String cmkArns,
      @Qualifier("decryptCryptoMaterialsManager")
          CryptoMaterialsManager decryptCryptoMaterialsManager,
      @Qualifier("encryptCryptoMaterialsManager")
          CryptoMaterialsManager encryptCryptoMaterialsManager,
      Region currentRegion) {
    this.currentRegion = currentRegion;
    this.awsCrypto = awsCrypto;
    log.info("CMK ARNs " + cmkArns);
    this.cmkArnList = splitArns(cmkArns);
    this.decryptCryptoMaterialsManager = decryptCryptoMaterialsManager;
    this.encryptCryptoMaterialsManager = encryptCryptoMaterialsManager;
  }

  /**
   * Encrypt the plainTextPayload.
   *
   * <p>Generates a Base64 encoded String the the 'AWS Encryption SDK Message Format'
   *
   * <p>http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/message-format.html
   *
   * @param plainTextPayload the secrets to encrypt
   * @param sdbPath the SDB path where these secrets are being stored (added to EncryptionContext)
   */
  public String encrypt(String plainTextPayload, String sdbPath) {
    return awsCrypto
        .encryptString(
            encryptCryptoMaterialsManager, plainTextPayload, buildEncryptionContext(sdbPath))
        .getResult();
  }

  public byte[] encrypt(byte[] bytes, String sdbPath) {
    return awsCrypto
        .encryptData(encryptCryptoMaterialsManager, bytes, buildEncryptionContext(sdbPath))
        .getResult();
  }

  /**
   * Decrypt the encryptedPayload.
   *
   * <p>Expects a Base64 encoded String the the 'AWS Encryption SDK Message Format'.
   *
   * <p>http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/message-format.html
   */
  public String decrypt(String encryptedPayload, String sdbPath) {
    ParsedCiphertext parsedCiphertext = getParsedCipherText(encryptedPayload);
    try {
      return decrypt(parsedCiphertext, sdbPath);
    } catch (RuntimeException e) {
      log.error("Decrypt operation failed " + CiphertextUtils.toJson(parsedCiphertext), e);
      throw e;
    }
  }

  ParsedCiphertext getParsedCipherText(String encryptedPayload) {
    return CiphertextUtils.parse(encryptedPayload);
  }

  ParsedCiphertext getParsedCipherText(byte[] encryptedPayload) {
    return CiphertextUtils.parse(encryptedPayload);
  }

  /**
   * Decrypt the encryptedPayload.
   *
   * <p>Expects a Base64 encoded String the the 'AWS Encryption SDK Message Format'.
   *
   * <p>http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/message-format.html
   */
  public byte[] decrypt(byte[] encryptedPayload, String sdbPath) {
    ParsedCiphertext parsedCiphertext = getParsedCipherText(encryptedPayload);
    try {
      return decryptToBytes(parsedCiphertext, sdbPath);
    } catch (RuntimeException e) {
      log.error("Decrypt operation failed " + CiphertextUtils.toJson(parsedCiphertext), e);
      throw e;
    }
  }

  /**
   * Decrypt the encryptedPayload.
   *
   * @param parsedCiphertext encryptedPayload
   * @param sdbPath the current SDB path
   */
  private String decrypt(ParsedCiphertext parsedCiphertext, String sdbPath) {
    validateEncryptionContext(parsedCiphertext, sdbPath);
    // Parses the ARNs out of the encryptedPayload so that you can manually rotate the CMKs, if
    // desired
    // Whatever CMKs were used in the encrypt operation will be used to decrypt
    List<String> cmkArns = CiphertextUtils.getCustomerMasterKeyArns(parsedCiphertext);
    CryptoMaterialsManager cryptoMaterialsManager =
        getCryptoMaterialsManager(cmkArns, currentRegion);
    return new String(
        awsCrypto.decryptData(cryptoMaterialsManager, parsedCiphertext).getResult(),
        StandardCharsets.UTF_8);
  }

  private CryptoMaterialsManager getCryptoMaterialsManager(
      List<String> cmkArns, Region currentRegion) {
    if (cmkArnList.containsAll(cmkArns)) {
      return decryptCryptoMaterialsManager;
    } else {
      MasterKeyProvider<KmsMasterKey> provider = initializeKeyProvider(cmkArns, currentRegion);
      return new DefaultCryptoMaterialsManager(provider);
    }
  }

  /**
   * Re-encrypt (i.e. decrypt then encrypt) String ciphertext
   *
   * @param encryptedPayload encryptedPayload
   * @param sdbPath the current SDB path
   * @return re-encrypted ciphertext
   */
  public String reencrypt(String encryptedPayload, String sdbPath) {
    String plaintext = decrypt(encryptedPayload, sdbPath);
    return encrypt(plaintext, sdbPath);
  }

  /**
   * Re-encrypt (i.e. decrypt then encrypt) byte array ciphertext
   *
   * @param encryptedPayload encryptedPayload
   * @param sdbPath the current SDB path
   * @return re-encrypted ciphertext
   */
  public byte[] reencrypt(byte[] encryptedPayload, String sdbPath) {
    byte[] plaintextBytes = decrypt(encryptedPayload, sdbPath);
    return encrypt(plaintextBytes, sdbPath);
  }

  /**
   * Decrypt the encryptedPayload.
   *
   * @param parsedCiphertext encryptedPayload
   * @param sdbPath the current SDB path
   */
  private byte[] decryptToBytes(ParsedCiphertext parsedCiphertext, String sdbPath) {
    validateEncryptionContext(parsedCiphertext, sdbPath);
    // Parses the ARNs out of the encryptedPayload so that you can manually rotate the CMKs, if
    // desired
    // Whatever CMKs were used in the encrypt operation will be used to decrypt
    List<String> cmkArns = CiphertextUtils.getCustomerMasterKeyArns(parsedCiphertext);
    CryptoMaterialsManager cryptoMaterialsManager =
        getCryptoMaterialsManager(cmkArns, currentRegion);
    return awsCrypto.decryptData(cryptoMaterialsManager, parsedCiphertext).getResult();
  }

  /**
   * Decrypt the encryptedPayload.
   *
   * @param parsedCiphertext encryptedPayload
   */
  public static String decrypt(
      ParsedCiphertext parsedCiphertext, AwsCrypto awsCrypto, Region currentRegion) {
    // Parses the ARNs out of the encryptedPayload so that you can manually rotate the CMKs, if
    // desired
    // Whatever CMKs were used in the encrypt operation will be used to decrypt
    List<String> cmkArns = CiphertextUtils.getCustomerMasterKeyArns(parsedCiphertext);
    MasterKeyProvider<KmsMasterKey> decryptProvider = initializeKeyProvider(cmkArns, currentRegion);
    return new String(
        awsCrypto.decryptData(decryptProvider, parsedCiphertext).getResult(),
        StandardCharsets.UTF_8);
  }

  /**
   * Validate the encryptionContext for the parsedCiphertext includes the expected sdbPath.
   *
   * <p>This step validates that the encrypted payload was created for the SDB that is currently
   * being decrypted. It is an integrity check. If this validation fails then the encrypted payload
   * may have been tampered with, e.g. copying the encrypted payload between two SDBs.
   *
   * @param parsedCiphertext the ciphertext to read the encryptionContext from
   * @param sdbPath the path expected in the encryptionContext
   */
  private void validateEncryptionContext(ParsedCiphertext parsedCiphertext, String sdbPath) {
    Map<String, String> encryptionContext = parsedCiphertext.getEncryptionContextMap();
    String pathFromEncryptionContext = encryptionContext.getOrDefault(SDB_PATH_PROPERTY_NAME, null);
    if (!StringUtils.equals(pathFromEncryptionContext, sdbPath)) {
      log.error("EncryptionContext did not have expected path, possible tampering: " + sdbPath);
      throw new IllegalArgumentException(
          "EncryptionContext did not have expected path, possible tampering: " + sdbPath);
    }
  }

  /** Split the ARNs from a single comma delimited string into a list. */
  public static List<String> splitArns(String cmkArns) {
    List<String> keyArns = Lists.newArrayList(StringUtils.split(cmkArns, ","));
    if (keyArns.size() < 2) {
      throw new IllegalArgumentException(
          "At least 2 CMK ARNs are required for high availability, size:" + keyArns.size());
    }
    return keyArns;
  }

  /**
   * Initialize a Multi-KMS-MasterKeyProvider.
   *
   * <p>For encrypt, KMS in all regions must be available. For decrypt, KMS in at least one region
   * must be available.
   */
  public static MasterKeyProvider<KmsMasterKey> initializeKeyProvider(
      List<String> cmkArns, Region currentRegion) {
    List<MasterKeyProvider<KmsMasterKey>> providers = new ArrayList();
    List<String> arnList = getSortedArnListByCurrentRegion(cmkArns, currentRegion);
    providers.add(KmsMasterKeyProvider.builder().buildStrict(arnList));
    return (MasterKeyProvider<KmsMasterKey>) MultipleProviderFactory.buildMultiProvider(providers);
  }

  /**
   * Initialize a Multi-KMS-MasterKeyProvider.
   *
   * <p>For encrypt, KMS in all regions must be available. For decrypt, KMS in at least one region
   * must be available.
   */
  public static MasterKeyProvider<KmsMasterKey> initializeKeyProvider(
      String cmkArns, Region currentRegion) {
    return initializeKeyProvider(splitArns(cmkArns), currentRegion);
  }

  /** ARN with current region should always go first to minimize latency */
  protected static List<String> getSortedArnListByCurrentRegion(
      List<String> cmkArns, Region currentRegion) {
    return cmkArns.stream()
        .sorted(
            (s1, s2) -> {
              if (s1.contains(currentRegion.getName())) {
                // ARN with current region should always go first
                return -1;
              } else if (s2.contains(currentRegion.getName())) {
                // ARN with current region should always go first
                return 1;
              } else {
                // otherwise order isn't that important
                return s1.compareTo(s2);
              }
            })
        .collect(Collectors.toList());
  }

  /**
   * Generate an encryption context (additional information about the payload). This context is not
   * encrypted and should not contain secrets.
   */
  protected Map<String, String> buildEncryptionContext(String sdbPath) {
    Map<String, String> context = new HashMap<>();
    context.put("created_on", DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
    context.put(SDB_PATH_PROPERTY_NAME, sdbPath);
    return context;
  }
}
