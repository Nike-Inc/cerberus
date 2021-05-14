package com.nike.cerberus.jobs;

import com.nike.cerberus.service.DistributedLockService;
import com.nike.cerberus.service.SecureDataService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class DataKeyRotationJobTest {

  @Mock private SecureDataService secureDataService;

  @Mock private DistributedLockService jobCoordinatorService;

  private DataKeyRotationJob dataKeyRotationJob;
  private String lockName = DataKeyRotationJob.class.getName();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    dataKeyRotationJob = new DataKeyRotationJob(secureDataService, 10, 1000, 1);
    dataKeyRotationJob.setJobCoordinatorService(jobCoordinatorService);
  }

  @Test
  public void testExecuteLockableCode() {
    dataKeyRotationJob.executeLockableCode();
    Mockito.verify(secureDataService).rotateDataKeys(10, 1000, 1);
  }

  @Test
  public void testExecuteWhenJobCoordinatorServiceDoesNotAcquireLock() {
    dataKeyRotationJob.execute();
    Mockito.verify(jobCoordinatorService).acquireLock(lockName);
    Mockito.verify(secureDataService, Mockito.never()).rotateDataKeys(10, 1000, 1);
    Mockito.verify(jobCoordinatorService, Mockito.never()).releaseLock(lockName);
  }

  @Test
  public void testExecuteWhenJobCoordinatorServiceAcquireLock() {
    Mockito.when(jobCoordinatorService.acquireLock(lockName)).thenReturn(true);
    Mockito.when(jobCoordinatorService.releaseLock(lockName)).thenReturn(false, true);
    dataKeyRotationJob.execute();
    Mockito.verify(jobCoordinatorService).acquireLock(lockName);
    Mockito.verify(secureDataService).rotateDataKeys(10, 1000, 1);
    Mockito.verify(jobCoordinatorService, Mockito.times(2)).releaseLock(lockName);
  }
}
