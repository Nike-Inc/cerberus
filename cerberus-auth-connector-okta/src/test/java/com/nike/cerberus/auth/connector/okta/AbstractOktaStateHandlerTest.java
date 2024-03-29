/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.auth.connector.okta;

import static groovy.test.GroovyTestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.auth.connector.okta.statehandlers.AbstractOktaStateHandler;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.impl.resource.DefaultFactor;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.FactorProvider;
import com.okta.authn.sdk.resource.FactorType;
import com.okta.authn.sdk.resource.User;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/** Tests the AbstractOktaStateHandler class */
public class AbstractOktaStateHandlerTest {

  // class under test
  private AbstractOktaStateHandler abstractOktaStateHandler;

  // Dependencies
  @Mock private AuthenticationClient client;
  private CompletableFuture<AuthResponse> authenticationResponseFuture;

  @Before
  public void setup() {

    initMocks(this);

    authenticationResponseFuture = new CompletableFuture<>();

    // create test object
    this.abstractOktaStateHandler =
        new AbstractOktaStateHandler(client, authenticationResponseFuture) {};
  }

  /////////////////////////
  // Test Methods
  /////////////////////////

  @Test
  public void getFactorKey() {

    DefaultFactor factor = mock(DefaultFactor.class);
    when(factor.getType()).thenReturn(FactorType.PUSH);
    when(factor.getProvider()).thenReturn(FactorProvider.OKTA);

    String expected = "okta-push";
    String actual = abstractOktaStateHandler.getFactorKey(factor);

    assertEquals(expected, actual);
  }

  @Test
  public void getDeviceNameGoogleTotp() {

    FactorProvider provider = FactorProvider.GOOGLE;
    FactorType type = FactorType.TOKEN_SOFTWARE_TOTP;

    DefaultFactor factor = mock(DefaultFactor.class);
    when(factor.getType()).thenReturn(type);
    when(factor.getProvider()).thenReturn(provider);

    String result = this.abstractOktaStateHandler.getDeviceName(factor);

    assertEquals("Google Authenticator", result);
  }

  @Test
  public void getDeviceNameOktaTotp() {

    FactorProvider provider = FactorProvider.OKTA;
    FactorType type = FactorType.TOKEN_SOFTWARE_TOTP;

    DefaultFactor factor = mock(DefaultFactor.class);
    when(factor.getType()).thenReturn(type);
    when(factor.getProvider()).thenReturn(provider);

    String result = this.abstractOktaStateHandler.getDeviceName(factor);

    assertEquals("Okta Verify TOTP", result);
  }

  @Test
  public void getDeviceNameOktaPush() {

    FactorProvider provider = FactorProvider.OKTA;
    FactorType type = FactorType.PUSH;

    DefaultFactor factor = mock(DefaultFactor.class);
    when(factor.getType()).thenReturn(type);
    when(factor.getProvider()).thenReturn(provider);

    String result = this.abstractOktaStateHandler.getDeviceName(factor);

    assertEquals("Okta Verify Push", result);
  }

  @Test
  public void getDeviceNameOktaCall() {

    FactorProvider provider = FactorProvider.OKTA;
    FactorType type = FactorType.CALL;

    DefaultFactor factor = mock(DefaultFactor.class);
    when(factor.getType()).thenReturn(type);
    when(factor.getProvider()).thenReturn(provider);

    String result = this.abstractOktaStateHandler.getDeviceName(factor);

    assertEquals("Okta Voice Call", result);
  }

  @Test
  public void getDeviceNameOktaSms() {

    FactorProvider provider = FactorProvider.OKTA;
    FactorType type = FactorType.SMS;

    DefaultFactor factor = mock(DefaultFactor.class);
    when(factor.getType()).thenReturn(type);
    when(factor.getProvider()).thenReturn(provider);

    String result = this.abstractOktaStateHandler.getDeviceName(factor);

    assertEquals("Okta Text Message Code", result);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getDeviceNameFailsNullFactor() {

    this.abstractOktaStateHandler.getDeviceName(null);
  }

  @Test
  public void validateUserFactorsSuccess() {

    DefaultFactor factor1 = mock(DefaultFactor.class);
    when(factor1.getStatus()).thenReturn(AbstractOktaStateHandler.MFA_FACTOR_NOT_SETUP_STATUS);
    DefaultFactor factor2 = mock(DefaultFactor.class);

    this.abstractOktaStateHandler.validateUserFactors(Lists.newArrayList(factor1, factor2));
  }

  @Test(expected = ApiException.class)
  public void validateUserFactorsFailsNull() {

    this.abstractOktaStateHandler.validateUserFactors(null);
  }

  @Test(expected = ApiException.class)
  public void validateUserFactorsFailsEmpty() {

    this.abstractOktaStateHandler.validateUserFactors(Lists.newArrayList());
  }

  @Test(expected = ApiException.class)
  public void validateUserFactorsFailsAllFactorsNotSetUp() {

    String status = AbstractOktaStateHandler.MFA_FACTOR_NOT_SETUP_STATUS;

    DefaultFactor factor1 = mock(DefaultFactor.class);
    when(factor1.getStatus()).thenReturn(status);

    DefaultFactor factor2 = mock(DefaultFactor.class);
    when(factor2.getStatus()).thenReturn(status);

    this.abstractOktaStateHandler.validateUserFactors(Lists.newArrayList(factor1, factor2));
  }

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
    abstractOktaStateHandler.handleSuccess(expectedResponse);

    AuthResponse actualResponse = authenticationResponseFuture.get(1, TimeUnit.SECONDS);

    //  verify results
    Assert.assertEquals(id, actualResponse.getData().getUserId());
    Assert.assertEquals(email, actualResponse.getData().getUsername());
    Assert.assertEquals(status, actualResponse.getStatus());
  }

