package com.nike.cerberus.auth.connector.okta;

import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.auth.connector.okta.statehandlers.MfaStateHandler;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;



/**
 * Tests the MfaOktaStateHandler class
 */
public class MfaStateHandlerTest {

    // class under test
    private MfaStateHandler mfaStateHandler;

    // Dependencies
    @Mock
    private AuthenticationClient client;
    private CompletableFuture<AuthResponse> authenticationResponseFuture;

    @Before
    public void setup() {

        initMocks(this);

        authenticationResponseFuture = new CompletableFuture<>();

        // create test object
        this.mfaStateHandler = new MfaStateHandler(client, authenticationResponseFuture) {

        };
    }

    /////////////////////////
    // Test Methods
    /////////////////////////

    @Test
    public void handleSuccess() throws Exception {

        String email = "email";
        String id = "id";
        AuthStatus status = AuthStatus.SUCCESS;

        AuthenticationResponse expectedResponse = mock(AuthenticationResponse.class);

        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getLogin()).thenReturn(email);
        when(expectedResponse.getUser()).thenReturn(user);

        // do the call
        mfaStateHandler.handleSuccess(expectedResponse);

        AuthResponse actualResponse = authenticationResponseFuture.get(1, TimeUnit.SECONDS);

        //  verify results
        assertEquals(id, actualResponse.getData().getUserId());
        assertEquals(email, actualResponse.getData().getUsername());
        assertEquals(status, actualResponse.getStatus());
    }

}
