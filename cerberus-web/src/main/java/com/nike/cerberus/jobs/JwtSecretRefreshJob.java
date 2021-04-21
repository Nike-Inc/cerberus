/*
 * Copyright (c) 2021 Nike, Inc.
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

import com.nike.cerberus.service.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically refresh JWT signing keys. */
@Slf4j
@ConditionalOnProperty("cerberus.jobs.jwtSecretRefreshJob.enabled")
@Component
public class JwtSecretRefreshJob {

  private final JwtService jwtService;

  @Autowired
  public JwtSecretRefreshJob(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Scheduled(cron = "${cerberus.jobs.jwtSecretRefreshJob.cronExpression}")
  public void execute() {
    log.info("Running JWT secret refresh job");
    jwtService.refreshKeys();
  }
}