  @Test(expected = ApiException.class)
  public void handleUnknownLockout() {

    String status = "LOCKED_OUT";

    AuthenticationResponse unknownResponse = mock(AuthenticationResponse.class);
    when(unknownResponse.getStatusString()).thenReturn(status);

    abstractOktaStateHandler.handleUnknown(unknownResponse);
  }

  @Test(expected = ApiException.class)
  public void handleUnknownPasswordExpired() {

    String status = "PASSWORD_EXPIRED";

    AuthenticationResponse unknownResponse = mock(AuthenticationResponse.class);
    when(unknownResponse.getStatusString()).thenReturn(status);

    abstractOktaStateHandler.handleUnknown(unknownResponse);
  }

  @Test
  public void testIsFido() {
    DefaultFactor nonFidoFactor = mock(DefaultFactor.class);
    when(nonFidoFactor.getVendorName()).thenReturn("Okta");
    Assert.assertFalse(abstractOktaStateHandler.isFido(nonFidoFactor));

    DefaultFactor fidoFactor = mock(DefaultFactor.class);
    when(fidoFactor.getVendorName()).thenReturn("FIDO");
    Assert.assertTrue(abstractOktaStateHandler.isFido(fidoFactor));
  }

  @Test
  public void testIsPush() {
    DefaultFactor nonPushFactor = mock(DefaultFactor.class);
    when(nonPushFactor.getVendorName()).thenReturn("Okta");
    when(nonPushFactor.getProvider()).thenReturn(FactorProvider.OKTA);
    when(nonPushFactor.getType()).thenReturn(FactorType.TOKEN_SOFTWARE_TOTP);

    Assert.assertFalse(abstractOktaStateHandler.isPush(nonPushFactor));

    DefaultFactor pushFactor = mock(DefaultFactor.class);
    when(pushFactor.getVendorName()).thenReturn("Okta");
    when(pushFactor.getProvider()).thenReturn(FactorProvider.OKTA);
    when(pushFactor.getType()).thenReturn(FactorType.PUSH);

    Assert.assertTrue(abstractOktaStateHandler.isPush(pushFactor));
  }

  @Test
  public void testShouldSkip() {
    DefaultFactor nonSkipFactor = mock(DefaultFactor.class);
    when(nonSkipFactor.getVendorName()).thenReturn("Okta");
    when(nonSkipFactor.getProvider()).thenReturn(FactorProvider.OKTA);
    when(nonSkipFactor.getType()).thenReturn(FactorType.TOKEN_SOFTWARE_TOTP);

    Assert.assertFalse(abstractOktaStateHandler.shouldSkip(nonSkipFactor));

    DefaultFactor pushFactor = mock(DefaultFactor.class);
    when(pushFactor.getVendorName()).thenReturn("Okta");
    when(pushFactor.getProvider()).thenReturn(FactorProvider.OKTA);
    when(pushFactor.getType()).thenReturn(FactorType.PUSH);

    Assert.assertTrue(abstractOktaStateHandler.isPush(pushFactor));
    Assert.assertTrue(abstractOktaStateHandler.shouldSkip(pushFactor));

    DefaultFactor fidoFactor = mock(DefaultFactor.class);
    when(fidoFactor.getVendorName()).thenReturn("FIDO");
    when(fidoFactor.getProvider()).thenReturn(FactorProvider.OKTA);
    when(fidoFactor.getType()).thenReturn(FactorType.TOKEN_SOFTWARE_TOTP);

    Assert.assertTrue(abstractOktaStateHandler.isFido(fidoFactor));
    Assert.assertTrue(abstractOktaStateHandler.shouldSkip(fidoFactor));
    verify(fidoFactor, times(0)).getProvider();
  }
}
