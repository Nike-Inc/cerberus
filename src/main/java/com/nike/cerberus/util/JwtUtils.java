package com.nike.cerberus.util;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.AuthTokenRecord;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;
import java.security.Key;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

public class JwtUtils {
    public static final String JWT_SECRET_KEY_CONFIG_PARAM = "cms.auth.jwt.secret.key";
    public static final String JWT_SECRET_ALGORITHM_CONFIG_PARAM = "cms.auth.jwt.secret.algorithm";
    private final Key key;
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public JwtUtils(@Named(JWT_SECRET_KEY_CONFIG_PARAM) final String keyString, @Named(JWT_SECRET_ALGORITHM_CONFIG_PARAM) final String algorithm) {
        byte[] bytes = Base64.getDecoder().decode(keyString);
        key = new SecretKeySpec(bytes, algorithm);
    }

    public String generateJwtToken(AuthTokenRecord tokenRecord){
        String jwtToken = Jwts.builder()
                .setSubject(tokenRecord.getPrincipal())
                .claim("principalType", tokenRecord.getPrincipalType())
                .claim("groups", tokenRecord.getGroups())
                .claim("isAdmin", tokenRecord.getIsAdmin())
                .claim("refreshCount", tokenRecord.getRefreshCount())
                .setExpiration(Date.from(tokenRecord.getExpiresTs().toInstant()))
                .setIssuedAt(Date.from(tokenRecord.getCreatedTs().toInstant()))
                .signWith(key)
                .compact();
        return jwtToken;
    }

    public Optional<AuthTokenRecord> parseClaim(String token) {
        Jws<Claims> claimsJws;
        try {
            claimsJws = Jwts.parser().setSigningKey(key).parseClaimsJws(token);
        } catch (JwtException e) {
            log.error("Error parsing JWT token: {}", token);
            throw new ApiException(DefaultApiError.ACCESS_DENIED);
        }
        Claims claims = claimsJws.getBody();
        String subject = claims.getSubject();
        AuthTokenRecord authTokenRecord = new AuthTokenRecord().setTokenHash(token)
                .setPrincipal(subject)
                .setExpiresTs(OffsetDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault()))
                .setCreatedTs(OffsetDateTime.ofInstant(claims.getIssuedAt().toInstant(), ZoneId.systemDefault()))
                .setPrincipalType(claims.get("principalType", String.class))
                .setGroups(claims.get("groups", String.class))
                .setIsAdmin(claims.get("isAdmin", Boolean.class))
                .setRefreshCount(claims.get("refreshCount", Integer.class));

        return Optional.of(authTokenRecord);
    }
}
