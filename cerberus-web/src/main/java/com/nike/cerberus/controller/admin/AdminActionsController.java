package com.nike.cerberus.controller.admin;

import com.nike.cerberus.domain.AuthKmsKeyMetadataResult;
import com.nike.cerberus.domain.SDBMetadata;
import com.nike.cerberus.service.KmsService;
import com.nike.cerberus.service.SafeDepositBoxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;

import static com.nike.cerberus.security.CerberusPrincipal.ROLE_ADMIN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@Slf4j
@Validated
@RestController
@RolesAllowed(ROLE_ADMIN)
@RequestMapping("/v1/admin")
public class AdminActionsController {

  private final KmsService kmsService;
  private final SafeDepositBoxService safeDepositBoxService;

  @Autowired
  public AdminActionsController(KmsService kmsService,
                                SafeDepositBoxService safeDepositBoxService) {

    this.kmsService = kmsService;
    this.safeDepositBoxService = safeDepositBoxService;
  }

  @RequestMapping(value = "/authentication-kms-metadata", method = GET)
  public AuthKmsKeyMetadataResult getAuthKmsKeyMetadata() {
    return new AuthKmsKeyMetadataResult(
      kmsService.getAuthenticationKmsMetadata()
    );
  }

  @RequestMapping(value = "/override-owner", method = POST, consumes = APPLICATION_JSON_VALUE)
  public void overrideSdbOwner(@RequestBody SDBMetadata request, Authentication authentication) {
    safeDepositBoxService.overrideOwner(request.getName(), request.getOwner(), authentication.getName());
  }
}
