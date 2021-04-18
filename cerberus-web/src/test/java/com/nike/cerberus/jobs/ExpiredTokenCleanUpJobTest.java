package com.nike.cerberus.jobs;

import static org.junit.Assert.assertNull;

import com.nike.cerberus.service.AuthTokenService;
import com.nike.cerberus.service.DistributedLockService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ExpiredTokenCleanUpJobTest {

  private ExpiredTokenCleanUpJob expiredTokenCleanUpJob;
  @Mock private AuthTokenService authTokenService;

  @Mock private DistributedLockService jobCoordinatorService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    expiredTokenCleanUpJob = new ExpiredTokenCleanUpJob(authTokenService, 1, 1000, 1);
    expiredTokenCleanUpJob.setJobCoordinatorService(jobCoordinatorService);
  }

  @Test
  public void testExecute() {
    try {
      Mockito.when(jobCoordinatorService.acquireLock(ArgumentMatchers.anyString()))
          .thenReturn(false);
      expiredTokenCleanUpJob.execute();
    } catch (Exception exception) {
      assertNull(exception);
    }
  }

  @Test
  public void testexecuteLockableCode() {
    try {
      Mockito.when(
              authTokenService.deleteExpiredTokens(
                  ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
          .thenReturn(1);
      expiredTokenCleanUpJob.executeLockableCode();
    } catch (Exception exception) {
      assertNull(exception);
    }
  }
}
