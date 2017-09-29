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
import org.knowm.sundial.Job;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class ExpiredTokenCleanUpJob extends Job {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AuthTokenService authTokenService;

    @Inject
    public ExpiredTokenCleanUpJob(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @Override
    public void doRun() throws JobInterruptException {
        log.info("Running expired token clean up job");
        int numberOfDeletedTokens = authTokenService.deleteExpiredTokens();
        log.info("Finished Running expired token clean up Job. Deleted {} tokens", numberOfDeletedTokens);
    }
}
