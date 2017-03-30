package com.nike.cerberus.auth.connector.onelogin;

import com.google.common.collect.Lists;
import com.nike.backstopper.exception.ApiException;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OneLoginClientTest {

    private static final String clientId = "clientId";
    private static final String clientSecret = "clientSecret";
    private static final String subdomain = "subdomain";

    private static final String accessToken = "accessToken";

    private OneLoginHttpClient httpClient;
    private OneLoginClient oneLoginClient;

    @Before
    public void setup() {
        httpClient = mock(OneLoginHttpClient.class);
        oneLoginClient = new OneLoginClient(clientId, clientSecret, subdomain, httpClient);
    }

    @Test
    public void test_createSessionLoginToken() {

        setupMocksToGiveAccessToken();

        String username = "username";
        String password = "password";

        CreateSessionLoginTokenRequest request = new CreateSessionLoginTokenRequest()
                .setUsernameOrEmail(username)
                .setPassword(password)
                .setSubdomain(subdomain);

        CreateSessionLoginTokenResponse response = mock(CreateSessionLoginTokenResponse.class);

        when(httpClient.execute("api/1/login/auth", "POST", oneLoginClient.buildAuthorizationBearerHeader(), request, CreateSessionLoginTokenResponse.class))
                .thenReturn(response);

        // invoke method under test
        CreateSessionLoginTokenResponse actualResponse = oneLoginClient.createSessionLoginToken(username, password);

        assertEquals(response, actualResponse);
    }

    @Test
    public void test_verifyFactor() {

        setupMocksToGiveAccessToken();

        String deviceId = "devid";
        String stateToken = "stateToken";
        String otpToken = "otpToken";

        VerifyFactorRequest request = new VerifyFactorRequest()
                .setDeviceId(deviceId)
                .setStateToken(stateToken)
                .setOtpToken(otpToken);

        VerifyFactorResponse response = mock(VerifyFactorResponse.class);

        when(httpClient.execute("api/1/login/verify_factor", "POST", oneLoginClient.buildAuthorizationBearerHeader(), request, VerifyFactorResponse.class)).thenReturn(response);

        // invoke method under test
        VerifyFactorResponse actualResponse = oneLoginClient.verifyFactor(deviceId, stateToken, otpToken);

        assertEquals(response, actualResponse);
    }

    @Test
    public void test_getUserById() {

        setupMocksToGiveAccessToken();

        long userId = 101L;

        GetUserResponse response = mock(GetUserResponse.class);

        when(httpClient.execute("api/1/users/" + userId, "GET", oneLoginClient.buildAuthorizationBearerHeader(), null, GetUserResponse.class))
                .thenReturn(response);

        // invoke method under test
        GetUserResponse actualResponse = oneLoginClient.getUserById(userId);

        assertEquals(response, actualResponse);

    }

    @Test
    public void test_buildAuthorizationBearerHeader() {

        setupMocksToGiveAccessToken();

        // invoke method under test
        Map<String, String> headers = oneLoginClient.buildAuthorizationBearerHeader();

        assertEquals(1, headers.size());
        assertEquals("bearer:" + accessToken, headers.get("Authorization"));
    }

    @Test
    public void test_requestAccessToken() {

        setupMocksToGiveAccessToken();

        // invoke method under test
        GenerateTokenResponseData response = oneLoginClient.requestAccessToken();

        assertEquals(accessToken, response.getAccessToken());
    }

    @Test(expected = ApiException.class)
    public void test_requestAccessToken_gives_error() {

        ResponseStatus status = new ResponseStatus();
        status.setError(true);
        status.setCode(500L);

        GenerateTokenResponse response = new GenerateTokenResponse();
        response.setStatus(status);

        when(httpClient.execute("auth/oauth2/token", "POST", oneLoginClient.buildAuthorizationHeader(), new GenerateTokenRequest(), GenerateTokenResponse.class))
                .thenReturn(response);

        // invoke method under test
        oneLoginClient.requestAccessToken();
    }

    private void setupMocksToGiveAccessToken() {

        ResponseStatus status = new ResponseStatus();
        status.setError(false);
        status.setCode(200L);

        GenerateTokenResponseData data = new GenerateTokenResponseData();
        data.setAccessToken(accessToken);

        GenerateTokenResponse response = new GenerateTokenResponse();
        response.setStatus(status);
        response.setData(Lists.newArrayList(data));

        when(httpClient.execute("auth/oauth2/token", "POST", oneLoginClient.buildAuthorizationHeader(), new GenerateTokenRequest(), GenerateTokenResponse.class))
                .thenReturn(response);


    }
}