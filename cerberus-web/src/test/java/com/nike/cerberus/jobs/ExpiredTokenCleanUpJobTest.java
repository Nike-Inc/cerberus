package com.nike.cerberus.jobs;

import com.nike.cerberus.service.AuthTokenService;
import com.nike.cerberus.service.DistributedLockService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ExpiredTokenCleanUpJobTest {

  @Mock private AuthTokenService authTokenService;

  @Mock private DistributedLockService jobCoordinatorService;

  private ExpiredTokenCleanUpJob expiredTokenCleanUpJob;

  private String lockName = ExpiredTokenCleanUpJob.class.getName();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    expiredTokenCleanUpJob = new ExpiredTokenCleanUpJob(authTokenService, 10, 10, 1000);
    expiredTokenCleanUpJob.setJobCoordinatorService(jobCoordinatorService);
  }

  @Test
  public void testExecute() {
    expiredTokenCleanUpJob.execute();
    Mockito.verify(jobCoordinatorService).acquireLock(lockName);
    Mockito.verify(authTokenService, Mockito.never()).deleteExpiredTokens(10, 10, 1000);
    Mockito.verify(jobCoordinatorService, Mockito.never()).releaseLock(lockName);
  }

  @Test
  public void testExecuteLockableCode() {
    expiredTokenCleanUpJob.executeLockableCode();
    Mockito.verify(authTokenService).deleteExpiredTokens(10, 10, 1000);
  }
}
