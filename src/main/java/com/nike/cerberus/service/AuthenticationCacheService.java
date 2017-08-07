package com.nike.cerberus.service;

import com.google.common.cache.Cache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.nike.cerberus.domain.IamPrincipalCredentials;
import com.nike.cerberus.domain.IamRoleAuthResponse;
import com.nike.cerberus.domain.IamRoleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache wrapper for AuthenticationService.
 * <p>
 * Authenticating an IAM principal is fairly expensive because it requires:
 * 1. KMS API calls to AWS (which are rate limited)
 * 2. Generating a Token in Vault (technically a write operation)
 * <p>
 * But the result of these calls can be cached and re-used for a short-period of time, e.g. 60 seconds.
 * <p>
 * In order to cache the auth result we do a slight hack where report the token lease is lower than it actually is.
 * E.g. if token lease is 60 minutes, we'll report it as 59 minutes so that we can keep it cached for up to 1 minute.
 */
@Singleton
public class AuthenticationCacheService {

    /**
     * Property name for how long IAM auth should be cached in seconds.
     */
    public static final String IAM_TOKEN_CACHE_TTL = "cms.iam.token.cache.ttl";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AuthenticationService authenticationService;
    private final Cache<Object, IamRoleAuthResponse> authCache;

    @Inject
    public AuthenticationCacheService(AuthenticationService authenticationService,
                                      @Named("iamAuthCache") Cache<Object, IamRoleAuthResponse> authCache) {
        this.authenticationService = authenticationService;
        this.authCache = authCache;
    }

    /**
     * Return cached auth, if present, otherwise auth.
     *
     * @deprecated this method is for the v1 auth service that will be removed eventually
     */
    public IamRoleAuthResponse authenticate(IamRoleCredentials credentials) {
        IamRoleAuthResponse response = authCache.getIfPresent(credentials);
        if (response != null) {
            logger.info("auth cache hit for {}", credentials.getRoleName());
        }
        else {
            response = authenticationService.authenticate(credentials);
            authCache.put(credentials, response);
        }
        return response;
    }

    /**
     * Return cached auth, if present, otherwise auth.
     */
    public IamRoleAuthResponse authenticate(IamPrincipalCredentials credentials) {
        IamRoleAuthResponse response = authCache.getIfPresent(credentials);
        if (response != null) {
            logger.info("auth cache hit for {}", credentials.getIamPrincipalArn());
        }
        else {
            response = authenticationService.authenticate(credentials);
            authCache.put(credentials, response);
        }
        return response;
    }

}
