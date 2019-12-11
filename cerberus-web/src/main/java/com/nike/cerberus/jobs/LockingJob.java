/*
 * Copyright (c) 2019 Nike, Inc.
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

package com.nike.cerberus.jobs;

import com.nike.cerberus.service.DistributedLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public abstract class LockingJob {

    private DistributedLockService jobCoordinatorService;

    @Autowired
    public void setJobCoordinatorService(DistributedLockService jobCoordinatorService) {
        this.jobCoordinatorService = jobCoordinatorService;
    }

    public void execute() {
        String jobName = this.getClass().getName();
        if (! jobCoordinatorService.acquireLock(jobName)) {
            log.info("Failed to acquire lock, another instance must be running the job. Job Name: {}", jobName);
            return;
        }

        log.info("Lock acquired for Job: {}, executing lockable code", jobName);
        try {
            executeLockableCode();
        } catch (Throwable t) {
            log.error("Failed to execute lockable job, releasing lock", t);
        } finally {
            log.info("Attempting to release lock for Job: {}", jobName);
            boolean released = false;
            do {
                try {
                    released = jobCoordinatorService.releaseLock(jobName);
                    if (!released) { // Sometimes it takes multiple calls to release, why?
                        log.warn("Failed to release lock, will retry after pause");
                        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                    }
                } catch (InterruptedException e) {
                    log.error("Pause interrupted while trying to release lock", e);
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    log.error("Something went wrong trying to release lock retrying", t);
                }
            } while (! released);
            log.info("Lock released for Job: {}", jobName);
        }
    }

    protected abstract void executeLockableCode();
}
