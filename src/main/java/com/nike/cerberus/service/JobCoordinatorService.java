/*
 * Copyright (c) 2017 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.service;

import com.nike.cerberus.mapper.LockMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class JobCoordinatorService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final SqlSessionFactory sqlSessionFactory;

    private Map<String, Lock> locks = new HashMap<>();

    @Inject
    public JobCoordinatorService(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public boolean acquireLockToRunJob(String jobName) {
        log.debug("Attempting to acquire lock for {}", jobName);
        Lock lock = new Lock(jobName);
        Thread thread = new Thread(lock);
        thread.start();
        locks.put(jobName, lock);

        lock.semaphore.acquireUninterruptibly();
        lock.semaphore.release();

        boolean didAcquireLock = lock.isLocked;

        if (! didAcquireLock) {
            log.error("Failed to acquire lock, returning false.");
            locks.remove(jobName);
        }
        return didAcquireLock;
    }

    public boolean releaseLock(String jobName) {
        Lock lock = locks.get(jobName);
        lock.release();
        lock.semaphore.acquireUninterruptibly();
        lock.semaphore.release();

        if (lock.didRelease) {
            locks.remove(jobName);
        }

        return lock.didRelease;
    }

    class Lock implements Runnable {
        Semaphore semaphore = new Semaphore(1);
        AtomicBoolean shouldKeepLock = new AtomicBoolean(false);
        boolean isLocked = false;
        boolean didRelease = false;
        private final String name;

        Lock(String name) {
            semaphore.acquireUninterruptibly();
            this.name = name;
        }

        public void release() {
            semaphore.acquireUninterruptibly();
            shouldKeepLock.set(false);
        }

        @Override
        public void run() {
            log.debug("Attempting to get lock");

            try (SqlSession session = sqlSessionFactory.openSession(false)) {

                LockMapper lockMapper = session.getMapper(LockMapper.class);

                Integer lockStatus = lockMapper.getLock(name);
                isLocked = lockStatus > 0;
                log.debug("Lock acquire for name: {} res: {}", name, lockStatus);
                if (isLocked) {
                    shouldKeepLock.set(true);
                }
                semaphore.release();

                while (shouldKeepLock.get()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        log.error("Failed to sleep", e);
                        shouldKeepLock.set(false);
                        semaphore.release();
                    }
                }
                if (isLocked) {
                    do {
                        log.debug("Attempting to release lock");
                        Integer releaseStatus = lockMapper.releaseLock(name);
                        didRelease = releaseStatus > 0;
                        if (!didRelease) {
                            try {
                                log.warn("Failed to release lock for {}, retrying, status: {}", name, releaseStatus);
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                log.error("Failed to sleep", e);
                                break;
                            }
                        }
                    } while (!didRelease);
                    log.debug("Lock released");
                }
                session.commit();
            } finally {
                semaphore.release();
            }
        }
    }
}
