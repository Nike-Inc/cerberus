package com.nike.cerberus.controller;

import static com.nike.cerberus.event.AuditUtils.createBaseAuditableEvent;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.SecureDataVersionsResult;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.event.AuditableEvent;
import com.nike.cerberus.security.PrincipalHasReadPermsForPath;
import com.nike.cerberus.service.EventProcessorService;
import com.nike.cerberus.service.SecureDataVersionService;
import com.nike.cerberus.util.SdbAccessRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/secret-versions")
public class GetSecureDataVersionsController {

  private final SecureDataVersionService secureDataVersionService;
  private final EventProcessorService eventProcessorService;
  private final SdbAccessRequest sdbAccessRequest; // Request scoped proxy bean

  @Autowired
  public GetSecureDataVersionsController(
      SecureDataVersionService secureDataVersionService,
      EventProcessorService eventProcessorService,
      SdbAccessRequest sdbAccessRequest) {

    this.secureDataVersionService = secureDataVersionService;
    this.eventProcessorService = eventProcessorService;
    this.sdbAccessRequest = sdbAccessRequest;
  }

  @PrincipalHasReadPermsForPath
  @RequestMapping(value = "/**", method = GET)
  public SecureDataVersionsResult getVersionPathsForSdb(
      @RequestParam(value = "limit", required = false, defaultValue = "100") int limit,
      @RequestParam(value = "offset", required = false, defaultValue = "0") int offset) {

    var result =
        secureDataVersionService.getSecureDataVersionSummariesByPath(
            sdbAccessRequest.getSdbId(),
            sdbAccessRequest.getPath(),
            sdbAccessRequest.getCategory(),
            limit,
            offset);

    if (result.getSecureDataVersionSummaries().isEmpty()) {
      AuditableEvent auditableEvent =
          createBaseAuditableEvent(getClass().getSimpleName())
              .withSuccess(false)
              .withAction(
                  "Failed to find versions for secret with path: " + sdbAccessRequest.getPath())
              .build();
      eventProcessorService.ingestEvent(auditableEvent);

      throw ApiException.newBuilder().withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST).build();
    }

    return result;
  }
}
