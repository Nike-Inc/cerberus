package com.nike.cerberus.service;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.MasterKeyProvider;
import com.amazonaws.encryptionsdk.ParsedCiphertext;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.amazonaws.encryptionsdk.multi.MultipleProviderFactory;
import com.mysql.jdbc.StringUtils;
import com.nike.cerberus.util.CiphertextUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for performing encryption and decryption of secrets using the 'AWS Encryption SDK'.
 */
public class EncryptionService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AwsCrypto awsCrypto;
    private final MasterKeyProvider<KmsMasterKey> encryptProvider;
    private final String cmsVersion;

    @Inject
    public EncryptionService(AwsCrypto awsCrypto,
                             @Named("cms.encryption.cmk.arns") String cmkArns,
                             @Named("service.version") String cmsVersion) {
        this.awsCrypto = awsCrypto;
        this.encryptProvider = initializeKeyProvider(splitArns(cmkArns));
        this.cmsVersion = cmsVersion;
    }

    /**
     * Encrypt the plainTextPayload.
     * <p>
     * Generates a Base64 encoded String the the 'AWS Encryption SDK Message Format'
     * <p>
     * http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/message-format.html
     */
    public String encrypt(String plainTextPayload) {
        return awsCrypto.encryptString(encryptProvider, plainTextPayload, buildEncryptionContext()).getResult();
    }

    /**
     * Decrypt the encryptedPayload.
     * <p>
     * Expects a Base64 encoded String the the 'AWS Encryption SDK Message Format'.
     * <p>
     * http://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/message-format.html
     */
    public String decrypt(String encryptedPayload) {
        ParsedCiphertext parsedCiphertext = CiphertextUtils.parse(encryptedPayload);
        try {
            return decrypt(parsedCiphertext);
        } catch (RuntimeException e) {
            log.error("Decrypt operation failed " + CiphertextUtils.toJson(parsedCiphertext), e);
            throw e;
        }
    }

    /**
     * Decrypt the encryptedPayload.
     */
    private String decrypt(ParsedCiphertext parsedCiphertext) {
        // Parses the ARNs out of the encryptedPayload so that you can manually rotate the CMKs, if desired
        // Whatever CMKs were used in the encrypt operation will be used to decrypt
        List<String> cmkArns = CiphertextUtils.getCustomerMasterKeyArns(parsedCiphertext);
        MasterKeyProvider<KmsMasterKey> decryptProvider = initializeKeyProvider(cmkArns);
        return new String(awsCrypto.decryptData(decryptProvider, parsedCiphertext).getResult(), StandardCharsets.UTF_8);

    }

    /**
     * Split the ARNs from a single comma delimited string into a list.
     */
    protected List<String> splitArns(String cmkArns) {
        log.info("CMK ARNs " + cmkArns);
        List<String> keyArns = StringUtils.split(cmkArns, ",", true);
        if (keyArns.size() < 2) {
            throw new IllegalArgumentException("At least 2 CMK ARNs are required for high availability, size:" + keyArns.size());
        }
        return keyArns;
    }

    /**
     * Initialize a Multi-KMS-MasterKeyProvider.
     * <p>
     * For encrypt, KMS in all regions must be available.
     * For decrypt, KMS in at least one region must be available.
     */
    protected MasterKeyProvider<KmsMasterKey> initializeKeyProvider(List<String> cmkArns) {
        List<MasterKeyProvider<KmsMasterKey>> providers = cmkArns.stream()
                .map(KmsMasterKeyProvider::new)
                .collect(Collectors.toList());
        return (MasterKeyProvider<KmsMasterKey>) MultipleProviderFactory.buildMultiProvider(providers);
    }

    /**
     * Generate an encryption context (additional information about the payload).
     * This context is not encrypted and should not contain secrets.
     */
    protected Map<String, String> buildEncryptionContext() {
        Map<String, String> context = new HashMap<>();
        context.put("created_on", DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
        context.put("created_by", cmsVersion);
        return context;
    }


}
