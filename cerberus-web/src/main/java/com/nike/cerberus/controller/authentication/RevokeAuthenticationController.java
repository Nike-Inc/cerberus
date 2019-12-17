package com.nike.cerberus.controller.authentication;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;

import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
public class RevokeAuthenticationController {
  private final AuthenticationService authenticationService;

  @Autowired
  public RevokeAuthenticationController(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @RequestMapping(method = DELETE)
  public void revokeAuthentication(Authentication authentication) {
    var cerberusPrincipal = (CerberusPrincipal) authentication;
    authenticationService.revoke(cerberusPrincipal.getToken());
  }
}
