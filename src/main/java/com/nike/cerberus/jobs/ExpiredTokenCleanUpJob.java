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

import javax.inject.Inject;
import javax.inject.Named;

public class ExpiredTokenCleanUpJob extends LockingJob {

    private final AuthTokenService authTokenService;
    private final int maxNumberOfTokensToDeletePerJobRun;
    private final int numberOfTokensToDeletePerBatch;
    private final int batchPauseTimeInMillis;

    @Inject
    public ExpiredTokenCleanUpJob(AuthTokenService authTokenService,
                                  @Named("cms.jobs.ExpiredTokenCleanUpJob.maxNumberOfTokensToDeletePerJobRun")
                                          int maxNumberOfTokensToDeletePerJobRun,
                                  @Named("cms.jobs.ExpiredTokenCleanUpJob.numberOfTokensToDeletePerBatch")
                                          int numberOfTokensToDeletePerBatch,
                                  @Named("cms.jobs.ExpiredTokenCleanUpJob.batchPauseTimeInMillis")
                                          int batchPauseTimeInMillis) {

        this.authTokenService = authTokenService;
        this.maxNumberOfTokensToDeletePerJobRun = maxNumberOfTokensToDeletePerJobRun;
        this.numberOfTokensToDeletePerBatch = numberOfTokensToDeletePerBatch;
        this.batchPauseTimeInMillis = batchPauseTimeInMillis;
    }

    @Override
    protected void executeLockableCode() {
        int numberOfDeletedTokens = authTokenService.deleteExpiredTokens(maxNumberOfTokensToDeletePerJobRun,
                numberOfTokensToDeletePerBatch, batchPauseTimeInMillis);
        log.info("Deleted {} tokens", numberOfDeletedTokens);
    }
}
