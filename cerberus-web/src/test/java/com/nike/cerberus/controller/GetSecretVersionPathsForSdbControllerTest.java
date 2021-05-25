package com.nike.cerberus.controller;

import com.nike.cerberus.service.SafeDepositBoxService;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class GetSecretVersionPathsForSdbControllerTest {

  @Mock private SafeDepositBoxService safeDepositBoxService;

  @InjectMocks private GetSecretVersionPathsForSdbController getSecretVersionPathsForSdbController;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetVersionPathsForSdb() {
    Set<String> paths = new HashSet<>();
    paths.add("path1");
    paths.add("path2");
    Mockito.when(safeDepositBoxService.getSecureDataVersionPathsForSdb("sdbId")).thenReturn(paths);
    Set<String> actualPaths = getSecretVersionPathsForSdbController.getVersionPathsForSdb("sdbId");
    Assert.assertSame(paths, actualPaths);
    Assert.assertEquals(paths, actualPaths);
  }
}
