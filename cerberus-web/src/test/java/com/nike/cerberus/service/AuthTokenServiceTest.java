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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.nike.cerberus.PrincipalType;
import com.nike.cerberus.dao.AuthTokenDao;
import com.nike.cerberus.domain.AuthTokenAcceptType;
import com.nike.cerberus.domain.AuthTokenIssueType;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.jwt.CerberusJwtClaims;
import com.nike.cerberus.record.AuthTokenRecord;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.util.AuthTokenGenerator;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.TokenHasher;
import com.nike.cerberus.util.UuidSupplier;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import junit.framework.AssertionFailedError;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

public class AuthTokenServiceTest {

  @Mock private UuidSupplier uuidSupplier;
  @Mock private TokenHasher tokenHasher;
  @Mock private AuthTokenGenerator authTokenGenerator;
  @Mock private AuthTokenDao authTokenDao;
  @Mock private DateTimeSupplier dateTimeSupplier;
  @Mock private JwtService jwtService;
  @Mock private JwtFeatureFlags tokenFlag;
  @Mock private CerberusPrincipal cerberusPrincipal;

  AuthTokenService authTokenService;

  @Before
  public void before() {
    initMocks(this);

    authTokenService =
        new AuthTokenService(
            uuidSupplier,
            tokenHasher,
            authTokenGenerator,
            authTokenDao,
            dateTimeSupplier,
            jwtService,
            tokenFlag);

    when(tokenFlag.getAcceptType()).thenReturn(AuthTokenAcceptType.ALL);
  }

  @Test
  public void
      test_that_generateToken_attempts_to_write_a_hashed_record_and_returns_proper_object_with_unhashed_token() {
    String id = UUID.randomUUID().toString();
    String expectedTokenId = "abc-123-def-456";
    OffsetDateTime now = OffsetDateTime.now();
    final String fakeHash = "kjadlkfjasdlkf;jlkj1243asdfasdf";
    String principal = "test-user@domain.com";
    String groups = "group1,group2,group3";
    when(tokenFlag.getIssueType()).thenReturn(AuthTokenIssueType.SESSION);
    when(uuidSupplier.get()).thenReturn(id);

    when(authTokenGenerator.generateSecureToken()).thenReturn(expectedTokenId);
    when(dateTimeSupplier.get()).thenReturn(now);
    when(tokenHasher.hashToken(expectedTokenId)).thenReturn(fakeHash);

    CerberusAuthToken token =
        authTokenService.generateToken(principal, PrincipalType.USER, false, groups, 5, 0);

    assertEquals(
        "The token should have the un-hashed value returned", expectedTokenId, token.getToken());
    assertEquals("The token should have a created date of now", now, token.getCreated());
    assertEquals(
        "The token should expire ttl minutes after now", now.plusMinutes(5), token.getExpires());
    assertEquals("The token should have the proper principal", principal, token.getPrincipal());
    assertEquals(
        "The token should be the principal type that was passed in",
        PrincipalType.USER,
        token.getPrincipalType());
    assertEquals("The token should not have access to admin endpoints", false, token.isAdmin());
    assertEquals(
        "The token should have the groups that where passed in", groups, token.getGroups());
    assertEquals(
        "The newly created token should have a refresh count of 0", 0, token.getRefreshCount());

    // TODO: does this need to be fixed?

    verify(authTokenDao)
        .createAuthToken(
            argThat(
                new ArgumentMatcher<AuthTokenRecord>() {
                  @Override
                  public boolean matches(Object argument) {
                    return ((AuthTokenRecord) argument).getTokenHash().equals(fakeHash);
                  }
                }));
  }

  @Test
  public void test_that_generateToken_attempts_to_write_a_jwt_and_returns_proper_object() {
    String id = UUID.randomUUID().toString();
    String expectedTokenId = "abc-123-def-456";
    OffsetDateTime now = OffsetDateTime.now();
    //    final String fakeHash = "kjadlkfjasdlkf;jlkj1243asdfasdf";
    String principal = "test-user@domain.com";
    String groups = "group1,group2,group3";
    when(tokenFlag.getIssueType()).thenReturn(AuthTokenIssueType.JWT);
    when(uuidSupplier.get()).thenReturn(id);
    when(jwtService.generateJwtToken(any())).thenReturn(expectedTokenId);

    when(dateTimeSupplier.get()).thenReturn(now);
    //    when(tokenHasher.hashToken(expectedTokenId)).thenReturn(fakeHash);

    CerberusAuthToken token =
        authTokenService.generateToken(principal, PrincipalType.USER, false, groups, 5, 0);

    assertEquals(
        "The token should have the un-hashed value returned", expectedTokenId, token.getToken());
    assertEquals("The token should have a created date of now", now, token.getCreated());
    assertEquals(
        "The token should expire ttl minutes after now", now.plusMinutes(5), token.getExpires());
    assertEquals("The token should have the proper principal", principal, token.getPrincipal());
    assertEquals(
        "The token should be the principal type that was passed in",
        PrincipalType.USER,
        token.getPrincipalType());
    assertEquals("The token should not have access to admin endpoints", false, token.isAdmin());
    assertEquals(
        "The token should have the groups that where passed in", groups, token.getGroups());
    assertEquals(
        "The newly created token should have a refresh count of 0", 0, token.getRefreshCount());

    // TODO: does this need to be fixed?

    //    verify(authTokenDao)
    //        .createAuthToken(
    //            argThat(
    //                new ArgumentMatcher<AuthTokenRecord>() {
    //                  @Override
    //                  public boolean matches(Object argument) {
    //                    return ((AuthTokenRecord) argument).getTokenHash().equals(fakeHash);
    //                  }
    //                }));
  }

