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

import com.nike.cerberus.service.AuthTokenService;
import com.nike.cerberus.service.JobCoordinatorService;
import org.knowm.sundial.Job;
import org.knowm.sundial.SundialJobScheduler;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class ExpiredTokenCleanUpJob extends Job {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AuthTokenService authTokenService;
    private final JobCoordinatorService jobCoordinatorService;

    @Inject
    public ExpiredTokenCleanUpJob(AuthTokenService authTokenService,
                                  JobCoordinatorService jobCoordinatorService) {

        this.authTokenService = authTokenService;
        this.jobCoordinatorService = jobCoordinatorService;
    }

    @Override
    public void doRun() throws JobInterruptException {
        String jobName = this.getJobContext().getJobName();
        if (! jobCoordinatorService.acquireLockToRunJob(jobName)) {
            log.info("Failed to acquire lock, exiting job");
            return;
        }

        try {
            log.info("Running expired token clean up job");
            int numberOfDeletedTokens = authTokenService.deleteExpiredTokens(250000, 1000);
            log.info("Finished Running expired token clean up Job. Deleted {} tokens", numberOfDeletedTokens);
        } catch (Throwable t) {
            log.error("Failed deleting expired tokens", t);
        } finally {
            log.info("Releasing lock");
            boolean released;
            do {
                released = jobCoordinatorService.releaseLock(jobName);
                log.info("Lock released: {}", released);
            } while (! released);
        }
    }
}
