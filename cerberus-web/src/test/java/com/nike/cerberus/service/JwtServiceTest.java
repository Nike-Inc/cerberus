package com.nike.cerberus.service;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Sets;
import com.nike.cerberus.dao.JwtBlocklistDao;
import com.nike.cerberus.jwt.CerberusJwtClaims;
import com.nike.cerberus.jwt.CerberusJwtKeySpec;
import com.nike.cerberus.jwt.CerberusSigningKeyResolver;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

public class JwtServiceTest {

  @Mock private CerberusSigningKeyResolver signingKeyResolver;

  @Mock private JwtBlocklistDao jwtBlocklistDao;

  private JwtService jwtService;

  private CerberusJwtKeySpec cerberusJwtKeySpec;

  private CerberusJwtClaims cerberusJwtClaims;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    jwtService = new JwtService(signingKeyResolver, "local", jwtBlocklistDao);
    ReflectionTestUtils.setField(jwtService, "maxTokenLength", 1600);
    cerberusJwtKeySpec = new CerberusJwtKeySpec(new byte[64], "HmacSHA512", "key id");
    cerberusJwtClaims = new CerberusJwtClaims();
    cerberusJwtClaims.setId("id");
    cerberusJwtClaims.setPrincipal("principal");
    cerberusJwtClaims.setGroups("groups");
    cerberusJwtClaims.setIsAdmin(true);
    cerberusJwtClaims.setPrincipalType("type");
    cerberusJwtClaims.setRefreshCount(1);
    cerberusJwtClaims.setCreatedTs(OffsetDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC));
    cerberusJwtClaims.setExpiresTs(
        OffsetDateTime.of(3000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC)); // should be good for a while

    when(signingKeyResolver.resolveSigningKey()).thenReturn(cerberusJwtKeySpec);
    when(signingKeyResolver.resolveSigningKey(any(JwsHeader.class), any(Claims.class)))
        .thenReturn(cerberusJwtKeySpec);
  }

  @Test
  public void test_generate_jwt_token_parse_and_validate_claim() {
    String token = jwtService.generateJwtToken(cerberusJwtClaims);
    assertEquals(3, token.split("\\.").length);
    Optional<CerberusJwtClaims> cerberusJwtClaimsOptional = jwtService.parseAndValidateToken(token);
    assertTrue(cerberusJwtClaimsOptional.isPresent());
    CerberusJwtClaims cerberusJwtClaims = cerberusJwtClaimsOptional.get();

    assertEquals("id", cerberusJwtClaims.getId());
    assertEquals("principal", cerberusJwtClaims.getPrincipal());
    assertEquals("groups", cerberusJwtClaims.getGroups());
    assertEquals(true, cerberusJwtClaims.getIsAdmin());
    assertEquals("type", cerberusJwtClaims.getPrincipalType());
    assertEquals(1, (long) cerberusJwtClaims.getRefreshCount());
    assertEquals(
        OffsetDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toEpochSecond(),
        cerberusJwtClaims.getCreatedTs().toEpochSecond());
    assertEquals(
        OffsetDateTime.of(3000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toEpochSecond(),
        cerberusJwtClaims.getExpiresTs().toEpochSecond());
  }

  @Test
  public void test_expired_token_returns_empty() {
    cerberusJwtClaims.setExpiresTs(OffsetDateTime.of(2000, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC));
    String token = jwtService.generateJwtToken(cerberusJwtClaims);
    Optional<CerberusJwtClaims> cerberusJwtClaims = jwtService.parseAndValidateToken(token);
    assertFalse(cerberusJwtClaims.isPresent());
  }

  @Test
  public void test_unsigned_token_returns_empty() {
    String token =
        "eyJraWQiOiJrZXkgaWQiLCJhbGciOiJIUzUxMiJ9.eyJqdGkiOiJpZCIsImlzcyI6ImxvY2FsIiwic3ViIjoicHJpbm"
            + "NpcGFsIiwicHJpbmNpcGFsVHlwZSI6InR5cGUiLCJncm91cHMiOiJncm91cHMiLCJpc0FkbWluIjp0cnVlLCJyZWZyZXNoQ291"
            + "bnQiOjEsImV4cCI6NDA3MDkxMjQ2MSwiaWF0Ijo5NDY2ODg0NjF9";
    Optional<CerberusJwtClaims> cerberusJwtClaims = jwtService.parseAndValidateToken(token);
    assertFalse(cerberusJwtClaims.isPresent());
  }

  @Test
  public void test_parseAndValidateToken_returns_empty_for_blocklisted_token() {
    String token = jwtService.generateJwtToken(cerberusJwtClaims);
    when(jwtBlocklistDao.getBlocklist()).thenReturn(Sets.newHashSet("id"));
    jwtService.refreshBlocklist();
    Optional<CerberusJwtClaims> cerberusJwtClaims = jwtService.parseAndValidateToken(token);
    assertFalse(cerberusJwtClaims.isPresent());
  }

  @Test
  public void test_that_revokeToken_calls_the_dao() {
    final String tokenId = "abc-123-def-456";
    jwtService.revokeToken(tokenId, OffsetDateTime.now());
    verify(jwtBlocklistDao, times(1)).addToBlocklist(any());
  }
}
