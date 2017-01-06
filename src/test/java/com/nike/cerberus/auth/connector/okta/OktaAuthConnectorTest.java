package com.nike.cerberus.auth.connector.okta;

import com.google.common.collect.Lists;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.okta.sdk.models.auth.AuthResult;
import com.okta.sdk.models.factors.Factor;
import com.okta.sdk.models.usergroups.UserGroup;
import com.okta.sdk.models.usergroups.UserGroupProfile;
import com.okta.sdk.models.users.User;
import com.okta.sdk.models.users.UserProfile;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the OktaAuthConnector class
 */
public class OktaAuthConnectorTest {

    // class under test
    private OktaAuthConnector oktaAuthConnector;

    // dependencies
    private OktaAuthHelper oktaAuthHelper;

    @Before
    public void setup() {

        // mock dependencies
        oktaAuthHelper = mock(OktaAuthHelper.class);

        // create test object
        oktaAuthConnector = new OktaAuthConnector(oktaAuthHelper);
    }

    /////////////////////////
    // Helper Methods
    /////////////////////////

    private EmbeddedAuthResponseDataV1 mockEmbedded(String email, String id, List<Factor> factors) {

        UserProfile profile = mock(UserProfile.class);
        when(profile.getLogin()).thenReturn(email);

        User user = mock(User.class);
        when(user.getProfile()).thenReturn(profile);
        when(user.getId()).thenReturn(id);

        EmbeddedAuthResponseDataV1 embedded = mock(EmbeddedAuthResponseDataV1.class);
        when(embedded.getUser()).thenReturn(user);
        when(embedded.getFactors()).thenReturn(factors);

        return embedded;
    }

    private Factor mockFactor(String provider, String id) {

        Factor factor = mock(Factor.class);
        when(factor.getId()).thenReturn(id);
        when(factor.getProvider()).thenReturn(provider);
        when(oktaAuthHelper.getDeviceName(factor)).thenCallRealMethod();

        return factor;
    }

    /////////////////////////
    // Test Methods
    /////////////////////////

    @Test
    public void authenticateHappySuccess() throws Exception {

        String username = "username";
        String password = "password";

        String email = "email";
        String id = "id";
        EmbeddedAuthResponseDataV1 embedded = mockEmbedded(email, id, null);

        AuthResult authResult = mock(AuthResult.class);
        when(authResult.getStatus()).thenReturn(OktaAuthHelper.AUTHENTICATION_SUCCESS_STATUS);

        when(oktaAuthHelper.authenticateUser(username, password, null)).thenReturn(authResult);
        when(oktaAuthHelper.getEmbeddedAuthData(authResult)).thenReturn(embedded);

        // do the call
        AuthResponse result = this.oktaAuthConnector.authenticate(username, password);

        // verify results
        assertEquals(result.getData().getUserId(), id);
        assertEquals(result.getData().getUsername(), email);
    }

    @Test
    public void authenticateHappyMfaSuccess() throws Exception {

        String username = "username";
        String password = "password";

        String email = "email";
        String id = "id";
        String provider = "provider";
        String deviceId = "device id";
        Factor factor = mockFactor(provider, deviceId);
        EmbeddedAuthResponseDataV1 embedded = mockEmbedded(email, id, Lists.newArrayList(factor));

        AuthResult authResult = mock(AuthResult.class);
        when(authResult.getStatus()).thenReturn(OktaAuthHelper.AUTHENTICATION_MFA_REQUIRED_STATUS);
        when(authResult.getStateToken()).thenReturn("state token");

        when(oktaAuthHelper.authenticateUser(username, password, null)).thenReturn(authResult);
        when(oktaAuthHelper.getEmbeddedAuthData(authResult)).thenReturn(embedded);

        // do the call
        AuthResponse result = this.oktaAuthConnector.authenticate(username, password);

        // verify results
        assertEquals(id, result.getData().getUserId());
        assertEquals(email, result.getData().getUsername());
        assertEquals(1, result.getData().getDevices().size());
        assertEquals(deviceId, result.getData().getDevices().get(0).getId());
        assertEquals(StringUtils.capitalize(provider), result.getData().getDevices().get(0).getName());
    }

    @Test
    public void mfaCheckHappy() {

        String stateToken = "state token";
        String deviceId = "device id";
        String otpToken = "otp token";

        String email = "email";
        String id = "id";
        EmbeddedAuthResponseDataV1 embedded = mockEmbedded(email, id, null);

        when(oktaAuthHelper.getEmbeddedAuthData(anyObject())).thenReturn(embedded);

        // do the call
        AuthResponse result = this.oktaAuthConnector.mfaCheck(stateToken, deviceId, otpToken);

        // verify results
        assertEquals(id, result.getData().getUserId());
        assertEquals(email, result.getData().getUsername());
    }

    @Test
    public void getGroupsHappy() {

        String id = "id";
        AuthData authData = mock(AuthData.class);
        when(authData.getUserId()).thenReturn(id);

        String name1 = "name 1";
        UserGroupProfile profile1 = mock(UserGroupProfile.class);
        UserGroup group1 = mock(UserGroup.class);
        when(profile1.getName()).thenReturn(name1);
        when(group1.getProfile()).thenReturn(profile1);

        String name2 = "name 2";
        UserGroupProfile profile2 = mock(UserGroupProfile.class);
        UserGroup group2 = mock(UserGroup.class);
        when(profile2.getName()).thenReturn(name2);
        when(group2.getProfile()).thenReturn(profile2);

        when(oktaAuthHelper.getUserGroups(id)).thenReturn(Lists.newArrayList(group1, group2));

        // do the call
        Set<String> result = this.oktaAuthConnector.getGroups(authData);

        // verify results
        assertTrue(result.contains(name1));
        assertTrue(result.contains(name2));
    }
}