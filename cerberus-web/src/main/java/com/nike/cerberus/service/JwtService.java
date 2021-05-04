/*
 * Copyright (c) 2021 Nike, Inc.
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

import static io.jsonwebtoken.JwtParser.SEPARATOR_CHAR;
import static org.springframework.transaction.annotation.Isolation.READ_UNCOMMITTED;

import com.nike.cerberus.dao.JwtBlocklistDao;
import com.nike.cerberus.error.AuthTokenTooLongException;
import com.nike.cerberus.jwt.CerberusJwtClaims;
import com.nike.cerberus.jwt.CerberusJwtKeySpec;
import com.nike.cerberus.jwt.CerberusSigningKeyResolver;
import com.nike.cerberus.record.JwtBlocklistRecord;
import io.jsonwebtoken.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Service for generating, parsing, and validating JWT tokens. */
@Component
@ComponentScan(basePackages = {"com.nike.cerberus.jwt", "com.nike.cerberus.dao"})
public class JwtService {

  protected final Logger log = LoggerFactory.getLogger(getClass());
  private static final String PRINCIPAL_TYPE_CLAIM_NAME = "principalType";
  private static final String GROUP_CLAIM_NAME = "groups";
  private static final String IS_ADMIN_CLAIM_NAME = "isAdmin";
  private static final String REFRESH_COUNT_CLAIM_NAME = "refreshCount";
  private static final int NUM_SEPARATORS_IN_JWT_TOKEN = 2;

  // Max header line length for an AWS ALB is 16k, so it needs to be less
  @Value("${cerberus.auth.jwt.maxTokenLength:#{16000}}")
  private int maxTokenLength;

  private final CerberusSigningKeyResolver signingKeyResolver;
  private final String environmentName;
  private final JwtBlocklistDao jwtBlocklistDao;

  private HashSet<String> blocklist;

  @Autowired
  public JwtService(
      CerberusSigningKeyResolver signingKeyResolver,
      @Value("${cerberus.environmentName}") String environmentName,
      JwtBlocklistDao jwtBlocklistDao) {
    this.signingKeyResolver = signingKeyResolver;
    this.environmentName = environmentName;
    this.jwtBlocklistDao = jwtBlocklistDao;
    refreshBlocklist();
  }

  /**
   * Generate JWT token
   *
   * @param cerberusJwtClaims Cerberus JWT claims
   * @return JWT token
   */
  public String generateJwtToken(CerberusJwtClaims cerberusJwtClaims)
      throws AuthTokenTooLongException {
    CerberusJwtKeySpec cerberusJwtKeySpec = signingKeyResolver.resolveSigningKey();
    String principal = cerberusJwtClaims.getPrincipal();

    String jwtToken =
        Jwts.builder()
            .setHeaderParam(JwsHeader.KEY_ID, cerberusJwtKeySpec.getKid())
            .setId(cerberusJwtClaims.getId())
            .setIssuer(environmentName)
            .setSubject(principal)
            .claim(PRINCIPAL_TYPE_CLAIM_NAME, cerberusJwtClaims.getPrincipalType())
            .claim(GROUP_CLAIM_NAME, cerberusJwtClaims.getGroups())
            .claim(IS_ADMIN_CLAIM_NAME, cerberusJwtClaims.getIsAdmin())
            .claim(REFRESH_COUNT_CLAIM_NAME, cerberusJwtClaims.getRefreshCount())
            .setExpiration(Date.from(cerberusJwtClaims.getExpiresTs().toInstant()))
            .setIssuedAt(Date.from(cerberusJwtClaims.getCreatedTs().toInstant()))
            .signWith(cerberusJwtKeySpec)
            .compressWith(CompressionCodecs.GZIP)
            .compact();

    int tokenLength = jwtToken.length();
    log.info("{}: JWT length: {}", principal, tokenLength);
    if (tokenLength > maxTokenLength) {
      String msg =
          String.format(
              "Token for %s is %d characters long. The max is %d bytes.",
              principal, tokenLength, maxTokenLength);
      throw new AuthTokenTooLongException(msg);
    }
    return jwtToken;
  }

  /**
   * Parse and validate JWT token
   *
   * @param token JWT token
   * @return Cerberus JWT claims
   */
  public Optional<CerberusJwtClaims> parseAndValidateToken(String token) {
    Jws<Claims> claimsJws;
    try {
      claimsJws =
          Jwts.parser()
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
    if (blocklist.contains(claims.getId())) {
      log.warn("This JWT token is blocklisted. ID: {}", claims.getId());
      return Optional.empty();
    }
    String subject = claims.getSubject();
    CerberusJwtClaims cerberusJwtClaims =
        CerberusJwtClaims.builder()
            .id(claims.getId())
            .principal(subject)
            .expiresTs(
                OffsetDateTime.ofInstant(
                    claims.getExpiration().toInstant(), ZoneId.systemDefault()))
            .createdTs(
                OffsetDateTime.ofInstant(claims.getIssuedAt().toInstant(), ZoneId.systemDefault()))
            .principalType(claims.get(PRINCIPAL_TYPE_CLAIM_NAME, String.class))
            .groups(claims.get(GROUP_CLAIM_NAME, String.class))
            .isAdmin(claims.get(IS_ADMIN_CLAIM_NAME, Boolean.class))
            .refreshCount(claims.get(REFRESH_COUNT_CLAIM_NAME, Integer.class))
            .build();

    return Optional.of(cerberusJwtClaims);
  }

  /** Refresh signing keys in {@link CerberusSigningKeyResolver} */
  public void refreshKeys() {
    signingKeyResolver.refresh();
  }

  /** Refresh JWT blocklist */
  public void refreshBlocklist() {
    blocklist = jwtBlocklistDao.getBlocklist();
  }

  /**
   * Revoke JWT
   *
   * @param id JWT ID
   * @param tokenExpires Expiration timestamp of the JWT
   */
  public void revokeToken(String id, OffsetDateTime tokenExpires) {
    blocklist.add(id);
    JwtBlocklistRecord jwtBlocklistRecord =
        new JwtBlocklistRecord().setId(id).setExpiresTs(tokenExpires);
    jwtBlocklistDao.addToBlocklist(jwtBlocklistRecord);
  }

  /**
   * Delete JWT blocklist entries that have expired
   *
   * @return
   */
  @Transactional(
      isolation = READ_UNCOMMITTED // allow dirty reads so we don't block other threads
      )
  public int deleteExpiredTokens() {
    return jwtBlocklistDao.deleteExpiredTokens();
  }

  /**
   * Return if the token looks like a JWT. Technically a JWT can have one dot but we don't allow it
   * here.
   *
   * @param token The token to examine
   * @return Does the token look like a JWT
   */
  public boolean isJwt(String token) {
    return StringUtils.countMatches(token, SEPARATOR_CHAR) == NUM_SEPARATORS_IN_JWT_TOKEN;
  }
}
