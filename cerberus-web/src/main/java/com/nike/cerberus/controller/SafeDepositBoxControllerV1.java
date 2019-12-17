package com.nike.cerberus.controller;

import static com.nike.cerberus.security.CerberusPrincipal.ROLE_USER;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import com.nike.cerberus.domain.SafeDepositBoxSummary;
import com.nike.cerberus.domain.SafeDepositBoxV1;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.security.PrincipalHasOwnerPermsForSdb;
import com.nike.cerberus.security.PrincipalHasReadPermsForSdb;
import com.nike.cerberus.service.SafeDepositBoxService;
import java.util.List;
import java.util.Map;
import javax.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Validated
@Deprecated // TODO can we delete v1 sdb endpoints now
@RestController
@RequestMapping("/v1/safe-deposit-box")
public class SafeDepositBoxControllerV1 {

  private final SafeDepositBoxService safeDepositBoxService;

  @Autowired
  public SafeDepositBoxControllerV1(SafeDepositBoxService safeDepositBoxService) {
    this.safeDepositBoxService = safeDepositBoxService;
  }

  @RolesAllowed(ROLE_USER)
  @RequestMapping(method = POST, consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<?> createSafeDepositBox(
      @RequestBody SafeDepositBoxV1 request,
      Authentication authentication,
      UriComponentsBuilder b) {
    var id = safeDepositBoxService.createSafeDepositBoxV1(request, authentication.getName());
    var body = Map.of("id", id);
    var url = b.path("/v1/safe-deposit-box/{id}").buildAndExpand(id).toUri();
    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(url);
    return new ResponseEntity<Map>(body, headers, CREATED);
  }

  @PrincipalHasReadPermsForSdb
  @RequestMapping(value = "/{sdbId:.+}", method = GET)
  public SafeDepositBoxV1 getSafeDepositBox(@PathVariable("sdbId") String sdbId) {
    return safeDepositBoxService.getSDBAndValidatePrincipalAssociationV1(sdbId);
  }

  @PrincipalHasOwnerPermsForSdb
  @RequestMapping(value = "/{sdbId:.+}", consumes = APPLICATION_JSON_VALUE, method = PUT)
  public void updateSafeDepositBox(
      @PathVariable("sdbId") String sdbId,
      @RequestBody SafeDepositBoxV1 request,
      Authentication authentication) {
    safeDepositBoxService.updateSafeDepositBoxV1(
        request, (CerberusPrincipal) authentication, sdbId);
  }

  @PrincipalHasOwnerPermsForSdb
  @RequestMapping(value = "/{sdbId:.+}", method = DELETE)
  public void deleteSafeDepositBox(@PathVariable("sdbId") String sdbId) {
    safeDepositBoxService.deleteSafeDepositBox(sdbId);
  }

  @RolesAllowed(ROLE_USER)
  @RequestMapping(method = GET)
  public List<SafeDepositBoxSummary> getSafeDepositBoxes(Authentication authentication) {
    return safeDepositBoxService.getAssociatedSafeDepositBoxes((CerberusPrincipal) authentication);
  }
}
