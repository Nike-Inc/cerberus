package com.nike.cerberus.util;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Hash auth tokens.
 *
 * <p>Tokens are stored in an encrypted database. Hashing is done to further protect them against
 * anyone that has access to the database. Tokens have a configurable TTL, e.g. 1 hour, so the
 * hashing only needs to be secure enough to protect them for the duration of the TTL.
 *
 * <p>https://www.owasp.org/index.php/Hashing_Java
 * https://stackoverflow.com/questions/29431884/java-security-pbekeyspec-how-many-iterations-are-enough
 */
@Component
public class TokenHasher {

  // configuration parameter names
  public static final String HASH_SALT_CONFIG_PARAM = "${cerberus.auth.token.hash.salt}";
  public static final String HASH_ALGORITHM_CONFIG_PARAM = "${cerberus.auth.token.hash.algorithm}";
  public static final String HASH_KEY_LENGTH_CONFIG_PARAM = "${cerberus.auth.token.hash.keyLength}";
  public static final String HASH_ITERATIONS_CONFIG_PARAM =
      "${cerberus.auth.token.hash.iterations}";

  private final byte[] salt;
  private final String algorithm;
  private final int keyLength;
  private final int iterations;

  /**
   * Hash auth tokens
   *
   * @param hashSalt the salt to use as a base64 encoded string
   * @param algorithm the algorithm to use
   * @param keyLength the key length
   * @param iterations the number of iterations
   */
  @Autowired
  public TokenHasher(
      @Value(HASH_SALT_CONFIG_PARAM) final String hashSalt,
      @Value(HASH_ALGORITHM_CONFIG_PARAM) final String algorithm,
      @Value(HASH_KEY_LENGTH_CONFIG_PARAM) final int keyLength,
      @Value(HASH_ITERATIONS_CONFIG_PARAM) final int iterations) {
    this.salt = Base64.getDecoder().decode(hashSalt);
    this.algorithm = algorithm;
    this.keyLength = keyLength;
    this.iterations = iterations;

    if (salt.length < 64) {
      throw new IllegalArgumentException(
          HASH_SALT_CONFIG_PARAM + " must be at least 64 bytes but was " + salt.length);
    } else if (keyLength < 256) {
      throw new IllegalArgumentException(
          HASH_KEY_LENGTH_CONFIG_PARAM + " must be at least 256 but was " + keyLength);
    } else if (iterations < 100) {
      throw new IllegalArgumentException(
          HASH_ITERATIONS_CONFIG_PARAM + " must be at 100 but was " + iterations);
    }
  }

  /**
   * Perform one way hash
   *
   * @param token The token to hash
   * @return The hashed token
   */
  public String hashToken(final String token) {
    try {
      final SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
      final PBEKeySpec spec = new PBEKeySpec(token.toCharArray(), salt, iterations, keyLength);
      final SecretKey key = skf.generateSecret(spec);
      return Hex.encodeHexString(key.getEncoded());
    } catch (Exception e) {
      throw new RuntimeException("There was a problem hashing the token", e);
    }
  }
}
