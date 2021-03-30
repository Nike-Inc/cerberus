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

package com.nike.cerberus.controller.authentication;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.aws.sts.AwsStsClient;
import com.nike.cerberus.event.filter.AuditLoggingFilterDetails;
import com.nike.cerberus.service.AuthenticationService;
import java.io.IOException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.authentication.OpenSamlAuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v2/auth/saml")
public class SamlController {

  private final AuthenticationService authenticationService;
  private final AwsStsClient awsStsClient;
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;

  @Autowired
  public SamlController(
      AuthenticationService authenticationService,
      AwsStsClient awsStsClient,
      AuditLoggingFilterDetails auditLoggingFilterDetails) {

    this.authenticationService = authenticationService;
    this.awsStsClient = awsStsClient;
    this.auditLoggingFilterDetails = auditLoggingFilterDetails;
  }

  @RequestMapping(value = "/{registrationId}", method = POST)
  public void authenticate(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository =
        new InMemoryRelyingPartyRegistrationRepository(
            RelyingPartyRegistrations.fromMetadataLocation("sample.metadata.location")
                .registrationId("one")
                .assertionConsumerServiceLocation("{baseUrl}/v2/auth/saml/{registrationId}")
                .build());
    Saml2AuthenticationTokenConverter saml2AuthenticationTokenConverter =
        new Saml2AuthenticationTokenConverter(
            new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository));
    Authentication authentication = saml2AuthenticationTokenConverter.convert(request);
    if (authentication == null) {
      Saml2Error saml2Error =
          new Saml2Error(
              Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
              "No relying party registration found");
      throw new Saml2AuthenticationException(saml2Error);
    }
    Authentication samlExtensionParsedAuthObject =
        new OpenSamlAuthenticationProvider().authenticate(authentication);

    // tokenservice.gettoken(email)
    AuthResponse authResponse =
        this.authenticationService.authenticate(samlExtensionParsedAuthObject.getName());
    String token = authResponse.getData().getClientToken().getClientToken();
    long leaseDuration = authResponse.getData().getClientToken().getLeaseDuration();
    Cookie tokenCookie = new Cookie("token", token);
    response.setHeader("SameSite", "None");
    tokenCookie.setPath("/");
    Cookie leaseDurationCookie = new Cookie("lease-duration", Long.toString(leaseDuration));
    response.setHeader("SameSite", "None");
    leaseDurationCookie.setPath("/");
    // add cookie to response
    response.addCookie(tokenCookie);
    response.addCookie(leaseDurationCookie);
    response.sendRedirect("/");
    //    return new RedirectView("\\");
  }
}
