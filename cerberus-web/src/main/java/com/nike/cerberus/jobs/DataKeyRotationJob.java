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

import com.nike.cerberus.service.SecureDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataKeyRotationJob extends LockingJob {

  private final SecureDataService secureDataService;
  private final int numberOfDataKeyToRotatePerJobRun;
  private final int dataKeyRotationPauseTimeInMillis;
  private final int dataKeyRotationIntervalInDays;

  @Autowired
  public DataKeyRotationJob(
      SecureDataService secureDataService,
      @Value("${cerberus.jobs.dataKeyRotationJob.numberOfDataKeyToRotatePerJobRun}")
          int numberOfDataKeyToRotatePerJobRun,
      @Value("${cerberus.jobs.dataKeyRotationJob.dataKeyRotationPauseTimeInMillis}")
          int dataKeyRotationPauseTimeInMillis,
      @Value("${cerberus.jobs.dataKeyRotationJob.dataKeyRotationIntervalInDays}")
          int dataKeyRotationIntervalInDays) {

    this.secureDataService = secureDataService;
    this.numberOfDataKeyToRotatePerJobRun = numberOfDataKeyToRotatePerJobRun;
    this.dataKeyRotationPauseTimeInMillis = dataKeyRotationPauseTimeInMillis;
    this.dataKeyRotationIntervalInDays = dataKeyRotationIntervalInDays;
  }

  @Override
  @Scheduled(cron = "${cerberus.jobs.dataKeyRotationJob.cronExpression}")
  public void execute() {
    super.execute();
  }

  @Override
  protected void executeLockableCode() {
    secureDataService.rotateDataKeys(
        numberOfDataKeyToRotatePerJobRun,
        dataKeyRotationPauseTimeInMillis,
        dataKeyRotationIntervalInDays);
    log.info("Rotated {} keys", numberOfDataKeyToRotatePerJobRun);
  }
}
