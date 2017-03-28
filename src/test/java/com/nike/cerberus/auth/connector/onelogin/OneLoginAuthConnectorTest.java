package com.nike.cerberus.auth.connector.onelogin;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.error.DefaultApiError;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OneLoginAuthConnectorTest {

    // constants used by several tests
    private static final long USER_ID = 1001L;
    private static final String USERNAME = "USERNAME";
    private static final String PASSWORD = "PASSWORD";
    private static final Long DEVICE_ID = 1001L;
    private static final String DEVICE_TYPE = "DEVICE_TYPE";
    private static final String STATE_TOKEN = "STATE_TOKEN";
    private static final String OTP_TOKEN = "OTP_TOKEN";

    // mock
    private OneLoginClient oneLoginClient = null;

    // class under test
    private OneLoginAuthConnector oneLoginAuthConnector = null;

    @Before
    public void setup() {
        oneLoginClient = mock(OneLoginClient.class);
        oneLoginAuthConnector = new OneLoginAuthConnector(oneLoginClient);
    }

    @Test
    public void testAuthenticateNoMfa() {
        ResponseStatus status = new ResponseStatus();
        status.setError(false);

        CreateSessionLoginTokenResponse createSessionLoginTokenResponse = new CreateSessionLoginTokenResponse();
        createSessionLoginTokenResponse.setStatus(status);

        SessionUser user = new SessionUser();
        user.setId(USER_ID);
        user.setUsername(USERNAME);

        SessionLoginTokenData sessionLoginTokenData = new SessionLoginTokenData();
        sessionLoginTokenData.setUser(user);

        createSessionLoginTokenResponse.setData(Lists.newArrayList(sessionLoginTokenData));

        when(oneLoginClient.createSessionLoginToken(USERNAME, PASSWORD)).thenReturn(createSessionLoginTokenResponse);

        // invoke method under test
        AuthResponse response = oneLoginAuthConnector.authenticate(USERNAME, PASSWORD);

        assertEquals(Long.toString(user.getId()),  response.getData().getUserId());
        assertEquals(user.getUsername(),  response.getData().getUsername());
        assertEquals(AuthStatus.SUCCESS, response.getStatus());
    }

    @Test
    public void testAuthenticateWithMfa() {
        ResponseStatus status = new ResponseStatus();
        status.setError(false);

        CreateSessionLoginTokenResponse createSessionLoginTokenResponse = new CreateSessionLoginTokenResponse();
        createSessionLoginTokenResponse.setStatus(status);

        SessionUser user = new SessionUser();
        user.setId(USER_ID);
        user.setUsername(USERNAME);

        MfaDevice device = new MfaDevice();
        device.setDeviceId(DEVICE_ID);
        device.setDeviceType(DEVICE_TYPE);

        SessionLoginTokenData sessionLoginTokenData = new SessionLoginTokenData();
        sessionLoginTokenData.setUser(user);
        sessionLoginTokenData.setStateToken(STATE_TOKEN);
        sessionLoginTokenData.setDevices(Lists.newArrayList(device));

        createSessionLoginTokenResponse.setData(Lists.newArrayList(sessionLoginTokenData));

        when(oneLoginClient.createSessionLoginToken(USERNAME, PASSWORD)).thenReturn(createSessionLoginTokenResponse);

        // invoke method under test
        AuthResponse response = oneLoginAuthConnector.authenticate(USERNAME, PASSWORD);

        assertEquals(Long.toString(user.getId()),  response.getData().getUserId());
        assertEquals(user.getUsername(),  response.getData().getUsername());
        assertEquals(AuthStatus.MFA_REQUIRED, response.getStatus());
        assertEquals(DEVICE_ID.toString(), response.getData().getDevices().get(0).getId());
        assertEquals(DEVICE_TYPE, response.getData().getDevices().get(0).getName());
    }

    @Test
    public void testAuthenticateMfaError() {

        setupMockWhereLoginGivesError(400L, "mfa something error message");

        try {
            // invoke method under test
            oneLoginAuthConnector.authenticate(USERNAME, PASSWORD);

            fail("expected exception not thrown");
        }
        catch (ApiException e) {
            assertTrue(e.getApiErrors().contains(DefaultApiError.MFA_SETUP_REQUIRED));
            assertFalse(e.getApiErrors().contains(DefaultApiError.AUTH_BAD_CREDENTIALS));
        }
    }

    @Test
    public void testAuthenticateBadCreds() {
        setupMockWhereLoginGivesError(400L, "any other error message");

        try {
            // invoke method under test
            oneLoginAuthConnector.authenticate(USERNAME, PASSWORD);

            fail("expected exception not thrown");
        }
        catch (ApiException e) {
            assertTrue(e.getApiErrors().contains(DefaultApiError.AUTH_BAD_CREDENTIALS));
            assertFalse(e.getApiErrors().contains(DefaultApiError.MFA_SETUP_REQUIRED));

        }
    }

    @Test
    public void testAuthenticateBadRequest() {
        setupMockWhereLoginGivesError(500L, "any error message");

        try {
            // invoke method under test
            oneLoginAuthConnector.authenticate(USERNAME, PASSWORD);

            fail("expected exception not thrown");
        }
        catch (ApiException e) {
            assertTrue(e.getApiErrors().contains(DefaultApiError.GENERIC_BAD_REQUEST));
        }
    }

    private void setupMockWhereLoginGivesError(long statusCode, String message) {
        ResponseStatus status = new ResponseStatus();
        status.setCode(statusCode);
        status.setMessage(message);
        status.setError(true);

        CreateSessionLoginTokenResponse createSessionLoginTokenResponse = new CreateSessionLoginTokenResponse();
        createSessionLoginTokenResponse.setStatus(status);

        when(oneLoginClient.createSessionLoginToken(USERNAME, PASSWORD)).thenReturn(createSessionLoginTokenResponse);
    }

    @Test
    public void testMfaCheck() {
        SessionUser user = new SessionUser();
        user.setId(USER_ID);
        user.setUsername(USERNAME);

        SessionLoginTokenData sessionLoginTokenData = new SessionLoginTokenData();
        sessionLoginTokenData.setUser(user);

        VerifyFactorResponse verifyFactorResponse = mock(VerifyFactorResponse.class);
        when(verifyFactorResponse.getStatus()).thenReturn(new ResponseStatus());
        when(verifyFactorResponse.getData()).thenReturn(Lists.newArrayList(sessionLoginTokenData));
        when(oneLoginClient.verifyFactor(DEVICE_ID.toString(), STATE_TOKEN, OTP_TOKEN)).thenReturn(verifyFactorResponse);

        // invoke method under test
        AuthResponse response = oneLoginAuthConnector.mfaCheck(STATE_TOKEN, DEVICE_ID.toString(), OTP_TOKEN);
        assertEquals(Long.toString(user.getId()),  response.getData().getUserId());
        assertEquals(user.getUsername(),  response.getData().getUsername());
        assertEquals(AuthStatus.SUCCESS, response.getStatus());
    }

    @Test
    public void testParseLdapGroups() {
        String ldapGroups = "CN=Application.foo.users,OU=Application,OU=Groups,DC=ad,DC=acme,DC=com;CN=Application.bar.users,OU=Application,OU=Groups,DC=ad,DC=acme,DC=com";
        Set<String> actualResults = oneLoginAuthConnector.parseLdapGroups(ldapGroups);
        Set<String> expectedResults = Sets.newHashSet("Application.bar.users", "Application.foo.users");
        assertEquals(expectedResults, actualResults);
    }

    @Test
    public void testParseLdapGroupsNull() {
        Set<String> actualResults = oneLoginAuthConnector.parseLdapGroups(null);
        Set<String> expectedResults = Sets.newHashSet();
        assertEquals(expectedResults, actualResults);
    }

    @Test
    public void testGetUserById() {
        ResponseStatus status = new ResponseStatus();
        status.setError(false);

        UserData userData = new UserData();
        userData.setId(USER_ID);

        GetUserResponse getUserResponse = new GetUserResponse();
        getUserResponse.setData(Lists.newArrayList(userData));
        getUserResponse.setStatus(status);

        when(oneLoginClient.getUserById(USER_ID)).thenReturn(getUserResponse);

        // invoke method under test
        UserData actualData = oneLoginAuthConnector.getUserById(USER_ID);

        assertEquals(userData.getId(), actualData.getId());
    }


    @Test
    public void testGetUserByIdError() {
        ResponseStatus status = new ResponseStatus();
        status.setError(true);
        GetUserResponse getUserResponse = new GetUserResponse();
        getUserResponse.setStatus(status);

        when(oneLoginClient.getUserById(USER_ID)).thenReturn(getUserResponse);

        try {
            // invoke method under test
            oneLoginAuthConnector.getUserById(USER_ID);

            fail("expected exception not thrown");
        }
        catch (ApiException e) {
            assertTrue(e.getApiErrors().contains(DefaultApiError.SERVICE_UNAVAILABLE));
        }
    }

    @Test
    public void testVerifyFactor400Error() {

        setupMockWhereVerifyGivesError(400L, "any error message");

        try {
            // invoke method under test
            oneLoginAuthConnector.verifyFactor(DEVICE_ID.toString(), STATE_TOKEN, OTP_TOKEN);

            fail("expected exception not thrown");
        }
        catch (ApiException e) {
            assertTrue(e.getApiErrors().contains(DefaultApiError.AUTH_BAD_CREDENTIALS));
        }
    }

    @Test
    public void testVerifyFactor500Error() {

        setupMockWhereVerifyGivesError(500L, "any error message");

        try {
            // invoke method under test
            oneLoginAuthConnector.verifyFactor(DEVICE_ID.toString(), STATE_TOKEN, OTP_TOKEN);

            fail("expected exception not thrown");
        }
        catch (ApiException e) {
            assertTrue(e.getApiErrors().contains(DefaultApiError.GENERIC_BAD_REQUEST));
        }
    }

    private void setupMockWhereVerifyGivesError(long statusCode, String message) {
        ResponseStatus status = new ResponseStatus();
        status.setCode(statusCode);
        status.setMessage(message);
        status.setError(true);

        VerifyFactorResponse verifyFactorResponse = new VerifyFactorResponse();
        verifyFactorResponse.setStatus(status);

        when(oneLoginClient.verifyFactor(DEVICE_ID.toString(), STATE_TOKEN, OTP_TOKEN)).thenReturn(verifyFactorResponse);
    }

    @Test
    public void testCreateSessionLoginToken() {
        ResponseStatus status = new ResponseStatus();
        status.setError(false);

        CreateSessionLoginTokenResponse createSessionLoginTokenResponse = new CreateSessionLoginTokenResponse();
        createSessionLoginTokenResponse.setStatus(status);

        SessionLoginTokenData expectedData = mock(SessionLoginTokenData.class);
        createSessionLoginTokenResponse.setData(Lists.newArrayList(expectedData));

        when(oneLoginClient.createSessionLoginToken(USERNAME, PASSWORD)).thenReturn(createSessionLoginTokenResponse);

        // invoke method under test
        SessionLoginTokenData  actualData = oneLoginAuthConnector.createSessionLoginToken(USERNAME, PASSWORD);

        assertEquals(expectedData,  actualData);
    }

}