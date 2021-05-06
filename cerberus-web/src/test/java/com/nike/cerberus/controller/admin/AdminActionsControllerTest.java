package com.nike.cerberus.controller.admin;

import com.nike.cerberus.domain.AuthKmsKeyMetadata;
import com.nike.cerberus.domain.AuthKmsKeyMetadataResult;
import com.nike.cerberus.domain.SDBMetadata;
import com.nike.cerberus.service.KmsService;
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
import org.springframework.security.core.Authentication;

public class AdminActionsControllerTest {

  @Mock private KmsService kmsService;
  @Mock private SafeDepositBoxService safeDepositBoxService;

  @InjectMocks private AdminActionsController adminActionsController;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetAuthKmsKeyMetadataWhenKmsServiceReturnsEmptyMetadata() {
    AuthKmsKeyMetadataResult authKmsKeyMetadata = adminActionsController.getAuthKmsKeyMetadata();
    Assert.assertNotNull(authKmsKeyMetadata);
    Assert.assertEquals(0, authKmsKeyMetadata.getAuthenticationKmsKeyMetadata().size());
  }

  @Test
  public void testGetAuthKmsKeyMetadataWhenKmsServiceReturnsNull() {
    Mockito.when(kmsService.getAuthenticationKmsMetadata()).thenReturn(null);
    AuthKmsKeyMetadataResult authKmsKeyMetadata = adminActionsController.getAuthKmsKeyMetadata();
    Assert.assertNotNull(authKmsKeyMetadata);
    Assert.assertNull(authKmsKeyMetadata.getAuthenticationKmsKeyMetadata());
  }

  @Test
  public void testGetAuthKmsKeyMetadataWhenKmsServiceReturnsListWithAuthenticationKmsData() {
    AuthKmsKeyMetadata authKmsKeyMetadata = new AuthKmsKeyMetadata();
    List<AuthKmsKeyMetadata> authKmsKeyMetadataList = new ArrayList<>();
    authKmsKeyMetadataList.add(authKmsKeyMetadata);
    Mockito.when(kmsService.getAuthenticationKmsMetadata()).thenReturn(authKmsKeyMetadataList);
    AuthKmsKeyMetadataResult authKmsKeyMetadataResult =
        adminActionsController.getAuthKmsKeyMetadata();
    Assert.assertNotNull(authKmsKeyMetadataResult);
    Assert.assertSame(
        authKmsKeyMetadataList, authKmsKeyMetadataResult.getAuthenticationKmsKeyMetadata());
    Assert.assertSame(
        authKmsKeyMetadata, authKmsKeyMetadataResult.getAuthenticationKmsKeyMetadata().get(0));
  }

  @Test
  public void testOverrideOwner() {
    SDBMetadata sdbMetadata = Mockito.mock(SDBMetadata.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    Mockito.when(sdbMetadata.getName()).thenReturn("name");
    Mockito.when(sdbMetadata.getOwner()).thenReturn("owner");
    Mockito.when(authentication.getName()).thenReturn("name");
    adminActionsController.overrideSdbOwner(sdbMetadata, authentication);
    Mockito.verify(safeDepositBoxService).overrideOwner("name", "owner", "name");
  }
}
