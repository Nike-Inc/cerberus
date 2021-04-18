package com.nike.cerberus.jobs;

import static org.junit.Assert.assertNull;

import com.nike.cerberus.service.DistributedLockService;
import com.nike.cerberus.service.SecureDataService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class DataKeyRotationJobTest {
  @Mock private SecureDataService secureDataService;
  private DataKeyRotationJob dataKeyRotationJob;
  @Mock private DistributedLockService jobCoordinatorService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    dataKeyRotationJob = new DataKeyRotationJob(secureDataService, 1, 1, 1);
    dataKeyRotationJob.setJobCoordinatorService(jobCoordinatorService);
  }

  @Test
  public void testExecute() {
    try {
      Mockito.when(jobCoordinatorService.acquireLock(ArgumentMatchers.anyString()))
          .thenReturn(false);
      dataKeyRotationJob.execute();
    } catch (Exception exception) {
      assertNull(exception);
    }
  }

  @Test
  public void testExecuteacquireLockTrue() {
    try {
      Mockito.when(jobCoordinatorService.acquireLock(ArgumentMatchers.anyString()))
          .thenReturn(true);
      Mockito.doNothing()
          .when(secureDataService)
          .rotateDataKeys(
              ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt());
      Mockito.when(jobCoordinatorService.releaseLock(ArgumentMatchers.anyString()))
          .thenReturn(true);
      dataKeyRotationJob.execute();
    } catch (Exception e) {
      assertNull(e);
    }
  }

  @Test
  public void testExecuteLockableCode() {
    try {
      Mockito.doNothing()
          .when(secureDataService)
          .rotateDataKeys(
              ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt());
      dataKeyRotationJob.executeLockableCode();
    } catch (Exception exception) {
      assertNull(exception);
    }
  }
}