  @Test
  public void test_that_getCerberusAuthToken_returns_empty_if_token_not_present() {
    final String tokenId = "abc-123-def-456";
    final String fakeHash = "kjadlkfjasdlkf;jlkj1243asdfasdf";

    //    when(tokenFlag.getIssueType()).thenReturn(AuthTokenType.SESSION);
    //    when(tokenFlag.getAcceptType()).thenReturn(AuthTokenType.ALL);
    when(tokenHasher.hashToken(tokenId)).thenReturn(fakeHash);
    when(authTokenDao.getAuthTokenFromHash(fakeHash)).thenReturn(Optional.empty());

    Optional<CerberusAuthToken> tokenOptional = authTokenService.getCerberusAuthToken(tokenId);
    assertTrue("optional should be empty", !tokenOptional.isPresent());
  }

  @Test
  public void test_that_getCerberusAuthToken_returns_empty_if_JWT_not_present() {
    final String tokenId = "abc.123.def";
    //    final String fakeHash = "kjadlkfjasdlkf;jlkj1243asdfasdf";

    //    when(tokenFlag.getIssueType()).thenReturn(AuthTokenType.JWT);
    when(jwtService.isJwt(tokenId)).thenReturn(true);
    when(jwtService.parseAndValidateToken(tokenId)).thenReturn(Optional.empty());
    //    when(tokenHasher.hashToken(tokenId)).thenReturn(fakeHash);
    //    when(authTokenDao.getAuthTokenFromHash(fakeHash)).thenReturn(Optional.empty());

    Optional<CerberusAuthToken> tokenOptional = authTokenService.getCerberusAuthToken(tokenId);
    assertTrue("optional should be empty", !tokenOptional.isPresent());
  }

  @Test
  public void test_that_when_a_token_is_expired_empty_is_returned_session() {
    final String tokenId = "abc-123-def-456";
    final String fakeHash = "kjadlkfjasdlkf;jlkj1243asdfasdf";

    when(tokenHasher.hashToken(tokenId)).thenReturn(fakeHash);
    when(authTokenDao.getAuthTokenFromHash(fakeHash))
        .thenReturn(
            Optional.of(new AuthTokenRecord().setExpiresTs(OffsetDateTime.now().minusHours(1))));

    Optional<CerberusAuthToken> tokenOptional = authTokenService.getCerberusAuthToken(tokenId);
    assertTrue("optional should be empty", !tokenOptional.isPresent());
  }

  @Test
  public void test_that_when_a_token_is_expired_empty_is_returned_jwt() {
    final String tokenId = "abc.123.def";
    //    final String fakeHash = "kjadlkfjasdlkf;jlkj1243asdfasdf";

    when(jwtService.isJwt(tokenId)).thenReturn(true);
    when(jwtService.parseAndValidateToken(tokenId))
        .thenReturn(
            Optional.of(new CerberusJwtClaims().setExpiresTs(OffsetDateTime.now().minusHours(1))));
    //    when(tokenHasher.hashToken(tokenId)).thenReturn(fakeHash);
    //    when(authTokenDao.getAuthTokenFromHash(fakeHash))
    //        .thenReturn(
    //            Optional.of(new
    // AuthTokenRecord().setExpiresTs(OffsetDateTime.now().minusHours(1))));

    Optional<CerberusAuthToken> tokenOptional = authTokenService.getCerberusAuthToken(tokenId);
    assertTrue("optional should be empty", !tokenOptional.isPresent());
  }

