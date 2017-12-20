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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.service.MetricsService;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.cerberus.service.SecureDataService;
import com.nike.cerberus.service.UserGroupPermissionService;
import org.knowm.sundial.Job;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically send KPI metrics to the enabled metrics services.
 */
@Singleton
public class KpiMetricsProcessingJob extends Job {

    private static final Logger log = LoggerFactory.getLogger(KpiMetricsProcessingJob.class);

    private final MetricsService metricsService;

    private final SafeDepositBoxService safeDepositBoxService;

    private final AwsIamRoleDao awsIamRoleDao;

    private final SecureDataService secureDataService;

    private final UserGroupPermissionService userGroupPermissionService;

    @Inject
    public KpiMetricsProcessingJob(MetricsService metricsService,
                                   SafeDepositBoxService safeDepositBoxService,
                                   AwsIamRoleDao awsIamRoleDao,
                                   SecureDataService secureDataService,
                                   UserGroupPermissionService userGroupPermissionService) {
        this.metricsService = metricsService;
        this.safeDepositBoxService = safeDepositBoxService;
        this.awsIamRoleDao = awsIamRoleDao;
        this.secureDataService = secureDataService;
        this.userGroupPermissionService = userGroupPermissionService;
    }

    @Override
    public void doRun() throws JobInterruptException {
        log.debug("Running KPI metrics processing job");
        try {
            processKpiMetrics();
        } catch (JobInterruptException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error processing KPI metrics", e);
        }
    }

    public void processKpiMetrics() {
        int numUniqueIamRoles = awsIamRoleDao.getTotalNumberOfUniqueIamRoles();
        int numUniqueOwnerGroups = userGroupPermissionService.getTotalNumUniqueOwnerGroups();
        int numUniqueNonOwnerGroups = userGroupPermissionService.getTotalNumUniqueNonOwnerGroups();
        int totalUniqueUserGroups = userGroupPermissionService.getTotalNumUniqueUserGroups();
        int numSDBs = safeDepositBoxService.getTotalNumberOfSafeDepositBoxes();
        int numDataNodes = secureDataService.getTotalNumberOfDataNodes();
        int numKeyValuePairs = secureDataService.getTotalNumberOfKeyValuePairs();

        log.info("Number of IAM roles: {}, Owner Groups: {}, Non-Owner Groups: {}, Total Unique Groups: {}, SDBs: {}, Nodes: {}, Key/Value Pairs: {}",
                numUniqueIamRoles,
                numUniqueOwnerGroups,
                numUniqueNonOwnerGroups,
                totalUniqueUserGroups,
                numSDBs,
                numDataNodes,
                numKeyValuePairs);

        metricsService.setGaugeValue("numberOfUniqueIamRoles", numUniqueIamRoles);
        metricsService.setGaugeValue("numberOfUniqueOwnerGroups", numUniqueOwnerGroups);
        metricsService.setGaugeValue("numberOfUniqueNonOwnerGroups", numUniqueNonOwnerGroups);
        metricsService.setGaugeValue("totalUniqueUserGroups", totalUniqueUserGroups);
        metricsService.setGaugeValue("numberOfSdbs", numSDBs);
        metricsService.setGaugeValue("numberOfDataNodes", numDataNodes);
        metricsService.setGaugeValue("numberOfKeyValuePairs", numKeyValuePairs);
    }

}
