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

import com.nike.cerberus.service.CleanUpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Scans through the data store and deletes in-active KMS CMKs */
@Slf4j
@Component
public class InactiveKmsKeyCleanUpJob extends LockingJob {

  private final CleanUpService cleanUpService;

  private final int expirationPeriodInDays;

  private final int pauseTimeInSeconds;

  @Autowired
  public InactiveKmsKeyCleanUpJob(
      CleanUpService cleanUpService,
      @Value("${cerberus.jobs.inactiveKmsCleanUpJob.deleteKmsKeysOlderThanNDays}")
          int expirationPeriodInDays,
      @Value("${cerberus.jobs.inactiveKmsCleanUpJob.batchPauseTimeInSeconds}")
          int pauseTimeInSeconds) {

    this.cleanUpService = cleanUpService;
    this.expirationPeriodInDays = expirationPeriodInDays;
    this.pauseTimeInSeconds = pauseTimeInSeconds;
  }

  @Override
  @Scheduled(cron = "${cerberus.jobs.inactiveKmsCleanUpJob.cronExpression}")
  public void execute() {
    super.execute();
  }

  @Override
  protected void executeLockableCode() {
    log.info("Starting KMS clean up...");
    int numKmsKeysCleanedUp =
        cleanUpService.cleanUpInactiveAndOrphanedKmsKeys(
            expirationPeriodInDays, pauseTimeInSeconds);
    log.info("Cleaned up {} KMS keys...", numKmsKeysCleanedUp);
  }
}
