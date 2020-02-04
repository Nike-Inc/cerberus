/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.service;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.codahale.metrics.Counter;
import com.nike.cerberus.mapper.LockMapper;
import com.nike.cerberus.metric.MetricsService;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DistributedLockServiceTest {

  private static final String JOB_NAME = "the-job-name";

  @Mock private SqlSessionFactory sqlSessionFactory;

  @Mock private SqlSession sqlSession;

  @Mock private LockMapper lockMapper;

  @Mock private MetricsService metricsService;

  private DistributedLockService jobCoordinatorService;

  @Before
  public void before() {
    initMocks(this);
    when(sqlSessionFactory.openSession(false)).thenReturn(sqlSession);
    when(sqlSession.getMapper(LockMapper.class)).thenReturn(lockMapper);
    when(metricsService.getOrCreateCounter(anyString(), anyMap())).thenReturn(new Counter());
    jobCoordinatorService = new DistributedLockService(sqlSessionFactory, metricsService);
  }

  @Test
  public void test_that_a_lock_can_be_acquired_and_released_happy_path() {
    when(lockMapper.getLock(JOB_NAME)).thenReturn(1);
    boolean acquired = jobCoordinatorService.acquireLock(JOB_NAME);
    assertTrue(acquired);

    when(lockMapper.releaseLock(JOB_NAME)).thenReturn(1);
    boolean wasReleased = jobCoordinatorService.releaseLock(JOB_NAME);
    assertTrue(wasReleased);
  }

  @Test
  public void test_that_acquireLockToRunJob_returns_false_cleanly_when_lock_cannot_be_acquired() {
    when(lockMapper.getLock(JOB_NAME)).thenReturn(0);
    boolean acquired = jobCoordinatorService.acquireLock(JOB_NAME);
    assertFalse(acquired);

    when(lockMapper.getLock(JOB_NAME)).thenReturn(1);
    acquired = jobCoordinatorService.acquireLock(JOB_NAME);
    assertTrue(acquired);
  }

  @Test
  public void test_that_release_lock_retries() {
    when(lockMapper.getLock(JOB_NAME)).thenReturn(1);
    boolean acquired = jobCoordinatorService.acquireLock(JOB_NAME);
    assertTrue(acquired);

    when(lockMapper.releaseLock(JOB_NAME)).thenReturn(0).thenReturn(1);
    boolean wasReleased = jobCoordinatorService.releaseLock(JOB_NAME);
    assertTrue(wasReleased);
    verify(lockMapper, times(2)).releaseLock(JOB_NAME);
  }
}
