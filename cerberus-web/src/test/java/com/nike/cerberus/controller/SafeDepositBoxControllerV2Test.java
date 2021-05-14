package com.nike.cerberus.controller;

import com.nike.cerberus.domain.SafeDepositBoxSummary;
import com.nike.cerberus.domain.SafeDepositBoxV2;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.service.SafeDepositBoxService;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.util.UriComponentsBuilder;

public class SafeDepositBoxControllerV2Test {

  @Mock private SafeDepositBoxService safeDepositBoxService;

  @InjectMocks private SafeDepositBoxControllerV2 safeDepositBoxControllerV2;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCreateSafeDepositBox() {
    SafeDepositBoxV2 safeDepositBoxV2 = Mockito.mock(SafeDepositBoxV2.class);
    Mockito.when(safeDepositBoxV2.getId()).thenReturn("id");
    Authentication authentication = Mockito.mock(Authentication.class);
    Mockito.when(authentication.getName()).thenReturn("name");
    Mockito.when(safeDepositBoxService.createSafeDepositBoxV2(safeDepositBoxV2, "name"))
        .thenReturn(safeDepositBoxV2);
    UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
    ResponseEntity<SafeDepositBoxV2> safeDepositBoxResponseEntity =
        safeDepositBoxControllerV2.createSafeDepositBox(
            safeDepositBoxV2, authentication, uriComponentsBuilder);
    Assert.assertSame(safeDepositBoxV2, safeDepositBoxResponseEntity.getBody());
    HttpHeaders headers = safeDepositBoxResponseEntity.getHeaders();
    Assert.assertEquals("/V2/safe-deposit-box/id", headers.get("Location").get(0));
  }

  @Test
  public void testGetSafeDepositBox() {
    SafeDepositBoxV2 safeDepositBoxV2 = Mockito.mock(SafeDepositBoxV2.class);
    Mockito.when(safeDepositBoxService.getSDBAndValidatePrincipalAssociationV2("sdbId"))
        .thenReturn(safeDepositBoxV2);
    SafeDepositBoxV2 actualSafeDepositBox = safeDepositBoxControllerV2.getSafeDepositBox("sdbId");
    Assert.assertSame(safeDepositBoxV2, actualSafeDepositBox);
  }

  @Test
  public void testUpdateSafeDepositBoxV2() {
    SafeDepositBoxV2 safeDepositBoxV2 = Mockito.mock(SafeDepositBoxV2.class);
    CerberusPrincipal cerberusPrincipal = Mockito.mock(CerberusPrincipal.class);
    Mockito.when(
            safeDepositBoxService.updateSafeDepositBoxV2(
                safeDepositBoxV2, cerberusPrincipal, "sdbId"))
        .thenReturn(safeDepositBoxV2);
    SafeDepositBoxV2 actualSafeDepositBoxV2 =
        safeDepositBoxControllerV2.updateSafeDepositBox(
            "sdbId", safeDepositBoxV2, cerberusPrincipal);
    Assert.assertSame(safeDepositBoxV2, actualSafeDepositBoxV2);
  }

  @Test
  public void testDeleteSafeDepositBox() {
    safeDepositBoxControllerV2.deleteSafeDepositBox("sdbId");
    Mockito.verify(safeDepositBoxService).deleteSafeDepositBox("sdbId");
  }

  @Test
  public void testGetSafeDepositBoxes() {
    CerberusPrincipal cerberusPrincipal = Mockito.mock(CerberusPrincipal.class);
    SafeDepositBoxSummary safeDepositBoxSummary = Mockito.mock(SafeDepositBoxSummary.class);
    List<SafeDepositBoxSummary> safeDepositBoxSummaries = new ArrayList<>();
    safeDepositBoxSummaries.add(safeDepositBoxSummary);
    Mockito.when(safeDepositBoxService.getAssociatedSafeDepositBoxes(cerberusPrincipal))
        .thenReturn(safeDepositBoxSummaries);
    List<SafeDepositBoxSummary> actualSafeDepositBoSummaries =
        safeDepositBoxControllerV2.getSafeDepositBoxes(cerberusPrincipal);
    Assert.assertSame(safeDepositBoxSummaries, actualSafeDepositBoSummaries);
    Assert.assertEquals(safeDepositBoxSummaries, actualSafeDepositBoSummaries);
  }
}
