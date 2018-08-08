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

import com.google.common.collect.ImmutableMap;
import com.nike.cerberus.mapper.LockMapper;
import com.nike.riposte.server.config.ServerConfig;
import com.nike.riposte.server.hooks.ServerShutdownHook;
import io.netty.channel.Channel;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This service provides the needed functionality to acquire and release a named lock from the data store in an independent transaction.
 * If you can lock and do work in a single transaction then using the LockDao directly is more efficient.
 */
public class DistributedLockService implements ServerShutdownHook {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String LOCK_ACQUIRED = "cms.lock-acquired";
    private static final String LOCK_ACQUIRE_FAILURE = "cms.lock-acquire-failure";
    private static final String LOCK_RELEASED = "cms.lock-released";
    private static final String LOCK_RELEASE_FAILURE = "cms.lock-release-failure";

    private final SqlSessionFactory sqlSessionFactory;
    private final MetricsService metricsService;

    private Map<String, Lock> locks = new HashMap<>();

    @Inject
    public DistributedLockService(SqlSessionFactory sqlSessionFactory,
                                  MetricsService metricsService) {

        this.sqlSessionFactory = sqlSessionFactory;
        this.metricsService = metricsService;
    }

    /**
     * This method acquires a distributed named lock.
     *
     * @param lockName The named lock that should try to be acquired.
     * @return true if the lock was successfully acquired.
     */
    public boolean acquireLock(String lockName) {
        log.debug("Attempting to acquire lock for {}", lockName);

        // Create a lock thread and start it.
        Lock lock = new Lock(lockName);
        Thread thread = new Thread(lock);
        thread.start();
        locks.put(lockName, lock);
        lock.lockThread = thread;

        // wait for the lock to signal that it finished attempting to acquire the lock
        lock.semaphore.acquireUninterruptibly();
        lock.semaphore.release();

        // check to see if the lock was acquired
        boolean didAcquireLock = lock.isLocked;

        if (didAcquireLock) {
            metricsService.getOrCreateCounter(LOCK_ACQUIRED, ImmutableMap.of("lock-name", lockName)).inc();
        } else {
            // if the lock wasn't acquired clear the thread and lock so it can be garbage collected
            locks.remove(lockName);
            log.error("Failed to acquire lock, returning false.");
            metricsService.getOrCreateCounter(LOCK_ACQUIRE_FAILURE, ImmutableMap.of("lock-name", lockName)).inc();
        }

        // return the result
        return didAcquireLock;
    }

    /**
     * Releases a named lock, it is not required to have the lock to call this method.
     *
     * @param lockName The lock name that was acquired.
     * @return true if the lock was released or this instance didn't have the lock to release.
     */
    public boolean releaseLock(String lockName) {
        // If this service doesn't have the requested named lock then return true.
        if (!locks.containsKey(lockName)) {
            return true;
        }

        // Get the lock and tell it to release
        Lock lock = locks.get(lockName);
        lock.release();

        // wait for the lock to signal that it has finished releasing.
        lock.semaphore.acquireUninterruptibly();
        lock.semaphore.release();

        // if the lock was released clean up the objects for garbage collection
        if (lock.didRelease) {
            metricsService.getOrCreateCounter(LOCK_RELEASED, ImmutableMap.of("lock-name", lockName)).inc();
            locks.remove(lockName);
        }

        // return the status
        return lock.didRelease;
    }

    /**
     * Shutdown hook to release locks and kill running threads.
     * Explicitly releasing the locks is not required, as the mysql will release the lock when the connection is closed.
     * However this will kill the threads which might try to keep the Java process alive during a graceful shutdown.
     */
    @Override
    public void executeServerShutdownHook(ServerConfig serverConfig, Channel channel) {
        log.info("Received shutdown hook, attempting to shutdown gracefully");
        locks.forEach((name, lock) -> {
            try {
                lock.release();
                lock.semaphore.tryAcquire(3, TimeUnit.SECONDS);
                locks.remove(name);
            } catch (InterruptedException e) {
                log.error("Failed to gracefully release lock: {}, interrupting thread", e);
                lock.interrupt();
            }
        });
        log.info("Shutdown hook finished");
    }

    /**
     * A simple class that can acquire, keep and release a lock in a single thread / mysql transaction.
     */
    class Lock implements Runnable {
        private Thread lockThread;
        private Semaphore semaphore = new Semaphore(1);
        private AtomicBoolean shouldKeepLock = new AtomicBoolean(false);
        private boolean isLocked = false;
        private boolean didRelease = false;
        private final String name;

        Lock(String name) {
            semaphore.acquireUninterruptibly();
            this.name = name;
        }

        public void release() {
            semaphore.acquireUninterruptibly();
            shouldKeepLock.set(false);
        }

        public void interrupt() {
            if (lockThread != null && lockThread.isAlive()) {
                lockThread.interrupt();
            }
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
                                metricsService.getOrCreateCounter(LOCK_RELEASE_FAILURE, ImmutableMap.of("lock-name", name)).inc();
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
