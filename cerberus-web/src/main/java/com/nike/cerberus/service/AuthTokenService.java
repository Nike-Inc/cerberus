/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.service;

import static com.google.common.base.Preconditions.checkArgument;
import static org.springframework.transaction.annotation.Isolation.READ_UNCOMMITTED;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.PrincipalType;
import com.nike.cerberus.dao.AuthTokenDao;
import com.nike.cerberus.domain.AuthTokenAcceptType;
import com.nike.cerberus.domain.AuthTokenInfo;
import com.nike.cerberus.domain.AuthTokenIssueType;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.error.AuthTokenTooLongException;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.jwt.CerberusJwtClaims;
import com.nike.cerberus.record.AuthTokenRecord;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.util.AuthTokenGenerator;
import com.nike.cerberus.util.CustomApiError;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.TokenHasher;
import com.nike.cerberus.util.UuidSupplier;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Data
@Component
@ConfigurationProperties("cerberus.auth.token")
class JwtFeatureFlags {
  private AuthTokenIssueType issueType;
  private AuthTokenAcceptType acceptType;
}

/** Service for handling authentication tokens. */
@Component
public class AuthTokenService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final UuidSupplier uuidSupplier;
  private final TokenHasher tokenHasher;
  private final AuthTokenGenerator authTokenGenerator;
  private final AuthTokenDao authTokenDao;
  private final DateTimeSupplier dateTimeSupplier;
  private final JwtService jwtService;

  private final JwtFeatureFlags tokenFlag;

  @Autowired
  public AuthTokenService(
      UuidSupplier uuidSupplier,
      TokenHasher tokenHasher,
      AuthTokenGenerator authTokenGenerator,
      AuthTokenDao authTokenDao,
      DateTimeSupplier dateTimeSupplier,
      JwtService jwtService,
      JwtFeatureFlags tokenFlag) {

    this.uuidSupplier = uuidSupplier;
    this.tokenHasher = tokenHasher;
    this.authTokenGenerator = authTokenGenerator;
    this.authTokenDao = authTokenDao;
    this.dateTimeSupplier = dateTimeSupplier;
    this.jwtService = jwtService;
    this.tokenFlag = tokenFlag;
  }

  public CerberusAuthToken generateToken(String principal,
                                         PrincipalType principalType,
                                         boolean isAdmin,
                                         String groups,
                                         long ttlInMinutes,
                                         int refreshCount) {

    checkArgument(StringUtils.isNotBlank(principal), "The principal must be set and not empty");

    String id = uuidSupplier.get();
    OffsetDateTime now = dateTimeSupplier.get();

    switch (tokenFlag.getIssueType()) {
      case JWT:
        try {
          return getCerberusAuthTokenFromJwt(
              principal, principalType, isAdmin, groups, ttlInMinutes, refreshCount, id, now);
        } catch (AuthTokenTooLongException e) {
          final String msg = e.getMessage();
          logger.info(msg);

          if (tokenFlag.getAcceptType() == AuthTokenAcceptType.ALL) {
            return getCerberusAuthTokenFromSession(
                principal, principalType, isAdmin, groups, ttlInMinutes, refreshCount, id, now);
          }
          throw ApiException.newBuilder()
              .withApiErrors(
                  CustomApiError.createCustomApiError(DefaultApiError.AUTH_TOKEN_TOO_LONG, msg))
              .withExceptionMessage(msg)
              .build();
        }
      case SESSION:
        return getCerberusAuthTokenFromSession(
            principal, principalType, isAdmin, groups, ttlInMinutes, refreshCount, id, now);
      default:
        throw ApiException.newBuilder()
            .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
            .build();
    }
  }

  private CerberusAuthToken getCerberusAuthTokenFromJwt(
      String principal,
      PrincipalType principalType,
      boolean isAdmin,
      String groups,
      long ttlInMinutes,
      int refreshCount,
      String id,
      OffsetDateTime now)
      throws AuthTokenTooLongException {

    AuthTokenInfo authTokenInfo;
    String token;

    authTokenInfo =
        new CerberusJwtClaims()
            .setId(id)
            .setCreatedTs(now)
            .setExpiresTs(now.plusMinutes(ttlInMinutes))
            .setPrincipal(principal)
            .setPrincipalType(principalType.getName())
            .setIsAdmin(isAdmin)
            .setGroups(groups)
            .setRefreshCount(refreshCount);
    token = jwtService.generateJwtToken((CerberusJwtClaims) authTokenInfo);
    return getCerberusAuthTokenFromRecord(token, authTokenInfo);
  }

  private CerberusAuthToken getCerberusAuthTokenFromSession(
      String principal,
      PrincipalType principalType,
      boolean isAdmin,
      String groups,
      long ttlInMinutes,
      int refreshCount,
      String id,
      OffsetDateTime now) {

    String token = authTokenGenerator.generateSecureToken();
    AuthTokenRecord authTokenRecord =
        new AuthTokenRecord()
            .setId(id)
            .setCreatedTs(now)
            .setExpiresTs(now.plusMinutes(ttlInMinutes))
            .setPrincipal(principal)
            .setPrincipalType(principalType.getName())
            .setIsAdmin(isAdmin)
            .setGroups(groups)
            .setRefreshCount(refreshCount);
    authTokenDao.createAuthToken(authTokenRecord);
    return getCerberusAuthTokenFromRecord(token, authTokenRecord);
  }

  private CerberusAuthToken getCerberusAuthTokenFromRecord(
      String token, AuthTokenInfo authTokenInfo) {
    return CerberusAuthToken.Builder.create()
        .withToken(token)
        .withCreated(authTokenInfo.getCreatedTs())
        .withExpires(authTokenInfo.getExpiresTs())
        .withPrincipal(authTokenInfo.getPrincipal())
        .withPrincipalType(PrincipalType.fromName(authTokenInfo.getPrincipalType()))
        .withIsAdmin(authTokenInfo.getIsAdmin())
        .withGroups(authTokenInfo.getGroups())
        .withRefreshCount(authTokenInfo.getRefreshCount())
        .withId(authTokenInfo.getId())
        .build();
  }

  public Optional<CerberusAuthToken> getCerberusAuthToken(String token) {
    Optional<? extends AuthTokenInfo> tokenRecord = Optional.empty();
    AuthTokenAcceptType acceptType = tokenFlag.getAcceptType();
    boolean isJwt = jwtService.isJwt(token);
    if (isJwt && (acceptType != AuthTokenAcceptType.SESSION)) {
      tokenRecord = jwtService.parseAndValidateToken(token);
    } else if (acceptType != AuthTokenAcceptType.JWT) {
      tokenRecord = authTokenDao.getAuthTokenFromHash(tokenHasher.hashToken(token));
    } else {
      String tokenType = isJwt ? "JWT" : "Session";
      logger.warn(
          "Returning empty optional, because token type is {} and only {} are accepted",
          tokenType,
          acceptType.toString());
      return Optional.empty();
    }

    OffsetDateTime now = OffsetDateTime.now();
    if (tokenRecord.isPresent() && tokenRecord.get().getExpiresTs().isBefore(now)) {
      logger.warn("Returning empty optional, because token was expired, expired: {}, now: {}", tokenRecord.get().getExpiresTs(), now);
      return Optional.empty();
    }

    return tokenRecord.map(authTokenRecord -> getCerberusAuthTokenFromRecord(token, authTokenRecord));
  }

