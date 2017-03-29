package com.nike.cerberus.auth.connector.onelogin;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

/**
 * Client for calling OneLogin APIs
 */
@Singleton
class OneLoginClient {

    private final String clientId;
    private final String clientSecret;
    private final String subdomain;

    private final OneLoginHttpClient httpClient;

    @Inject
    public OneLoginClient(@Named("auth.connector.onelogin.client_id") final String clientId,
                          @Named("auth.connector.onelogin.client_secret") final String clientSecret,
                          @Named("auth.connector.onelogin.subdomain") final String subdomain,
                          OneLoginHttpClient httpClient) {

        Preconditions.checkNotNull(clientId);
        Preconditions.checkNotNull(clientSecret);
        Preconditions.checkNotNull(subdomain);

        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.subdomain = subdomain;

        this.httpClient = httpClient;
    }

    /**
     * Attempt to login a user
     */
    public CreateSessionLoginTokenResponse createSessionLoginToken(final String username, final String password) {
        CreateSessionLoginTokenRequest request = new CreateSessionLoginTokenRequest()
                .setUsernameOrEmail(username)
                .setPassword(password)
                .setSubdomain(subdomain);

        return httpClient.execute("api/1/login/auth",
                "POST",
                buildAuthorizationBearerHeader(),
                request,
                CreateSessionLoginTokenResponse.class);
    }

    /**
     * Verify MFA
     */
    public VerifyFactorResponse verifyFactor(final String deviceId,
                                             final String stateToken,
                                             final String otpToken) {
        VerifyFactorRequest request = new VerifyFactorRequest()
                .setDeviceId(deviceId)
                .setStateToken(stateToken)
                .setOtpToken(otpToken);

        return httpClient.execute("api/1/login/verify_factor",
                "POST",
                buildAuthorizationBearerHeader(),
                request,
                VerifyFactorResponse.class);
    }

    /**
     * Get info about a user
     */
    public GetUserResponse getUserById(long userId) {
        return httpClient.execute("api/1/users/" + userId,
                "GET",
                buildAuthorizationBearerHeader(),
                null,
                GetUserResponse.class);
    }

    /**
     * Builds a map containing the Authorization header with a valid bearer token.
     *
     * @return Map containing the Authorization header and value.
     */
    protected Map<String, String> buildAuthorizationBearerHeader() {
        final Map<String, String> headers = Maps.newHashMap();
        headers.put("Authorization", String.format("bearer:%s", requestAccessToken().getAccessToken()));
        return headers;
    }

    /**
     * Requests an access token using the configured client id and secret.
     *
     * @return Access token
     */
    protected GenerateTokenResponseData requestAccessToken() {


        final GenerateTokenResponse generateTokenResponse = httpClient.execute("auth/oauth2/token", "POST", buildAuthorizationHeader(), new GenerateTokenRequest(), GenerateTokenResponse.class);

        if (generateTokenResponse.getStatus().isError()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionMessage("Failed to generate an access token with OneLogin!")
                    .build();
        }

        return generateTokenResponse.getData().get(0);
    }

    /**
     * Used in GenerateTokenRequest
     */
    protected Map<String,String> buildAuthorizationHeader() {
        final Map<String, String> headers = Maps.newHashMap();
        headers.put("Authorization", String.format("client_id:%s, client_secret:%s", clientId, clientSecret));
        return headers;
    }


}
