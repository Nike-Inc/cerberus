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
 *
 */

package com.nike.cerberus.jobs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nike.cerberus.service.JwtService;
import org.knowm.sundial.Job;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically refresh JWT blacklist.
 */
@Singleton
public class JwtBlacklistRefreshJob extends Job {

    private static final Logger log = LoggerFactory.getLogger(JwtBlacklistRefreshJob.class);

    private final JwtService jwtService;

    @Inject
    public JwtBlacklistRefreshJob(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void doRun() throws JobInterruptException {
        log.debug("Running JWT blacklist refresh job");
        try {
            jwtService.refreshBlacklist();
        } catch (JobInterruptException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error refreshing JWT blacklist", e);
        }
    }
}
