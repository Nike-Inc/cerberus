/*
 * Copyright (c) 2018 Nike, Inc.
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

package com.nike.cerberus.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.ResultConfiguration;
import com.amazonaws.services.athena.model.StartQueryExecutionRequest;
import com.amazonaws.services.athena.model.StartQueryExecutionResult;
import com.google.inject.name.Named;
import com.nike.cerberus.aws.AthenaClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class AthenaService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String TABLE_TEMPLATE = "%s_audit_db.audit_data";

    private final String environmentName;
    private final AthenaClientFactory athenaClientFactory;
    private Set<String> partitions = new HashSet<>();

    @Inject
    public AthenaService(@Named("cms.env.name") String environmentName,
                         AthenaClientFactory athenaClientFactory) {

        this.environmentName = environmentName;
        this.athenaClientFactory = athenaClientFactory;
    }

    public void addPartitionIfMissing(String region, String bucket, String year, String month, String day, String hour) {
        String partition = String.format("year=%s/month=%s/day=%s/hour=%s", year, month, day, hour);
        String table = String.format(TABLE_TEMPLATE, environmentName);
        if (! partitions.contains(partition)) {
            try {
                String query = String.format("ALTER TABLE %s ADD PARTITION (year='%s', month='%s', day='%s', hour='%s') " +
                                "LOCATION 's3://%s/audit-logs/partitioned/year=%s/month=%s/day=%s/hour=%s'",
                        table,
                        year, month, day, hour,
                        bucket,
                        year, month, day, hour);

                AmazonAthena athena = athenaClientFactory.getClient(region);

                StartQueryExecutionResult result = athena.startQueryExecution(new StartQueryExecutionRequest()
                        .withQueryString(query)
                        .withResultConfiguration(new ResultConfiguration().withOutputLocation(String.format("s3://%s/results/", bucket)))
                );
                log.debug("Started query: '{}' to add partition: '{}' to table: '{}'", result.getQueryExecutionId(), partition, table);
                partitions.add(partition);
            } catch (AmazonClientException e) {
                log.error("Failed to start add partition query for year={}/month={}/day={}/hour={}", year, month, day, hour, e);
            }
        }
    }
}
