package com.nike.cerberus.controller;

import com.nike.cerberus.domain.SecureFileSummaryResult;
import com.nike.cerberus.service.SecureDataService;
import com.nike.cerberus.util.SdbAccessRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class SecureFilesSummaryControllerTest {

  @Mock private SecureDataService secureDataService;
  @Mock private SdbAccessRequest sdbAccessRequest;
  @InjectMocks private SecureFilesSummaryController secureFilesSummaryController;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testListSecureFiles() {
    SecureFileSummaryResult secureFileSummaryResult = Mockito.mock(SecureFileSummaryResult.class);
    Mockito.when(sdbAccessRequest.getSdbId()).thenReturn("sdbId");
    Mockito.when(sdbAccessRequest.getPath()).thenReturn("path");
    Mockito.when(secureDataService.listSecureFilesSummaries("sdbId", "path", 10, 10))
        .thenReturn(secureFileSummaryResult);
    SecureFileSummaryResult actualSecureFileSummaryResult =
        secureFilesSummaryController.listSecureFiles(10, 10);
    Assert.assertSame(secureFileSummaryResult, actualSecureFileSummaryResult);
  }
}
