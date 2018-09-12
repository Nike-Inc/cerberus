package com.nike.cerberus.auth.connector.okta.statehandlers;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.okta.authn.sdk.AuthenticationException;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.Factor;
import com.okta.authn.sdk.resource.VerifyFactorRequest;
import com.okta.authn.sdk.impl.resource.DefaultVerifyPassCodeFactorRequest;

import java.util.concurrent.CompletableFuture;


public class MfaStateHandler extends AuthenticationStateHandlerAdapter {

    public final AuthenticationClient client;
    public final CompletableFuture<AuthenticationResponse> authenticationResponseFuture;

    public MfaStateHandler(AuthenticationClient client, CompletableFuture<AuthenticationResponse> authenticationResponseFuture) {
        this.client = client;
        this.authenticationResponseFuture = authenticationResponseFuture;
    }

    public void handleMfaChallenge(AuthenticationResponse mfaChallengeResponse) {
        if (! (mfaChallengeResponse.getFactors().get(0).get("factorType") == "push")) {
            return;
        }

        VerifyFactorRequest request = client.instantiate(VerifyFactorRequest.class);
        request.setStateToken(mfaChallengeResponse.getStateToken());

        String id = mfaChallengeResponse.getFactors().get(0).getId();

        if (mfaChallengeResponse.getFactorResult() == "REJECTED") {
            authenticationResponseFuture.complete(mfaChallengeResponse);
            return;
        }

        try {
            client.verifyFactor(id, request, this);
        } catch (AuthenticationException e) {
            authenticationResponseFuture.complete(mfaChallengeResponse);
        }
    }

    @Override
    public void handleSuccess(AuthenticationResponse successResponse) {
        authenticationResponseFuture.complete(successResponse);
    }

    @Override
    public void handleUnauthenticated(AuthenticationResponse unauthenticatedResponse) {
        authenticationResponseFuture.complete(unauthenticatedResponse);
    }

    public void handleMfaRequired(AuthenticationResponse mfaRequiredResponse) {

        Integer factorSelection = Integer.valueOf(System.console().readLine("Select factor to verify?"));
        Factor factor = mfaRequiredResponse.getFactors().get(factorSelection);

        String type = factor.getType().toString();
        try {
            switch (type) {
                case "token:software:totp":
                    handleCode(factor, mfaRequiredResponse.getStateToken());
                    break;
                case "sms":
                    triggerCodeGeneratingFactorAndPromptForCode(factor, mfaRequiredResponse.getStateToken());
                    break;
                case "call":
                    triggerCodeGeneratingFactorAndPromptForCode(factor, mfaRequiredResponse.getStateToken());
                    break;
                case "push":
                    handlePush(factor, mfaRequiredResponse.getStateToken());
                    break;
                default:
                    throw new RuntimeException("Unknown factor type: " + type);
            }
        } catch (AuthenticationException e) {
            throw ApiException.newBuilder()
                    .withExceptionCause(e)
                    .withApiErrors(DefaultApiError.FAILED_TO_READ_FACTOR)
                    .withExceptionMessage("Failed to read factor type.")
                    .build();
        }
    }

    public void handleUnknown(AuthenticationResponse typedUnknownResponse) {
        authenticationResponseFuture.complete(typedUnknownResponse);
    }

    public void triggerCodeGeneratingFactorAndPromptForCode(Factor factor, String stateToken) throws AuthenticationException {
        client.challengeFactor(factor.getId(), stateToken, this);
        String code = System.console().readLine("Enter code: ");
        verifyCode(code, factor.getId(), stateToken);
    }

    public void handleCode(Factor factor, String stateToken) throws AuthenticationException {
        String otp = System.console().readLine("Enter one time code: ");
        verifyCode(otp, factor.getId(), stateToken);
    }

    public void handlePush(Factor factor, String stateToken) throws AuthenticationException {
        client.challengeFactor(factor.getId(), stateToken, this);
    }

    public void verifyCode(String code, String id, String stateToken) throws AuthenticationException {
        DefaultVerifyPassCodeFactorRequest request = client.instantiate(DefaultVerifyPassCodeFactorRequest.class);
        request.setPassCode(code);
        request.setStateToken(stateToken);

        client.verifyFactor(id, request, this);
    }

}
