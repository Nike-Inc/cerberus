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

import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.metric.MetricsService;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.cerberus.service.SecureDataService;
import com.nike.cerberus.service.UserGroupPermissionService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically send KPI metrics to the enabled metrics services. */
@Slf4j
@ConditionalOnProperty("cerberus.jobs.kpiMetricsProcessingJob.enabled")
@Component
public class KpiMetricsProcessingJob {

  private final MetricsService metricsService;

  private final SafeDepositBoxService safeDepositBoxService;

  private final AwsIamRoleDao awsIamRoleDao;

  private final SecureDataService secureDataService;

  private final UserGroupPermissionService userGroupPermissionService;

  @Autowired
  public KpiMetricsProcessingJob(
      MetricsService metricsService,
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

  @Scheduled(cron = "${cerberus.jobs.kpiMetricsProcessingJob.cronExpression}")
  public void execute() {
    log.debug("Running KPI metrics processing job");
    try {
      processKpiMetrics();
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
    int numFiles = secureDataService.getTotalNumberOfFiles();
    Map<String, String> dimensions = Map.of();

    log.info(
        "Number of IAM roles: {}, Owner Groups: {}, Non-Owner Groups: {}, Total Unique Groups: {}, SDBs: {}, "
            + "Nodes: {}, Key/Value Pairs: {}, Number of Secure Files: {}",
        numUniqueIamRoles,
        numUniqueOwnerGroups,
        numUniqueNonOwnerGroups,
        totalUniqueUserGroups,
        numSDBs,
        numDataNodes,
        numKeyValuePairs,
        numFiles);

    metricsService.getOrCreateCallbackGauge(
        "numberOfUniqueIamRoles", () -> numUniqueIamRoles, dimensions);
    ;
    metricsService.getOrCreateCallbackGauge(
        "numberOfUniqueOwnerGroups", () -> numUniqueOwnerGroups, dimensions);
    ;
    metricsService.getOrCreateCallbackGauge(
        "numberOfUniqueNonOwnerGroups", () -> numUniqueNonOwnerGroups, dimensions);
    ;
    metricsService.getOrCreateCallbackGauge(
        "totalUniqueUserGroups", () -> totalUniqueUserGroups, dimensions);
    ;
    metricsService.getOrCreateCallbackGauge("numberOfSdbs", () -> numSDBs, dimensions);
    ;
    metricsService.getOrCreateCallbackGauge("numberOfDataNodes", () -> numDataNodes, dimensions);
    ;
    metricsService.getOrCreateCallbackGauge(
        "numberOfKeyValuePairs", () -> numKeyValuePairs, dimensions);
    ;
    metricsService.getOrCreateCallbackGauge("numberOfFiles", () -> numFiles, dimensions);
    ;
  }
}
