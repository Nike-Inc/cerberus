package com.nike.cerberus.dao;

import com.nike.cerberus.mapper.LockMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class LockDaoTest {

  @Mock private LockMapper lockMapper;

  @InjectMocks private LockDao lockDao;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetLock() {
    Mockito.when(lockMapper.getLock("lock")).thenReturn(6);
    Integer lock = lockDao.getLock("lock");
    Assert.assertEquals(Integer.valueOf(6), lock);
    Mockito.verify(lockMapper).getLock("lock");
  }

  @Test
  public void testReleaseLock() {
    Mockito.when(lockMapper.releaseLock("lock")).thenReturn(6);
    Integer lock = lockDao.releaseLock("lock");
    Assert.assertEquals(Integer.valueOf(6), lock);
    Mockito.verify(lockMapper).releaseLock("lock");
  }
}
