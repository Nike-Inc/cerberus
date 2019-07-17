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

import com.nike.cerberus.PrincipalType;
import com.nike.cerberus.dao.AuthTokenDao;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.record.AuthTokenRecord;
import com.nike.cerberus.util.AuthTokenGenerator;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.JwtUtils;
import com.nike.cerberus.util.TokenHasher;
import com.nike.cerberus.util.UuidSupplier;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static org.mybatis.guice.transactional.Isolation.READ_UNCOMMITTED;

/**
 * Service for handling authentication tokens.
 */
@Singleton
public class AuthTokenService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UuidSupplier uuidSupplier;
    private final TokenHasher tokenHasher;
    private final AuthTokenGenerator authTokenGenerator;
    private final AuthTokenDao authTokenDao;
    private final DateTimeSupplier dateTimeSupplier;
    private final JwtUtils jwtUtils;

    @Inject
    public AuthTokenService(UuidSupplier uuidSupplier,
                            TokenHasher tokenHasher,
                            AuthTokenGenerator authTokenGenerator,
                            AuthTokenDao authTokenDao,
                            DateTimeSupplier dateTimeSupplier, JwtUtils jwtUtils) {

        this.uuidSupplier = uuidSupplier;
        this.tokenHasher = tokenHasher;
        this.authTokenGenerator = authTokenGenerator;
        this.authTokenDao = authTokenDao;
        this.dateTimeSupplier = dateTimeSupplier;
        this.jwtUtils = jwtUtils;
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
        OffsetDateTime now = dateTimeSupplier.get();

        AuthTokenRecord tokenRecord = new AuthTokenRecord()
                .setId(id)
                .setCreatedTs(now)
                .setExpiresTs(now.plusMinutes(ttlInMinutes))
                .setPrincipal(principal)
                .setPrincipalType(principalType.getName())
                .setIsAdmin(isAdmin)
                .setGroups(groups)
                .setRefreshCount(refreshCount);
        String jwtToken = jwtUtils.generateJwtToken(tokenRecord);

        return getCerberusAuthTokenFromRecord(jwtToken, tokenRecord);
    }

    private CerberusAuthToken getCerberusAuthTokenFromRecord(String token, AuthTokenRecord tokenRecord) {
        return CerberusAuthToken.Builder.create()
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

    public Optional<CerberusAuthToken> getCerberusAuthToken(String token) {
        Optional<AuthTokenRecord> tokenRecord = jwtUtils.parseClaim(token);

        OffsetDateTime now = OffsetDateTime.now();
        if (tokenRecord.isPresent() && tokenRecord.get().getExpiresTs().isBefore(now)) {
            logger.warn("Returning empty optional, because token was expired, expired: {}, now: {}", tokenRecord.get().getExpiresTs(), now);
            return Optional.empty();
        }

        return tokenRecord.map(authTokenRecord -> getCerberusAuthTokenFromRecord(token, authTokenRecord));
    }

    @Transactional
    public void revokeToken(String token) {
        String hash = tokenHasher.hashToken(token);
        authTokenDao.deleteAuthTokenFromHash(hash);
    }

    @Transactional(
            isolation = READ_UNCOMMITTED, // allow dirty reads so we don't block other threads
            autoCommit = true // auto commit each batched / chunked delete
    )
    public int deleteExpiredTokens(int maxDelete, int batchSize, int batchPauseTimeInMillis) {
        return authTokenDao.deleteExpiredTokens(maxDelete, batchSize, batchPauseTimeInMillis);
    }
}
