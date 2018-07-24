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
 *
 */

package com.nike.cerberus.jobs;

import com.nike.cerberus.service.CleanUpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Scans through the data store and deletes in-active KMS CMKs
 */
public class InactiveKmsKeyCleanUpJob extends LockingJob {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CleanUpService cleanUpService;

    private final int expirationPeriodInDays;

    private final int pauseTimeInSeconds;

    @Inject
    public InactiveKmsKeyCleanUpJob(CleanUpService cleanUpService,
                                    @Named("cms.jobs.KmsCleanUpJob.deleteKmsKeysOlderThanNDays")
                                    int expirationPeriodInDays,
                                    @Named("cms.jobs.KmsCleanUpJob.batchPauseTimeInSeconds")
                                    int pauseTimeInSeconds) {

        this.cleanUpService = cleanUpService;
        this.expirationPeriodInDays = expirationPeriodInDays;
        this.pauseTimeInSeconds = pauseTimeInSeconds;
    }

    @Override
    protected void executeLockableCode() {
        logger.info("Starting KMS clean up...");
        int numKmsKeysCleanedUp = cleanUpService.cleanUpInactiveAndOrphanedKmsKeys(expirationPeriodInDays, pauseTimeInSeconds);
        logger.info("Cleaned up {} KMS keys...", numKmsKeysCleanedUp);
    }
}
