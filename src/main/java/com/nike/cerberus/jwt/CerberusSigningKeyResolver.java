package com.nike.cerberus.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.name.Named;
import com.nike.cerberus.service.ConfigService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.security.Key;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A subclass of {@link SigningKeyResolverAdapter} that resolves the key used for JWT signing and signature validation
 */
@Singleton
public class CerberusSigningKeyResolver extends SigningKeyResolverAdapter {

    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    private CerberusJwtKeySpec signingKey;
    private Map<String, CerberusJwtKeySpec> keyMap;
    private boolean checkKeyRotation;
    private long nextRotationTs;
    private String nextKeyId;

    private static final String DEFAULT_ALGORITHM = "HmacSHA512";
    private static final String DEFAULT_JWT_ALG_HEADER = "HS512";
    private static final int DEFAULT_MINIMUM_KEY_LENGTH = 512 / 8; // hardcoding these for now

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public CerberusSigningKeyResolver(JwtServiceOptionalPropertyHolder jwtServiceOptionalPropertyHolder,
                                      ConfigService configService, ObjectMapper objectMapper) {
        this.configService = configService;
        this.objectMapper = objectMapper;

        // Override key with properties, useful for local development
        if (!StringUtils.isBlank(jwtServiceOptionalPropertyHolder.jwtSecretOverrideMaterial) &&
                !StringUtils.isBlank(jwtServiceOptionalPropertyHolder.jwtSecretOverrideKeyId)) {
            byte[] key = Base64.getDecoder().decode(jwtServiceOptionalPropertyHolder.jwtSecretOverrideMaterial);
            this.signingKey = new CerberusJwtKeySpec(key,
                    DEFAULT_ALGORITHM,
                    jwtServiceOptionalPropertyHolder.jwtSecretOverrideKeyId);
            rotateKeyMap(signingKey);
        } else {
            refresh();
        }
    }

    /**
     * This 'holder' class allows optional injection of Cerberus JWT-specific properties that are only necessary for
     * local development.
     *
     * The 'optional=true' parameter to Guice @Inject cannot be used in combination with the @Provides annotation
     * or with constructor injection.
     *
     * https://github.com/google/guice/wiki/FrequentlyAskedQuestions
     */
    static class JwtServiceOptionalPropertyHolder {
        private static final String JWT_SECRET_OVERRIDE_MATERIAL_CONFIG_PARAM = "cms.auth.jwt.secret.override.material";
        @com.google.inject.Inject(optional=true)
        @Named(JWT_SECRET_OVERRIDE_MATERIAL_CONFIG_PARAM)
        String jwtSecretOverrideMaterial;

        private static final String JWT_SECRET_OVERRIDE_KID_CONFIG_PARAM = "cms.auth.jwt.secret.override.kid";
        @com.google.inject.Inject(optional=true)
        @Named(JWT_SECRET_OVERRIDE_KID_CONFIG_PARAM)
        String jwtSecretOverrideKeyId;
    }

    @Override
    public Key resolveSigningKey(JwsHeader jwsHeader, Claims claims) {
        // Rejects non HS512 token
        if (!StringUtils.equals(DEFAULT_JWT_ALG_HEADER, jwsHeader.getAlgorithm())) {
            throw new IllegalArgumentException("Algorithm not supported");
        }
        String keyId = jwsHeader.getKeyId();
        Key key = lookupVerificationKey(keyId);

        return key;
    }

    /**
     * Return the signing key that should be used to sign JWT. The signing key is defined as the "newest active key"
     * i.e. key with the biggest effectiveTs value and effectiveTs <= now.
     * @return The signing key
     */
    public CerberusJwtKeySpec resolveSigningKey() {
        if (checkKeyRotation) {
            rotateSigningKey();
            return signingKey;
        } else {
            return signingKey;
        }
    }

    /**
     * Poll {@link ConfigService} for JWT config and update key map with new data
     */
    public void refresh() {
        JwtSecretData jwtSecretData = getJwtSecretData();

        rotateKeyMap(jwtSecretData);
        setSigningKey(jwtSecretData);
    }

    /**
     * Poll {@link ConfigService} for JWT config and validate new data
     * @return JWT config
     */
    protected JwtSecretData getJwtSecretData() {
        String jwtSecretsString = configService.getJwtSecrets();
        try {
            JwtSecretData jwtSecretData = objectMapper.readValue(jwtSecretsString, JwtSecretData.class);
            validateJwtSecretData(jwtSecretData);
            return jwtSecretData;
        } catch (IOException e) {
            log.error("IOException encountered during deserialization");
            throw new RuntimeException(e);
        }
    }

