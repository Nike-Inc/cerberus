/*
 * Copyright (c) 2019 Nike, Inc.
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
import com.nike.cerberus.dao.JwtBlacklistDao;
import com.nike.cerberus.jwt.CerberusJwtClaims;
import com.nike.cerberus.jwt.CerberusJwtKeySpec;
import com.nike.cerberus.jwt.CerberusSigningKeyResolver;
import com.nike.cerberus.record.JwtBlacklistRecord;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;

import static io.jsonwebtoken.JwtParser.SEPARATOR_CHAR;
import static org.mybatis.guice.transactional.Isolation.READ_UNCOMMITTED;

/**
 * Service for generating, parsing, and validating JWT tokens.
 */
@Singleton
public class JwtService {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final static String PRINCIPAL_TYPE_CLAIM_NAME = "principalType";
    private final static String GROUP_CLAIM_NAME = "groups";
    private final static String IS_ADMIN_CLAIM_NAME = "isAdmin";
    private final static String REFRESH_COUNT_CLAIM_NAME = "refreshCount";

    private final CerberusSigningKeyResolver signingKeyResolver;
    private final String environmentName;
    private final JwtBlacklistDao jwtBlacklistDao;

    private HashSet<String> blacklist;

    @Inject
    public JwtService(CerberusSigningKeyResolver signingKeyResolver,
                      @Named("cms.env.name") String environmentName,
                      JwtBlacklistDao jwtBlacklistDao) {
        this.signingKeyResolver = signingKeyResolver;
        this.environmentName = environmentName;
        this.jwtBlacklistDao = jwtBlacklistDao;
        refreshBlacklist();
    }

    /**
     * Generate JWT token
     * @param cerberusJwtClaims Cerberus JWT claims
     * @return JWT token
     */
    public String generateJwtToken(CerberusJwtClaims cerberusJwtClaims){
        CerberusJwtKeySpec cerberusJwtKeySpec = signingKeyResolver.resolveSigningKey();
        String jwtToken = Jwts.builder()
                .setHeaderParam(JwsHeader.KEY_ID, cerberusJwtKeySpec.getKid())
                .setId(cerberusJwtClaims.getId())
                .setIssuer(environmentName)
                .setSubject(cerberusJwtClaims.getPrincipal())
                .claim(PRINCIPAL_TYPE_CLAIM_NAME, cerberusJwtClaims.getPrincipalType())
                .claim(GROUP_CLAIM_NAME, cerberusJwtClaims.getGroups())
                .claim(IS_ADMIN_CLAIM_NAME, cerberusJwtClaims.getIsAdmin())
                .claim(REFRESH_COUNT_CLAIM_NAME, cerberusJwtClaims.getRefreshCount())
                .setExpiration(Date.from(cerberusJwtClaims.getExpiresTs().toInstant()))
                .setIssuedAt(Date.from(cerberusJwtClaims.getCreatedTs().toInstant()))
                .signWith(cerberusJwtKeySpec)
                .compact();
        return jwtToken;
    }

    /**
     * Parse and validate JWT token
     * @param token JWT token
     * @return Cerberus JWT claims
     */
    public Optional<CerberusJwtClaims> parseAndValidateToken(String token) {
        Jws<Claims> claimsJws;
        try {
            claimsJws = Jwts.parser()
                    .requireIssuer(environmentName)
                    .setSigningKeyResolver(signingKeyResolver)
                    .parseClaimsJws(token);
        } catch (InvalidClaimException e) {
            log.warn("Invalid claim when parsing token: {}", token, e);
            return Optional.empty();
        } catch (JwtException e) {
            log.warn("Error parsing JWT token: {}", token, e);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            log.warn("Error parsing JWT token: {}", token, e);
            return Optional.empty();
        }
        Claims claims = claimsJws.getBody();
        if (blacklist.contains(claims.getId())) {
            log.warn("This JWT token is blacklisted. ID: {}", claims.getId());
            return Optional.empty();
        }
        String subject = claims.getSubject();
        CerberusJwtClaims cerberusJwtClaims = new CerberusJwtClaims()
                .setId(claims.getId())
                .setPrincipal(subject)
                .setExpiresTs(OffsetDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault()))
                .setCreatedTs(OffsetDateTime.ofInstant(claims.getIssuedAt().toInstant(), ZoneId.systemDefault()))
                .setPrincipalType(claims.get(PRINCIPAL_TYPE_CLAIM_NAME, String.class))
                .setGroups(claims.get(GROUP_CLAIM_NAME, String.class))
                .setIsAdmin(claims.get(IS_ADMIN_CLAIM_NAME, Boolean.class))
                .setRefreshCount(claims.get(REFRESH_COUNT_CLAIM_NAME, Integer.class));

        return Optional.of(cerberusJwtClaims);
    }

    /**
     * Refresh signing keys in {@link CerberusSigningKeyResolver}
     */
    public void refreshKeys() {
        signingKeyResolver.refresh();
    }

    /**
     * Refresh JWT blacklist
     */
    public void refreshBlacklist() {
        blacklist = jwtBlacklistDao.getBlacklist();
    }

    /**
     * Revoke JWT
     * @param id JWT ID
     * @param tokenExpires Expiration timestamp of the JWT
     */
    public void revokeToken(String id, OffsetDateTime tokenExpires) {
        blacklist.add(id);
        JwtBlacklistRecord jwtBlacklistRecord = new JwtBlacklistRecord()
                .setId(id)
                .setExpiresTs(tokenExpires);
        jwtBlacklistDao.addToBlacklist(jwtBlacklistRecord);
    }

    /**
     * Delete JWT blacklist entries that have expired
     * @return
     */
    @Transactional(
            isolation = READ_UNCOMMITTED // allow dirty reads so we don't block other threads
    )
    public int deleteExpiredTokens() {
        return jwtBlacklistDao.deleteExpiredTokens();
    }

    /**
     * Return if the token looks like a JWT. Technically a JWT can have one dot but we don't allow it here.
     * @param token The token to examine
     * @return Does the token look like a JWT
     */
    public boolean isJwt(String token) {
        return StringUtils.countMatches(token, SEPARATOR_CHAR) == 2;
    }
}