//  @Transactional
//  public CerberusAuthToken generateToken(
//      String principal,
//      PrincipalType principalType,
//      boolean isAdmin,
//      String groups,
//      long ttlInMinutes,
//      int refreshCount) {
//
//    checkArgument(StringUtils.isNotBlank(principal), "The principal must be set and not empty");
//
//    String id = uuidSupplier.get();
//    String token = authTokenGenerator.generateSecureToken();
//    OffsetDateTime now = dateTimeSupplier.get();
//
//    AuthTokenRecord tokenRecord =
//        new AuthTokenRecord()
//            .setId(id)
//            .setTokenHash(tokenHasher.hashToken(token))
//            .setCreatedTs(now)
//            .setExpiresTs(now.plusMinutes(ttlInMinutes))
//            .setPrincipal(principal)
//            .setPrincipalType(principalType.getName())
//            .setIsAdmin(isAdmin)
//            .setGroups(groups)
//            .setRefreshCount(refreshCount);
//
//    authTokenDao.createAuthToken(tokenRecord);
//
//    return getCerberusAuthTokenFromRecord(token, tokenRecord);
//  }
//
//  private CerberusAuthToken getCerberusAuthTokenFromRecord(
//      String token, AuthTokenRecord tokenRecord) {
//    return CerberusAuthToken.builder()
//        .token(token)
//        .created(tokenRecord.getCreatedTs())
//        .expires(tokenRecord.getExpiresTs())
//        .principal(tokenRecord.getPrincipal())
//        .principalType(PrincipalType.fromName(tokenRecord.getPrincipalType()))
//        .isAdmin(tokenRecord.getIsAdmin())
//        .groups(tokenRecord.getGroups())
//        .refreshCount(tokenRecord.getRefreshCount())
//        .build();
//  }
//
//  public Optional<CerberusAuthToken> getCerberusAuthTokenWithJwt(String token) {
//    Optional<CerberusJwtClaim> jwtClaim = // get jwt claim with token (parseAndValidateToken)
//    return tokenRecord.map(
//            authTokenRecord -> getCerberusAuthTokenFromRecord(token, jwtClaim));
//  }
//
//  public Optional<CerberusAuthToken> getCerberusAuthToken(String token) {
//    Optional<AuthTokenRecord> tokenRecord =
//        authTokenDao.getAuthTokenFromHash(tokenHasher.hashToken(token));
//
//    OffsetDateTime now = OffsetDateTime.now();
//    if (tokenRecord.isPresent() && tokenRecord.get().getExpiresTs().isBefore(now)) {
//      logger.warn(
//          "Returning empty optional, because token was expired, expired: {}, now: {}",
//          tokenRecord.get().getExpiresTs(),
//          now);
//      return Optional.empty();
//    }
//
//    return tokenRecord.map(
//        authTokenRecord -> getCerberusAuthTokenFromRecord(token, authTokenRecord));
//  }

  @Transactional
  public void revokeToken(CerberusPrincipal cerberusPrincipal, OffsetDateTime tokenExpires) {
    if (jwtService.isJwt(cerberusPrincipal.getToken())) {
      logger.info("Revoking token ID: {}", cerberusPrincipal);
      jwtService.revokeToken(cerberusPrincipal.getTokenId(), tokenExpires);
    } else {
      String hash = tokenHasher.hashToken(cerberusPrincipal.getToken());
      authTokenDao.deleteAuthTokenFromHash(hash);
    }
  }

  @Transactional(
      isolation = READ_UNCOMMITTED // allow dirty reads so we don't block other threads
      //             = true // auto commit each batched / chunked delete TODO verify spring way
      )
  public int deleteExpiredTokens(int maxDelete, int batchSize, int batchPauseTimeInMillis) {
    return authTokenDao.deleteExpiredTokens(maxDelete, batchSize, batchPauseTimeInMillis);
  }
}
