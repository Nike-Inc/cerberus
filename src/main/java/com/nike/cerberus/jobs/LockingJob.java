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

package com.nike.cerberus.jobs;

import com.nike.cerberus.service.JobCoordinatorService;
import org.knowm.sundial.Job;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public abstract class LockingJob extends Job {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private JobCoordinatorService jobCoordinatorService;

    @Inject
    public void setJobCoordinatorService(JobCoordinatorService jobCoordinatorService) {
        this.jobCoordinatorService = jobCoordinatorService;
    }

    @Override
    public void doRun() throws JobInterruptException {
        String jobName = this.getJobContext().getJobName();
        if (! jobCoordinatorService.acquireLockToRunJob(jobName)) {
            log.info("Failed to acquire lock, another instance must be running the job. Job Name: {}", jobName);
            return;
        }

        log.info("Lock acquired for Job: {}, executing lockable code", jobName);
        try {
            executeLockableCode();
        } catch (Throwable t) {
            log.error("Failed to execute lockable job, releasing lock", t);
        }

        log.info("Attempting to release lock for Job: {}", jobName);
        boolean released = false;
        do {
            try {
                released = jobCoordinatorService.releaseLock(jobName);
                if (!released) { // Sometimes it takes multiple calls to release, why?
                    log.warn("Failed to release log, will retry after pause");
                    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                }
            } catch (InterruptedException | JobInterruptException e) {
                log.error("Pause interrupted while trying to release lock", e);
                throw new JobInterruptException();
            } catch (Throwable t) {
                log.error("Something went wrong trying to release lock retrying", t);
            }
        } while (! released);
        log.info("Lock released for Job: {}", jobName);
    }

    protected abstract void executeLockableCode();
}
