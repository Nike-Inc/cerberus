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


import com.nike.cerberus.service.SecureDataService;

import javax.inject.Inject;
import javax.inject.Named;

public class DataKeyRotationJob extends LockingJob {

    private final SecureDataService secureDataService;
    private final int numberOfDataKeyToRotatePerJobRun;
    private final int dataKeyRotationPauseTimeInMillis;
    private final int dataKeyRotationIntervalInDays;

    @Inject
    public DataKeyRotationJob(SecureDataService secureDataService,
                              @Named("cms.jobs.DataKeyRotationJob.numberOfDataKeyToRotatePerJobRun")
                                      int numberOfDataKeyToRotatePerJobRun,
                              @Named("cms.jobs.DataKeyRotationJob.dataKeyRotationPauseTimeInMillis")
                                          int dataKeyRotationPauseTimeInMillis,
                              @Named("cms.jobs.DataKeyRotationJob.dataKeyRotationIntervalInDays")
                                      int dataKeyRotationIntervalInDays) {

        this.secureDataService = secureDataService;
        this.numberOfDataKeyToRotatePerJobRun = numberOfDataKeyToRotatePerJobRun;
        this.dataKeyRotationPauseTimeInMillis = dataKeyRotationPauseTimeInMillis;
        this.dataKeyRotationIntervalInDays = dataKeyRotationIntervalInDays;
    }


    @Override
    protected void executeLockableCode() {
        secureDataService.rotateDataKeys(numberOfDataKeyToRotatePerJobRun, dataKeyRotationPauseTimeInMillis, dataKeyRotationIntervalInDays);
    }
}
