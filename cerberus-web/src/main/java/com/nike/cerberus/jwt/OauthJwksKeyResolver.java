package com.nike.cerberus.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A subclass of {@link SigningKeyResolverAdapter} that resolves the key used for JWT signing and
 * signature validation of OAuth JWT
 */
@Component
public class OauthJwksKeyResolver extends SigningKeyResolverAdapter {

  private final ObjectMapper objectMapper;

  private final String jwksUrl;
  private final JwksHttpClient jwksHttpClient;
  private Map<String, PublicKey> keyMap;

  protected final Logger log = LoggerFactory.getLogger(this.getClass());

  private PublicKey getRsaPublicKey(JsonWebKeySet.JsonWebKey jsonWebKey) {
    BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(jsonWebKey.getN()));
    BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(jsonWebKey.getE()));
    try {
      return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException(
          "The key is invalid. Maybe check the JWKS endpoint to see if the keys are valid.", e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Well this is not supposed to happen.", e);
    }
  }

  @Autowired
  public OauthJwksKeyResolver(
      ObjectMapper objectMapper,
      @Value("${cerberus.auth.oauth.jwksUrl}") String jwksUrl,
      JwksHttpClient jwksHttpClient) {
    this.objectMapper = objectMapper;
    this.jwksUrl = jwksUrl;
    this.jwksHttpClient = jwksHttpClient;
    JsonWebKeySet jsonWebKeySet = this.jwksHttpClient.getJsonWebKeySet(jwksUrl);
    keyMap =
        Arrays.stream(jsonWebKeySet.getKeys())
            .collect(
                Collectors.toConcurrentMap(
                    JsonWebKeySet.JsonWebKey::getKid, this::getRsaPublicKey));
  }

  @Override
  public Key resolveSigningKey(JwsHeader jwsHeader, Claims claims) {
    String keyId = jwsHeader.getKeyId();
    Key key = lookupVerificationKey(keyId);

    return key;
  }

  private PublicKey lookupVerificationKey(String keyId) {
    if (StringUtils.isBlank(keyId)) {
      throw new IllegalArgumentException("Key ID cannot be empty");
    }
    try {
      PublicKey keySpec = keyMap.get(keyId);
      if (keySpec == null) {
        throw new IllegalArgumentException("The key ID " + keyId + " is invalid or expired");
      }

      return keySpec;
    } catch (NullPointerException e) {
      throw new IllegalArgumentException("The key ID " + keyId + " is either invalid or expired");
    }
  }

  /** Poll for JWT config and update key map with new data */
  public void refresh() {
    JsonWebKeySet jsonWebKeySet = this.jwksHttpClient.getJsonWebKeySet(this.jwksUrl);
    keyMap =
        Arrays.stream(jsonWebKeySet.getKeys())
            .collect(
                Collectors.toConcurrentMap(
                    JsonWebKeySet.JsonWebKey::getKid, this::getRsaPublicKey));
  }
}
