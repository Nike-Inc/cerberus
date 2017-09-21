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
import com.nike.cerberus.PrincipalType;
import com.nike.cerberus.dao.AuthTokenDao;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.record.AuthTokenRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.UuidSupplier;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.OffsetDateTime;

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
        String token = uuidSupplier.get();
        OffsetDateTime now = dateTimeSupplier.get();

        AuthTokenRecord tokenRecord = new AuthTokenRecord()
                .setId(id)
                .setTokenHash(getHashFromToken(token))
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
     * @param token The token to hash
     * @return The hashed token
     */
    private String getHashFromToken(String token) {
        // TODO IMPLEMENT
//        throw new RuntimeException("NOT IMPLEMENTED");
        return token;
    }

    public CerberusAuthToken getCerberusAuthToken(String token) {
        AuthTokenRecord tokenRecord = authTokenDao.getAuthTokenFromHash(getHashFromToken(token));
        return getCerberusAuthTokenFromRecord(token, tokenRecord);
    }
}
