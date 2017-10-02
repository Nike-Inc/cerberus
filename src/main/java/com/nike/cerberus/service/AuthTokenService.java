/*
 * Copyright (c) 2017 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.service;

import com.google.inject.name.Named;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.PrincipalType;
import com.nike.cerberus.dao.AuthTokenDao;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.AuthTokenRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.RandomString;
import com.nike.cerberus.util.UuidSupplier;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Service for handling authentication tokens.
 */
public class AuthTokenService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String HASH_SALT = "auth.token.hashSalt";

    private final UuidSupplier uuidSupplier;
    private final String hashSalt;
    private final AuthTokenDao authTokenDao;
    private final DateTimeSupplier dateTimeSupplier;

    @Inject
    public AuthTokenService(UuidSupplier uuidSupplier,
                            @Named(HASH_SALT) String hashSalt,
                            AuthTokenDao authTokenDao,
                            DateTimeSupplier dateTimeSupplier) {

        this.uuidSupplier = uuidSupplier;
        this.hashSalt = hashSalt;
        this.authTokenDao = authTokenDao;
        this.dateTimeSupplier = dateTimeSupplier;
    }

    @Transactional
    public CerberusAuthToken generateToken(String principal,
                                           PrincipalType principalType,
                                           boolean isAdmin,
                                           String groups,
                                           long ttlInMinutes,
                                           int refreshCount) {

        checkArgument(StringUtils.isNotBlank(principal), "The principal must be set and not empty");

        String id = uuidSupplier.get();
        String token = new RandomString().nextString();
        OffsetDateTime now = dateTimeSupplier.get();

        AuthTokenRecord tokenRecord = new AuthTokenRecord()
                .setId(id)
                .setTokenHash(hashToken(token))
                .setCreatedTs(now)
                .setExpiresTs(now.plusMinutes(ttlInMinutes))
                .setPrincipal(principal)
                .setPrincipalType(principalType.getName())
                .setIsAdmin(isAdmin)
                .setGroups(groups)
                .setRefreshCount(refreshCount);

        authTokenDao.createAuthToken(tokenRecord);

        return getCerberusAuthTokenFromRecord(token, tokenRecord);
    }

    private CerberusAuthToken getCerberusAuthTokenFromRecord(String token, AuthTokenRecord tokenRecord) {
        return new CerberusAuthToken.CerberusAuthTokenBuilder()
                .withToken(token)
                .withCreated(tokenRecord.getCreatedTs())
                .withExpires(tokenRecord.getExpiresTs())
                .withPrincipal(tokenRecord.getPrincipal())
                .withPrincipalType(PrincipalType.fromName(tokenRecord.getPrincipalType()))
                .withIsAdmin(tokenRecord.getIsAdmin())
                .withGroups(tokenRecord.getGroups())
                .withRefreshCount(tokenRecord.getRefreshCount())
                .build();
    }

    /**
     * https://www.owasp.org/index.php/Hashing_Java
     *
     * @param token The token to hash
     * @return The hashed token
     */
    private String hashToken(String token) {
        int keyLength = 256;

        // https://stackoverflow.com/questions/29431884/java-security-pbekeyspec-how-many-iterations-are-enough
        int iterations = 100;
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            PBEKeySpec spec = new PBEKeySpec(token.toCharArray(), hashSalt.getBytes(), iterations, keyLength);
            SecretKey key = skf.generateSecret(spec);
            return Hex.encodeHexString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("There was a problem hashing the token", e);
        }
    }

    public Optional<CerberusAuthToken> getCerberusAuthToken(String token) {
        Optional<AuthTokenRecord> tokenRecord = authTokenDao.getAuthTokenFromHash(hashToken(token));

        // TODO is there a bug here with daylight savings?
        OffsetDateTime now = OffsetDateTime.now();
        if (tokenRecord.isPresent() && tokenRecord.get().getExpiresTs().isBefore(now)) {
            logger.warn("Returning empty optional, because token was expired, expired: {}, now: {}", tokenRecord.get().getExpiresTs(), now);
            return Optional.empty();
        }

        return tokenRecord.map(authTokenRecord -> getCerberusAuthTokenFromRecord(token, authTokenRecord));
    }

    @Transactional
    public void revokeToken(String token) {
        String hash = hashToken(token);
        authTokenDao.deleteAuthTokenFromHash(hash);
    }

    // TODO is there a bug here with daylight savings, probably
    @Transactional
    public int deleteExpiredTokens() {
        return authTokenDao.deleteExpiredTokens();
    }
}
