package com.nike.cerberus.util;

import com.google.inject.name.Named;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.inject.Inject;

/**
 * Hash auth tokens.
 * <p>
 * Tokens have a configurable TTL, e.g. 1 hour.
 * <p>
 * https://www.owasp.org/index.php/Hashing_Java
 */
public class TokenHasher {

    public static final String HASH_SALT_CONFIG_PARAM = "auth.token.hashSalt";
    private final String hashSalt;

    @Inject
    public TokenHasher(@Named(HASH_SALT_CONFIG_PARAM) String hashSalt) {
        this.hashSalt = hashSalt;
    }

    /**
     * Perform one way hash
     *
     * @param token The token to hash
     * @return The hashed token
     */
    public String hashToken(String token) {
        int keyLength = 256;

        // TODO: additional research, security review
        // TODO: all constants in method should be configurable

        // https://stackoverflow.com/questions/29431884/java-security-pbekeyspec-how-many-iterations-are-enough
        int iterations = 100;
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            // TODO: base64 decode the hashSalt once we correct it in test environments
            PBEKeySpec spec = new PBEKeySpec(token.toCharArray(), hashSalt.getBytes(), iterations, keyLength);
            SecretKey key = skf.generateSecret(spec);
            return Hex.encodeHexString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("There was a problem hashing the token", e);
        }
    }
}
