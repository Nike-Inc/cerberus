package com.nike.cerberus.controller;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.SecureDataVersionSummary;
import com.nike.cerberus.domain.SecureDataVersionsResult;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.event.filter.AuditLoggingFilterDetails;
import com.nike.cerberus.service.SecureDataVersionService;
import com.nike.cerberus.util.SdbAccessRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class GetSecureDataVersionsControllerTest {
  @Mock private SecureDataVersionService secureDataVersionService;
  @Mock private SdbAccessRequest sdbAccessRequest;
  @Mock private AuditLoggingFilterDetails auditLoggingFilterDetails;

  @InjectMocks private GetSecureDataVersionsController getSecureDataVersionsController;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetVersionPathsForSdbWhenSecureDataVersionsAreNotPresent() {
    SecureDataVersionsResult secureDataVersionsResult =
        Mockito.mock(SecureDataVersionsResult.class);
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    Mockito.when(sdbAccessRequest.getCategory()).thenReturn("category");
    Mockito.when(
            secureDataVersionService.getSecureDataVersionSummariesByPath(
                "sdbId", "path", "category", 10, 10))
        .thenReturn(secureDataVersionsResult);
    ApiError apiError = null;
    try {
      getSecureDataVersionsController.getVersionPathsForSdb(10, 10);
    } catch (ApiException apiException) {
      apiError = apiException.getApiErrors().get(0);
    }
    Assert.assertEquals(DefaultApiError.GENERIC_BAD_REQUEST, apiError);
    Mockito.verify(auditLoggingFilterDetails)
        .setAction("Failed to find versions for secret with path: " + sdbAccessRequest.getPath());
  }

  @Test
  public void testGetVersionPaths() {
    SecureDataVersionsResult secureDataVersionsResult =
        Mockito.mock(SecureDataVersionsResult.class);
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    Mockito.when(sdbAccessRequest.getCategory()).thenReturn("category");
    List<SecureDataVersionSummary> secureDataVersionSummaries = new ArrayList<>();
    SecureDataVersionSummary secureDataVersionSummary =
        Mockito.mock(SecureDataVersionSummary.class);
    secureDataVersionSummaries.add(secureDataVersionSummary);
    Mockito.when(secureDataVersionsResult.getSecureDataVersionSummaries())
        .thenReturn(secureDataVersionSummaries);
    Mockito.when(
            secureDataVersionService.getSecureDataVersionSummariesByPath(
                "sdbId", "path", "category", 10, 10))
        .thenReturn(secureDataVersionsResult);
    SecureDataVersionsResult actualSecureDataVersionsResult =
        getSecureDataVersionsController.getVersionPathsForSdb(10, 10);
    Assert.assertSame(secureDataVersionsResult, actualSecureDataVersionsResult);
  }
}
