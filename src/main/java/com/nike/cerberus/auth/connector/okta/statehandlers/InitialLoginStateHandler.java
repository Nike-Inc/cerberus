package com.nike.cerberus.auth.connector.okta.statehandlers;

import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.resource.AuthenticationResponse;

import java.util.concurrent.CompletableFuture;

public class InitialLoginStateHandler extends AuthenticationStateHandlerAdapter {

    public final CompletableFuture<AuthenticationResponse> authenticationResponseFuture;

    public InitialLoginStateHandler(CompletableFuture<AuthenticationResponse> authenticationResponseFuture) {
        this.authenticationResponseFuture = authenticationResponseFuture;
    }

    @Override
    public void handleUnknown(AuthenticationResponse typedUnknownResponse) {
        authenticationResponseFuture.complete(typedUnknownResponse);
    }
}
