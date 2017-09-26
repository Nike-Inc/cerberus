package com.nike.cerberus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for performing encryption and decryption of secrets.
 */
public class EncryptionService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public String encrypt(String plainTextPayload) {
        log.error("ENCRYPT NOT IMPLEMENTED RETURNING PLAIN TEXT");
        return plainTextPayload;
    }

    public String decrypt(String encryptedPayload) {
        log.error("DECRYPT NOT IMPLEMENTED RETURNING PLAIN TEXT");
        return encryptedPayload;
    }

}
