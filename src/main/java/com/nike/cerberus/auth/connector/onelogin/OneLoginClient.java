package com.nike.cerberus.auth.connector.onelogin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.server.config.guice.OneLoginGuiceModule;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

/**
 * Client for calling OneLogin APIs
 */
@Singleton
class OneLoginClient {

    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/json");

    private static final Set<String> VALID_API_REGIONS = Sets.newHashSet("us", "eu");

    private static final String DEFAULT_ONELOGIN_API_URI_TEMPLATE = "https://api.%s.onelogin.com/";

    private final URI oneloginApiUri;

    private final String clientId;
    private final String clientSecret;
    private final String subdomain;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public OneLoginClient(@Named("auth.connector.onelogin.api_region") final String oneloginApiRegion,
                          @Named("auth.connector.onelogin.client_id") final String clientId,
                          @Named("auth.connector.onelogin.client_secret") final String clientSecret,
                          @Named("auth.connector.onelogin.subdomain") final String subdomain,
                          final OkHttpClient httpClient,
                          @Named(OneLoginGuiceModule.ONE_LOGIN_OBJECT_MAPPER_NAME) final ObjectMapper objectMapper) {

        Preconditions.checkArgument(VALID_API_REGIONS.contains(oneloginApiRegion),
                "OneLogin API region is invalid! Valid values: %s, %s",
                VALID_API_REGIONS.toArray());
        Preconditions.checkNotNull(clientId);
        Preconditions.checkNotNull(clientSecret);
        Preconditions.checkNotNull(subdomain);

        this.oneloginApiUri = URI.create(String.format(DEFAULT_ONELOGIN_API_URI_TEMPLATE, oneloginApiRegion));

        this.clientId = clientId;
        this.clientSecret = clientSecret;

        this.subdomain = subdomain;

        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Attempt to login a user
     */
    public CreateSessionLoginTokenResponse createSessionLoginToken(final String username, final String password) {
        CreateSessionLoginTokenRequest request = new CreateSessionLoginTokenRequest()
                .setUsernameOrEmail(username)
                .setPassword(password)
                .setSubdomain(subdomain);

        return execute("api/1/login/auth", "POST", request, CreateSessionLoginTokenResponse.class);
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

        return execute("api/1/login/verify_factor", "POST", request, VerifyFactorResponse.class);
    }

    /**
     * Get info about a user
     */
    public GetUserResponse getUserById(long userId) {
        return execute("api/1/users/" + userId, "GET", null, GetUserResponse.class);
    }

    protected <M> M execute(final String path, final String method, final Object requestBody, final Class<M> responseClass) {
        Response response = execute(buildUrl(path), method, buildAuthorizationBearerHeader(), requestBody);
        return parseResponseBody(response, responseClass);
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
        final GenerateTokenRequest request = new GenerateTokenRequest();
        final Map<String, String> headers = Maps.newHashMap();
        headers.put("Authorization", String.format("client_id:%s, client_secret:%s", clientId, clientSecret));

        final Response response = execute(buildUrl("auth/oauth2/token"), "POST", headers, request);
        final GenerateTokenResponse generateTokenResponse = parseResponseBody(response, GenerateTokenResponse.class);

        if (generateTokenResponse.getStatus().isError()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionMessage("Failed to generate an access token with OneLogin!")
                    .build();
        }

        return generateTokenResponse.getData().get(0);
    }

    /**
     * Executes the HTTP request based on the input parameters.
     *
     * @param url         The URL to execute the request against
     * @param method      The HTTP method for the request
     * @param requestBody The request body of the HTTP request
     * @return Response from the server
     */
    protected Response execute(final HttpUrl url, final String method, final Map<String, String> headers, final Object requestBody) {
        try {

            Request.Builder requestBuilder = new Request.Builder().url(url)
                    .addHeader("Accept", DEFAULT_MEDIA_TYPE.toString());

            if (headers != null) {
                headers.forEach(requestBuilder::addHeader);
            }

            if (requestBody != null) {
                requestBuilder.addHeader("Content-Type", DEFAULT_MEDIA_TYPE.toString())
                        .method(method, RequestBody.create(DEFAULT_MEDIA_TYPE,
                                objectMapper.writeValueAsString(requestBody).getBytes(Charset.forName("UTF-8"))));
            } else {
                requestBuilder.method(method, null);
            }

            return httpClient.newCall(requestBuilder.build()).execute();
        } catch (IOException e) {
            if (e instanceof SSLException
                    && e.getMessage() != null
                    && e.getMessage().contains("Unrecognized SSL message, plaintext connection?")) {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                        .withExceptionCause(e)
                        .withExceptionMessage("I/O error while communicating with OneLogin. Unrecognized SSL message may be due to a web proxy e.g. AnyConnect")
                        .build();
            } else {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                        .withExceptionCause(e)
                        .withExceptionMessage("I/O error while communicating with OneLogin.")
                        .build();
            }
        }
    }

    /**
     * Builds the full URL for preforming an operation against Vault.
     *
     * @param path Path for the requested operation
     * @return Full URL to execute a request against
     */
    protected HttpUrl buildUrl(final String path) {
        String baseUrl = oneloginApiUri.toString();

        if (!StringUtils.endsWith(baseUrl, "/")) {
            baseUrl += "/";
        }

        return HttpUrl.parse(baseUrl + path);
    }

    /**
     * Convenience method for parsing the HTTP response and mapping it to a class.
     *
     * @param response      The HTTP response object
     * @param responseClass The class to map the response body to
     * @param <M>           Represents the type to map to
     * @return Deserialized object from the response body
     */
    protected <M> M parseResponseBody(final Response response, final Class<M> responseClass) {
        try {
            return objectMapper.readValue(response.body().string(), responseClass);
        } catch (IOException e) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionCause(e)
                    .withExceptionMessage("Error parsing the response body from OneLogin.")
                    .build();
        }
    }
}
