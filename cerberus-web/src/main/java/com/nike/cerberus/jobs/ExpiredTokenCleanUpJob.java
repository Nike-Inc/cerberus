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

import com.nike.cerberus.service.AuthTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@ConditionalOnProperty("cerberus.jobs.expiredTokenCleanUpJob.enabled")
@Component
public class ExpiredTokenCleanUpJob extends LockingJob {

  private final AuthTokenService authTokenService;
  private final int maxNumberOfTokensToDeletePerJobRun;
  private final int numberOfTokensToDeletePerBatch;
  private final int batchPauseTimeInMillis;

  @Autowired
  public ExpiredTokenCleanUpJob(
      AuthTokenService authTokenService,
      @Value("${cerberus.jobs.expiredTokenCleanUpJob.maxNumberOfTokensToDeletePerJobRun}")
          int maxNumberOfTokensToDeletePerJobRun,
      @Value("${cerberus.jobs.expiredTokenCleanUpJob.numberOfTokensToDeletePerBatch}")
          int numberOfTokensToDeletePerBatch,
      @Value("${cerberus.jobs.expiredTokenCleanUpJob.batchPauseTimeInMillis}")
          int batchPauseTimeInMillis) {

    this.authTokenService = authTokenService;
    this.maxNumberOfTokensToDeletePerJobRun = maxNumberOfTokensToDeletePerJobRun;
    this.numberOfTokensToDeletePerBatch = numberOfTokensToDeletePerBatch;
    this.batchPauseTimeInMillis = batchPauseTimeInMillis;
  }

  @Override
  @Scheduled(cron = "${cerberus.jobs.expiredTokenCleanUpJob.cronExpression}")
  public void execute() {
    super.execute();
  }

  @Override
  protected void executeLockableCode() {
    int numberOfDeletedTokens =
        authTokenService.deleteExpiredTokens(
            maxNumberOfTokensToDeletePerJobRun,
            numberOfTokensToDeletePerBatch,
            batchPauseTimeInMillis);
    log.info("Deleted {} tokens", numberOfDeletedTokens);
  }
}