  @Test
  public void
      test_that_when_a_valid_non_expired_token_record_is_present_the_optional_is_populated_with_valid_token_object_session() {
    String id = UUID.randomUUID().toString();
    String tokenId = "abc-123-def-456";
    OffsetDateTime now = OffsetDateTime.now();
    final String fakeHash = "kjadlkfjasdlkf;jlkj1243asdfasdf";
    String principal = "test-user@domain.com";
    String groups = "group1,group2,group3";

    when(tokenHasher.hashToken(tokenId)).thenReturn(fakeHash);
    when(authTokenDao.getAuthTokenFromHash(fakeHash))
        .thenReturn(
            Optional.of(
                new AuthTokenRecord()
                    .setId(id)
                    .setTokenHash(fakeHash)
                    .setCreatedTs(now)
                    .setExpiresTs(now.plusHours(1))
                    .setPrincipal(principal)
                    .setPrincipalType(PrincipalType.USER.getName())
                    .setIsAdmin(false)
                    .setGroups(groups)
                    .setRefreshCount(0)));

    Optional<CerberusAuthToken> tokenOptional = authTokenService.getCerberusAuthToken(tokenId);

    CerberusAuthToken token =
        tokenOptional.orElseThrow(() -> new AssertionFailedError("Token should be present"));
    assertEquals(tokenId, token.getToken());
    assertEquals(now, token.getCreated());
    assertEquals(now.plusHours(1), token.getExpires());
    assertEquals(principal, token.getPrincipal());
    assertEquals(PrincipalType.USER, token.getPrincipalType());
    assertEquals(false, token.isAdmin());
    assertEquals(groups, token.getGroups());
    assertEquals(0, token.getRefreshCount());
  }

  @Test
  public void
      test_that_when_a_valid_non_expired_token_record_is_present_the_optional_is_populated_with_valid_token_object_jwt() {
    String id = UUID.randomUUID().toString();
    String tokenId = "abc.123.def";
    OffsetDateTime now = OffsetDateTime.now();
    //    final String fakeHash = "kjadlkfjasdlkf;jlkj1243asdfasdf";
    String principal = "test-user@domain.com";
    String groups = "group1,group2,group3";

    when(jwtService.isJwt(tokenId)).thenReturn(true);
    when(jwtService.parseAndValidateToken(tokenId))
        .thenReturn(
            Optional.of(
                new CerberusJwtClaims()
                    .setId(id)
                    .setCreatedTs(now)
                    .setExpiresTs(now.plusHours(1))
                    .setPrincipal(principal)
                    .setPrincipalType(PrincipalType.USER.getName())
                    .setIsAdmin(false)
                    .setGroups(groups)
                    .setRefreshCount(0)));

    Optional<CerberusAuthToken> tokenOptional = authTokenService.getCerberusAuthToken(tokenId);

    CerberusAuthToken token =
        tokenOptional.orElseThrow(() -> new AssertionFailedError("Token should be present"));
    assertEquals(tokenId, token.getToken());
    assertEquals(now, token.getCreated());
    assertEquals(now.plusHours(1), token.getExpires());
    assertEquals(principal, token.getPrincipal());
    assertEquals(PrincipalType.USER, token.getPrincipalType());
    assertEquals(false, token.isAdmin());
    assertEquals(groups, token.getGroups());
    assertEquals(0, token.getRefreshCount());
  }

  //   TODO: does this need to be updated to use jwt?

  @Test
  public void test_that_revokeToken_calls_the_dao_with_the_hashed_token() {
    final String tokenId = "abc-123-def-456";
    final String fakeHash = "kjadlkfjasdlkf;jlkj1243asdfasdf";
    OffsetDateTime now = OffsetDateTime.now();
    when(tokenHasher.hashToken(tokenId)).thenReturn(fakeHash);
    when(cerberusPrincipal.getToken()).thenReturn(tokenId);
    authTokenService.revokeToken(cerberusPrincipal, now);
    verify(authTokenDao).deleteAuthTokenFromHash(fakeHash);
  }

  @Test
  public void test_that_revokeToken_calls_the_jwt_revoke_token() {
    String tokenId = "abcdef";
    String token = "abc.123.def";
    OffsetDateTime now = OffsetDateTime.now();
    when(jwtService.isJwt(token)).thenReturn(true);
    when(cerberusPrincipal.getTokenId()).thenReturn(tokenId);
    when(cerberusPrincipal.getToken()).thenReturn(token);
    authTokenService.revokeToken(cerberusPrincipal, now);
    verify(jwtService).revokeToken(tokenId, now);
  }

  @Test
  public void test_that_deleteExpiredTokens_directly_proxies_dao() {
    int maxDelete = 1;
    int batchSize = 2;
    int batchPauseTimeInMillis = 3;

    authTokenService.deleteExpiredTokens(maxDelete, batchSize, batchPauseTimeInMillis);
    verify(authTokenDao).deleteExpiredTokens(maxDelete, batchSize, batchPauseTimeInMillis);
  }
}
