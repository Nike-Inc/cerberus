package com.nike.cerberus.auth.connector.okta;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.auth.connector.okta.statehandlers.PushStateHandler;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.AuthenticationStatus;
import com.okta.authn.sdk.resource.User;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PushStateHandlerTest {

  private PushStateHandler pushStateHandler;

  @Mock private AuthenticationClient client;
  private CompletableFuture<AuthResponse> authenticationResponseFuture;

  @Before
  public void setup() {

    initMocks(this);

    authenticationResponseFuture = new CompletableFuture<>();

    // create test object
    this.pushStateHandler = new PushStateHandler(client, authenticationResponseFuture) {};
  }

  @Test
  public void handleMfaChallengeHappy()
      throws InterruptedException, ExecutionException, TimeoutException {
    String email = "email";
    String id = "id";
    AuthStatus status = AuthStatus.MFA_CHALLENGE;

    AuthenticationResponse expectedResponse = mock(AuthenticationResponse.class);

    User user = mock(User.class);
    when(user.getId()).thenReturn(id);
    when(user.getLogin()).thenReturn(email);
    when(expectedResponse.getUser()).thenReturn(user);
    when(expectedResponse.getStatus()).thenReturn(AuthenticationStatus.MFA_CHALLENGE);

    // do the call
    pushStateHandler.handleMfaChallenge(expectedResponse);

    AuthResponse actualResponse = authenticationResponseFuture.get(1, TimeUnit.SECONDS);

    //  verify results
    assertEquals(id, actualResponse.getData().getUserId());
    assertEquals(email, actualResponse.getData().getUsername());
    assertEquals(status, actualResponse.getStatus());
  }

  @Test
  public void handleMfaSuccessHappy()
      throws InterruptedException, ExecutionException, TimeoutException {
    String email = "email";
    String id = "id";
    AuthStatus status = AuthStatus.SUCCESS;

    AuthenticationResponse expectedResponse = mock(AuthenticationResponse.class);

    User user = mock(User.class);
    when(user.getId()).thenReturn(id);
    when(user.getLogin()).thenReturn(email);
    when(expectedResponse.getUser()).thenReturn(user);
    when(expectedResponse.getStatus()).thenReturn(AuthenticationStatus.SUCCESS);

    // do the call
    pushStateHandler.handleSuccess(expectedResponse);

    AuthResponse actualResponse = authenticationResponseFuture.get(1, TimeUnit.SECONDS);

    //  verify results
    assertEquals(id, actualResponse.getData().getUserId());
    assertEquals(email, actualResponse.getData().getUsername());
    assertEquals(status, actualResponse.getStatus());
  }
}
