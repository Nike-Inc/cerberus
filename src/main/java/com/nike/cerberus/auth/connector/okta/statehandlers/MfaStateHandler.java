package com.nike.cerberus.auth.connector.okta.statehandlers;

import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.resource.AuthenticationResponse;

import java.util.concurrent.CompletableFuture;

/**
 * MFA state handler to handle success when verifying an MFA factor.
 *
 * Though handleSuccess in this method is very similar to the method in
 * InitialLoginStateHandler, we are keeping MfaStateHandler because we are
 * planning to build on it to add SMS/Call functionality.
 */

public class MfaStateHandler extends AbstractOktaStateHandler {

    public MfaStateHandler(AuthenticationClient client, CompletableFuture<AuthResponse> authenticationResponseFuture) {
        super(client, authenticationResponseFuture);
    }

    /**
     * Handles authentication success.
     * @param successResponse - Authentication response from the Completable Future
     */
    @Override
    public void handleSuccess(AuthenticationResponse successResponse) {

        final String userId = successResponse.getUser().getId();
        final String userLogin = successResponse.getUser().getLogin();

        final AuthData authData = new AuthData()
                .setUserId(userId)
                .setUsername(userLogin);
        AuthResponse authResponse = new AuthResponse()
                .setData(authData)
                .setStatus(AuthStatus.SUCCESS);

        authenticationResponseFuture.complete(authResponse);
    }
}
