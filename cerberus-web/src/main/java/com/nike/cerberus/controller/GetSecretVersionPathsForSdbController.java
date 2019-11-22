package com.nike.cerberus.controller;

import com.nike.cerberus.security.PrincipalHasReadPermsForSdb;
import com.nike.cerberus.service.SafeDepositBoxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@RestController
@RequestMapping("/v1/sdb-secret-version-paths")
public class GetSecretVersionPathsForSdbController {

  private final SafeDepositBoxService safeDepositBoxService;

  @Autowired
  public GetSecretVersionPathsForSdbController(SafeDepositBoxService safeDepositBoxService) {
    this.safeDepositBoxService = safeDepositBoxService;
  }

  @PrincipalHasReadPermsForSdb
  @RequestMapping(value = "/{sdbId:.+}", method = GET)
  public Set<String> getVersionPathsForSdb(@PathVariable("sdbId") String sdbId) {
      return safeDepositBoxService.getSecureDataVersionPathsForSdb(sdbId);
  }
}
