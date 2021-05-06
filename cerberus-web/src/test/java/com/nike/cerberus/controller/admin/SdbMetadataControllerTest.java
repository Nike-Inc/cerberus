package com.nike.cerberus.controller.admin;

import com.nike.cerberus.domain.SDBMetadata;
import com.nike.cerberus.domain.SDBMetadataResult;
import com.nike.cerberus.service.MetadataService;
import com.nike.cerberus.service.SafeDepositBoxService;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

public class SdbMetadataControllerTest {

  @Mock private MetadataService metadataService;

  @Mock private SafeDepositBoxService safeDepositBoxService;

  @InjectMocks private SdbMetadataController sdbMetadataController;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetMetadata() {
    SDBMetadataResult sdbMetadataResultMock = Mockito.mock(SDBMetadataResult.class);
    Mockito.when(metadataService.getSDBMetadata(1, 2, "sdbNameFilter"))
        .thenReturn(sdbMetadataResultMock);
    SDBMetadataResult sdbMetadataResult = sdbMetadataController.getMetadata(1, 2, "sdbNameFilter");
    Assert.assertSame(sdbMetadataResultMock, sdbMetadataResult);
  }

  @Test
  public void testRestoreSdbIncludingDataInRequest() {
    SDBMetadata sdbMetadata = Mockito.mock(SDBMetadata.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    Mockito.when(authentication.getName()).thenReturn("name");
    Mockito.when(sdbMetadata.getPath()).thenReturn("path");
    Mockito.when(safeDepositBoxService.getSafeDepositBoxIdByPath("path"))
        .thenReturn(Optional.of("id"));
    sdbMetadataController.restoreSdbIncludingDataInRequest(sdbMetadata, authentication);
    Mockito.verify(safeDepositBoxService).deleteSafeDepositBox("id");
    Mockito.verify(metadataService).restoreMetadata(sdbMetadata, "name");
  }

  @Test
  public void testRestoreSdbIncludingDataInRequestIfSafeBoIdIsNotPresent() {
    SDBMetadata sdbMetadata = Mockito.mock(SDBMetadata.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    Mockito.when(authentication.getName()).thenReturn("name");
    Mockito.when(sdbMetadata.getPath()).thenReturn("path");
    Mockito.when(safeDepositBoxService.getSafeDepositBoxIdByPath("path"))
        .thenReturn(Optional.empty());
    sdbMetadataController.restoreSdbIncludingDataInRequest(sdbMetadata, authentication);
    Mockito.verify(safeDepositBoxService, Mockito.never())
        .deleteSafeDepositBox(Mockito.anyString());
    Mockito.verify(metadataService).restoreMetadata(sdbMetadata, "name");
  }
}
