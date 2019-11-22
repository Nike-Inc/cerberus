package com.nike.cerberus.controller;

import com.nike.cerberus.domain.SecureFileSummaryResult;
import com.nike.cerberus.security.PrincipalHasReadPermsForPath;
import com.nike.cerberus.service.SecureDataService;
import com.nike.cerberus.util.SdbAccessRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Slf4j
@Validated
@RestController
@RequestMapping("/v1/secure-files")
public class SecureFilesSummaryController {

  private final SecureDataService secureDataService;
  private final SdbAccessRequest sdbAccessRequest; // Request scoped proxy bean

  @Autowired
  public SecureFilesSummaryController(SecureDataService secureDataService,
                                      SdbAccessRequest sdbAccessRequest) {

    this.secureDataService = secureDataService;
    this.sdbAccessRequest = sdbAccessRequest;
  }

  @PrincipalHasReadPermsForPath
  @RequestMapping(value = "/**", method = GET)
  public SecureFileSummaryResult listSecureFiles(@RequestParam(value = "limit", required = false, defaultValue = "100") int limit,
                                                 @RequestParam(value = "offset", required = false, defaultValue = "0") int offset) {

    return secureDataService.listSecureFilesSummaries(
      sdbAccessRequest.getSdbId(),
      sdbAccessRequest.getPath(),
      limit,
      offset);
  }
}
