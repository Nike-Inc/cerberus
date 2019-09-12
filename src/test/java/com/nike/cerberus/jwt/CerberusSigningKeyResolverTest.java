package com.nike.cerberus.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.service.ConfigService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.DefaultJwsHeader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.security.Key;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;



public class CerberusSigningKeyResolverTest {
    @Mock
    private CerberusSigningKeyResolver.JwtServiceOptionalPropertyHolder jwtServiceOptionalPropertyHolder;

    @Mock
    private ConfigService configService;

    @Mock
    private ObjectMapper objectMapper;

    private CerberusSigningKeyResolver cerberusSigningKeyResolver;

    private JwtSecretData jwtSecretData = new JwtSecretData();

    private String configStoreJwtSecretData;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        LinkedList<JwtSecret> jwtSecrets = new LinkedList<>();
        jwtSecretData.setJwtSecrets(jwtSecrets);

        JwtSecret jwtSecret1 = new JwtSecret();
        jwtSecret1.setCreatedTs(100);
        jwtSecret1.setEffectiveTs(200);
        jwtSecret1.setId("key id 1");
        jwtSecret1.setSecret("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==");
        jwtSecrets.add(jwtSecret1);

        JwtSecret jwtSecret2 = new JwtSecret();
        jwtSecret2.setCreatedTs(300);
        jwtSecret2.setEffectiveTs(400);
        jwtSecret2.setId("key id 2");
        jwtSecret2.setSecret("BAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==");
        jwtSecrets.add(jwtSecret2);

        JwtSecret jwtSecret3 = new JwtSecret();
        jwtSecret3.setCreatedTs(500);
        jwtSecret3.setEffectiveTs(600);
        jwtSecret3.setId("key id 3");
        jwtSecret3.setSecret("CAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==");
        jwtSecrets.add(jwtSecret3);

        when(objectMapper.readValue(anyString(), same(JwtSecretData.class))).thenReturn(jwtSecretData);
        when(configService.getJwtSecrets()).thenReturn(configStoreJwtSecretData);

        cerberusSigningKeyResolver = new CerberusSigningKeyResolver(
                jwtServiceOptionalPropertyHolder, configService, objectMapper);
    }

    @Test
    public void test_get_future_jwt_secrets() {
        List<JwtSecret> futureJwtSecrets = cerberusSigningKeyResolver.getFutureJwtSecrets(jwtSecretData, 300);
        assertEquals(2, futureJwtSecrets.size());
        assertEquals("key id 2", futureJwtSecrets.get(0).getId());
    }

    @Test
    public void test_get_current_key_id(){
        String keyId = cerberusSigningKeyResolver.getSigningKeyId(jwtSecretData, 700);
        assertEquals("key id 3", keyId);
    }

    @Test
    public void test_get_current_key_id_with_future_key(){
        String keyId = cerberusSigningKeyResolver.getSigningKeyId(jwtSecretData, 500);
        assertEquals("key id 2", keyId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_refresh_with_weak_key_should_throw_exception(){
        LinkedList<JwtSecret> jwtSecrets = cerberusSigningKeyResolver.getJwtSecretData().getJwtSecrets();
        JwtSecret jwtSecret = new JwtSecret();
        jwtSecret.setCreatedTs(500);
        jwtSecret.setEffectiveTs(600);
        jwtSecret.setId("key id weak");
        jwtSecret.setSecret("AAA==");
        jwtSecrets.add(jwtSecret);

        cerberusSigningKeyResolver.refresh();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_refresh_with_empty_kid_should_throw_exception(){
        LinkedList<JwtSecret> jwtSecrets = cerberusSigningKeyResolver.getJwtSecretData().getJwtSecrets();
        JwtSecret jwtSecret = new JwtSecret();
        jwtSecret.setCreatedTs(500);
        jwtSecret.setEffectiveTs(600);
        jwtSecret.setId("");
        jwtSecret.setSecret("DAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==");
        jwtSecrets.add(jwtSecret);

        cerberusSigningKeyResolver.refresh();
    }

    @Test
    public void test_resolve_signing_key_returns_newest_key() {
        CerberusJwtKeySpec keySpec = cerberusSigningKeyResolver.resolveSigningKey();
        assertEquals("key id 3", keySpec.getKid());
        assertEquals("HmacSHA512", keySpec.getAlgorithm());
        assertEquals(8, keySpec.getEncoded()[0]); // 8 == "CA"
    }

    @Test
    public void test_resolve_signing_key_returns_correct_key() {
        JwsHeader jwsHeader = new DefaultJwsHeader();
        jwsHeader.setAlgorithm("HS512");
        jwsHeader.setKeyId("key id 2");
        Claims claims = new DefaultClaims();
        Key key = cerberusSigningKeyResolver.resolveSigningKey(jwsHeader, claims);
        assertEquals(key.getAlgorithm(), "HmacSHA512");
        assertEquals(4, key.getEncoded()[0]); // 4 == "BA"
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_resolve_signing_key_throws_error_on_invalid_key_id() {
        JwsHeader jwsHeader = new DefaultJwsHeader();
        jwsHeader.setAlgorithm("HS512");
        jwsHeader.setKeyId("key id 666");
        Claims claims = new DefaultClaims();
        cerberusSigningKeyResolver.resolveSigningKey(jwsHeader, claims);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_resolve_signing_key_throws_error_on_invalid_algorithm() {
        JwsHeader jwsHeader = new DefaultJwsHeader();
        jwsHeader.setAlgorithm("none");
        Claims claims = new DefaultClaims();
        cerberusSigningKeyResolver.resolveSigningKey(jwsHeader, claims);
    }
}