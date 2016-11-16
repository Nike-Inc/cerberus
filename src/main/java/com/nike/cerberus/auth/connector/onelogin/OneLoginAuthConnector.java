/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.auth.connector.onelogin;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthConnector;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthMfaDevice;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.error.DefaultApiError;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * OneLogin version 1 API implementation of the AuthConnector interface.
 */
public class OneLoginAuthConnector implements AuthConnector {

    private static final Set<String> VALID_API_REGIONS = Sets.newHashSet("us", "eu");

    private static final String DEFAULT_ONELOGIN_API_URI_TEMPLATE = "https://api.%s.onelogin.com/";

    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/json");

    private static final int DEFAULT_TIMEOUT = 15;

    private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final OkHttpClient httpClient= new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
            .writeTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
            .readTimeout(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT)
            .build();

    private final ObjectMapper objectMapper;

    private final URI oneloginApiUri;

    private final String clientId;

    private final String clientSecret;

    private final String subdomain;

    private GenerateTokenResponseData tokenData;

    private OffsetDateTime tokenExpireTime;

    @Inject
    public OneLoginAuthConnector(@Named("auth.connector.onelogin.api_region") final String oneloginApiRegion,
                                 @Named("auth.connector.onelogin.client_id") final String clientId,
                                 @Named("auth.connector.onelogin.client_secret") final String clientSecret,
                                 @Named("auth.connector.onelogin.subdomain") final String subdomain) {
        Preconditions.checkArgument(VALID_API_REGIONS.contains(oneloginApiRegion),
                "OneLogin API region is invalid! Valid values: %s, %s",
                VALID_API_REGIONS.toArray());
        Preconditions.checkNotNull(clientId);
        Preconditions.checkNotNull(clientSecret);
        Preconditions.checkNotNull(subdomain);

        this.oneloginApiUri = URI.create(String.format(DEFAULT_ONELOGIN_API_URI_TEMPLATE, oneloginApiRegion));

        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.objectMapper.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.subdomain = subdomain;
    }

    @Override
    public AuthResponse authenticate(String username, String password) {
        final SessionLoginTokenData sessionLoginToken = createSessionLoginToken(username, password);
        final AuthData authData = new AuthData();
        final AuthResponse authResponse = new AuthResponse().setData(authData);

        if (StringUtils.isNotBlank(sessionLoginToken.getStateToken())) {
            authResponse.setStatus(AuthStatus.MFA_REQUIRED);
            authData.setStateToken(sessionLoginToken.getStateToken());

            sessionLoginToken.getDevices().forEach(d -> authData.getDevices().add(new AuthMfaDevice()
                    .setId(String.valueOf(d.getDeviceId()))
                    .setName(d.getDeviceType())));
        } else {
            authResponse.setStatus(AuthStatus.SUCCESS);
        }

        authData.setUserId(String.valueOf(sessionLoginToken.getUser().getId()));
        authData.setUsername(sessionLoginToken.getUser().getUsername());

        return authResponse;
    }

    @Override
    public AuthResponse mfaCheck(String stateToken, String deviceId, String otpToken) {
        final SessionLoginTokenData sessionLoginToken = verifyFactor(deviceId, stateToken, otpToken);
        final AuthData authData = new AuthData();
        final AuthResponse authResponse = new AuthResponse().setData(authData);

        authResponse.setStatus(AuthStatus.SUCCESS);
        authData.setUserId(String.valueOf(sessionLoginToken.getUser().getId()));
        authData.setUsername(sessionLoginToken.getUser().getUsername());

        return authResponse;
    }

    @Override
    public Set<String> getGroups(AuthData data) {
        final UserData userData = getUserById(Long.parseLong(data.getUserId()));
        return parseLdapGroups(userData.getMemberOf());
    }

    /**
     * Takes the list of ldapGroups received from OneLogin and parses them in to a set of Strings
     * @param ldapGroups A string consisting of ldap groups received from OneLogin
     * @return A set of Strings consisting of the ldap groups that were parsed from the provided string
     */
    protected Set<String> parseLdapGroups(final String ldapGroups) {
        Set<String> groups = new HashSet<>();
        if (ldapGroups == null) {
            return groups;
        }

        // One Login double xml escapes entries
        String escapedLdapGroups = StringEscapeUtils.unescapeXml(StringEscapeUtils.unescapeXml(ldapGroups));

        Iterable<String> canonicalNameIterable;
        Iterable<String> piecesIterable;
        Iterable<String> canonicalNames = Splitter.on(";").split(escapedLdapGroups);
        for (String canonicalName : canonicalNames) {
            canonicalNameIterable = Splitter.on(",").split(canonicalName);
            String[] pieces = Iterables.toArray(canonicalNameIterable, String.class);

            piecesIterable = Splitter.on("=").split(pieces[0]);
            String[] parts = Iterables.toArray(piecesIterable, String.class);
            if (parts.length >= 2) {
                groups.add(parts[1]);
            } else {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                        .withExceptionMessage("OneLogin user info member-of field is malformed!")
                        .build();
            }
        }

        return groups;
    }

    /**
     * Request for getting all user data by their ID.
     *
     * @param userId User ID
     * @return User data
     */
    protected UserData getUserById(final long userId) {

        final Response response = execute(buildUrl("api/1/users/" + userId), "GET",
                buildAuthorizationBearerHeader(), null);
        final GetUserResponse getUserResponse = parseResponseBody(response, GetUserResponse.class);

        if (getUserResponse.getStatus().isError()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionMessage(getUserResponse.getStatus().getMessage())
                    .build();
        }

        return getUserResponse.getData().get(0);
    }

