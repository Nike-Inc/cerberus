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

import com.nike.cerberus.service.JwtService;

import javax.inject.Inject;

/**
 * Periodically clean up JWT blacklist.
 */
public class JwtBlacklistCleanUpJob extends LockingJob {

    private final JwtService jwtService;

    @Inject
    public JwtBlacklistCleanUpJob(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void executeLockableCode() {
        int numberOfDeletedTokens = jwtService.deleteExpiredTokens();
        log.info("Deleted {} JWT blacklist entries", numberOfDeletedTokens);
    }
}
