package com.nike.cerberus.service;

import com.codahale.metrics.Counter;
import com.nike.cerberus.mapper.LockMapper;
import com.nike.cerberus.metric.MetricsService;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DistributedLockServiceTest {

    private static final String JOB_NAME = "the-job-name";

    @Mock
    private SqlSessionFactory sqlSessionFactory;

    @Mock
    private SqlSession sqlSession;

    @Mock
    private LockMapper lockMapper;

    @Mock
    private MetricsService metricsService;

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