    /**
     * Request for verifying a MFA factor.
     *
     * @param deviceId MFA device id
     * @param stateToken State token
     * @param optToken Token from MFA device
     * @return Session login token
     */
    protected SessionLoginTokenData verifyFactor(final String deviceId,
                                                 final String stateToken,
                                                 final String optToken) {
        VerifyFactorRequest request = new VerifyFactorRequest()
                .setDeviceId(deviceId)
                .setStateToken(stateToken)
                .setOtpToken(optToken);

        final Response response = execute(buildUrl("api/1/login/verify_factor"), "POST",
                buildAuthorizationBearerHeader(), request);
        final VerifyFactorResponse verifyFactorResponse = parseResponseBody(response,
                VerifyFactorResponse.class);

        if (verifyFactorResponse.getStatus().isError()) {
            if (verifyFactorResponse.getStatus().getCode() == 400L) {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS)
                        .withExceptionMessage(verifyFactorResponse.getStatus().getMessage())
                        .build();
            } else {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST)
                        .withExceptionMessage(verifyFactorResponse.getStatus().getMessage())
                        .build();
            }
        }

        return verifyFactorResponse.getData().get(0);
    }

    /**
     * Request for creating a session login token.  This is used to validate a user's credentials with OneLogin.
     *
     * @param username OneLogin username
     * @param password OneLogin password
     * @return Session login token
     */
    protected SessionLoginTokenData createSessionLoginToken(final String username, final String password) {
        CreateSessionLoginTokenRequest request = new CreateSessionLoginTokenRequest()
                .setUsernameOrEmail(username)
                .setPassword(password)
                .setSubdomain(subdomain);

        final Response response = execute(buildUrl("api/1/login/auth"), "POST",
                buildAuthorizationBearerHeader(), request);
        final CreateSessionLoginTokenResponse createSessionLoginTokenResponse = parseResponseBody(response,
                CreateSessionLoginTokenResponse.class);

        if (createSessionLoginTokenResponse.getStatus().isError()) {
            if (createSessionLoginTokenResponse.getStatus().getCode() == 400L) {
                if (StringUtils.startsWithIgnoreCase(createSessionLoginTokenResponse.getStatus().getMessage(), "MFA")) {
                    throw ApiException.newBuilder()
                            .withApiErrors(DefaultApiError.MFA_SETUP_REQUIRED)
                            .withExceptionMessage(createSessionLoginTokenResponse.getStatus().getMessage())
                            .build();
                } else {
                    throw ApiException.newBuilder()
                            .withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS)
                            .withExceptionMessage(createSessionLoginTokenResponse.getStatus().getMessage())
                            .build();
                }
            } else {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST)
                        .withExceptionMessage(createSessionLoginTokenResponse.getStatus().getMessage())
                        .build();
            }
        }

        return createSessionLoginTokenResponse.getData().get(0);
    }

    /**
     * Determines where to get an access token from.  If one is present and still active, it simply returns that,
     * otherwise it determines if it needs to request an access token or simply refresh with a refresh token.
     *
     * @return Access token
     */
    protected String getAccessToken() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        OffsetDateTime refreshTokenExpireTime = null;

        if (tokenData != null) {
            refreshTokenExpireTime = tokenData.getCreatedAt().plusDays(45).minusMinutes(5);
        }

        if (tokenData == null || now.isAfter(refreshTokenExpireTime)) {
            tokenData = requestAccessToken();
            tokenExpireTime = tokenData.getCreatedAt().plusSeconds(tokenData.getExpiresIn()).minusMinutes(5);
        } else if (now.isAfter(tokenExpireTime)) {
            tokenData = refreshAccessToken();
            tokenExpireTime = tokenData.getCreatedAt().plusSeconds(tokenData.getExpiresIn()).minusMinutes(5);
        }

        return tokenData.getAccessToken();
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
     * Refreshes the access token using the current refresh token.
     *
     * @return  Access token
     */
    protected GenerateTokenResponseData refreshAccessToken() {
        final RefreshTokenRequest request = new RefreshTokenRequest()
                .setAccessToken(tokenData.getAccessToken())
                .setRefreshToken(tokenData.getRefreshToken());

        final Response response = execute(buildUrl("auth/oauth2/token"), "POST", null, request);
        final RefreshTokenResponse refreshTokenResponse = parseResponseBody(response, RefreshTokenResponse.class);

        if (refreshTokenResponse.getStatus().isError()) {
            tokenData = null;
            tokenExpireTime = null;

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionMessage("Failed to refresh token with OneLogin!")
                    .build();
        }

        return refreshTokenResponse.getData().get(0);
    }

    /**
     * Builds a map containing the Authorization header with a valid bearer token.
     *
     * @return Map containing the Authorization header and value.
     */
    protected Map<String, String> buildAuthorizationBearerHeader() {
        final Map<String, String> headers = Maps.newHashMap();
        headers.put("Authorization", String.format("bearer:%s", getAccessToken()));
        return headers;
    }

    /**
     * Builds the full URL for preforming an operation against Vault.
     *
     * @param path   Path for the requested operation
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