    /**
     * Validate {@link JwtSecretData}. Validates required fields and rejects weak keys.
     * @param jwtSecretData JWT config
     */
    protected void validateJwtSecretData(JwtSecretData jwtSecretData) {
        if (jwtSecretData == null || jwtSecretData.getJwtSecrets() == null) {
            throw new IllegalArgumentException("JWT secret data cannot be null");
        }
        if (jwtSecretData.getJwtSecrets().isEmpty()) {
            throw new IllegalArgumentException("JWT secret data cannot be empty");
        }

        long minEffectiveTs = 0;

        for (JwtSecret jwtSecret : jwtSecretData.getJwtSecrets()) {
            if (jwtSecret.getSecret() == null) {
                throw new IllegalArgumentException("JWT secret cannot be null");
            }
            if (Base64.getDecoder().decode(jwtSecret.getSecret()).length < DEFAULT_MINIMUM_KEY_LENGTH) {
                throw new IllegalArgumentException("JWT secret does NOT meet minimum length requirement of " + DEFAULT_MINIMUM_KEY_LENGTH);
            }
            if (StringUtils.isBlank(jwtSecret.getId())) {
                throw new IllegalArgumentException("JWT secret key ID cannot be empty");
            }
            minEffectiveTs = Math.min(minEffectiveTs, jwtSecret.getEffectiveTs());
        }

        long now = System.currentTimeMillis();
        if (now < minEffectiveTs) {
            // Prevents rotation or start up if no key is active
            throw new IllegalArgumentException("Requires at least 1 active JWT secret");
        }
    }

    /**
     * Set the signing key that should be used to sign JWT and the next signing key in line. The signing key is defined
     * as the "newest active key" i.e. key with the biggest effectiveTs value and effectiveTs <= now.
     * @param jwtSecretData JWT config
     */
    protected void setSigningKey(JwtSecretData jwtSecretData) {
        // Find the active key
        long now = System.currentTimeMillis();
        String currentKeyId = getSigningKeyId(jwtSecretData, now);
        signingKey = keyMap.get(currentKeyId);

        // Find the next key
        List<JwtSecret> futureJwtSecrets = getFutureJwtSecrets(jwtSecretData, now);

        // Set up rotation
        if (!futureJwtSecrets.isEmpty()) {
            JwtSecret jwtSecret = futureJwtSecrets.get(0);
            checkKeyRotation = true;
            nextRotationTs = jwtSecret.getEffectiveTs();
            nextKeyId = jwtSecret.getId();
        } else {
            checkKeyRotation = false;
        }
    }

    /**
     * Get future signing keys i.e. keys with effectiveTs > now.
     * @param jwtSecretData JWT config
     * @param now Timestamp of now
     * @return Future signing keys
     */
    protected List<JwtSecret> getFutureJwtSecrets(JwtSecretData jwtSecretData, long now) {
        return jwtSecretData.getJwtSecrets().stream()
                .filter(secretData -> secretData.getEffectiveTs() > now)
                .sorted((secretData1, secretData2) ->
                        secretData1.getEffectiveTs() - secretData2.getEffectiveTs() < 0 ? -1 : 1)
                // this puts older keys in the front of the list
                .collect(Collectors.toList());
    }

    /**
     * Get the ID of signing key that should be used to sign JWT. The signing key is defined as the "newest active key"
     * i.e. key with the biggest effectiveTs value and effectiveTs <= now.
     * @param jwtSecretData JWT config
     * @param now Timestamp of now in millisecond
     * @return ID of the signing key
     */
    protected String getSigningKeyId(JwtSecretData jwtSecretData, long now) {
        List<JwtSecret> sortedJwtSecrets = jwtSecretData.getJwtSecrets().stream()
                .filter(secretData -> secretData.getEffectiveTs() <= now)
                .sorted((secretData1, secretData2) -> secretData1.getEffectiveTs() - secretData2.getEffectiveTs() > 0 ? -1 : 1) // this puts newer keys in the front of the list
                .collect(Collectors.toList());
        String currentKeyId = sortedJwtSecrets.get(0).getId();
        return currentKeyId;
    }

    private void rotateKeyMap(JwtSecretData jwtSecretData) {
        ConcurrentHashMap<String, CerberusJwtKeySpec> keyMap = new ConcurrentHashMap<>();
        for (JwtSecret jwtSecret: jwtSecretData.getJwtSecrets()) {
            CerberusJwtKeySpec keySpec = new CerberusJwtKeySpec(
                    Base64.getDecoder().decode(jwtSecret.getSecret()),
                    DEFAULT_ALGORITHM,
                    jwtSecret.getId());
            keyMap.put(jwtSecret.getId(), keySpec);
        }
        this.keyMap = keyMap;
    }

    private void rotateKeyMap(CerberusJwtKeySpec cerberusJwtKeySpec) {
        ConcurrentHashMap<String, CerberusJwtKeySpec> keyMap = new ConcurrentHashMap<>();
        keyMap.put(cerberusJwtKeySpec.getKid(), cerberusJwtKeySpec);
        this.keyMap = keyMap;
    }

    private Key lookupVerificationKey(String keyId){
        if (StringUtils.isBlank(keyId)) {
            throw new IllegalArgumentException("Key ID cannot be empty");
        }
        try {
            CerberusJwtKeySpec keySpec = keyMap.get(keyId);
            if (keySpec == null) {
                throw new IllegalArgumentException("The key ID " + keyId + " is invalid or expired");
            }

            return keySpec;
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("The key ID " + keyId + " is either invalid or expired");
        }
    }

    private void rotateSigningKey() {
        long now = System.currentTimeMillis();
        if (now >= nextRotationTs) {
            this.signingKey = keyMap.get(nextKeyId);
        }
        checkKeyRotation = false;
    }
}