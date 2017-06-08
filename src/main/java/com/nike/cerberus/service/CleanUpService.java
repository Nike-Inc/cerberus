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

package com.nike.cerberus.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility to clean up inactive and orphaned KMS keys
 */
@Singleton
public class CleanUpService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

//    private static final String KMS_KEY_CLEAN_UP_INTERVAL_OVERRIDE = "cms.kms.cleanup.interval.hours.override";

//    private static final Integer DEFAULT_KMS_KEY_CLEAN_UP_INTERVAL = 24; // in hours

//    private static final String KMS_KEY_EXPIRATION_INTERVAL_OVERRIDE = "cms.kms.key.inactive.after.n.days.override";

    private static final int DEFAULT_KMS_KEY_INACTIVE_AFTER_N_DAYS = 30;

    private static final int DEFAULT_SLEEP_BETWEEN_KMS_CALLS = 15;  // in seconds

//    @Inject(optional=true)
//    @Named(KMS_KEY_CLEAN_UP_INTERVAL_OVERRIDE)
//    private Integer kmsKeyCleanUpInterval = DEFAULT_KMS_KEY_CLEAN_UP_INTERVAL;

//    @Inject(optional=true)
//    @Named(KMS_KEY_EXPIRATION_INTERVAL_OVERRIDE)
//    private Integer kmsKeyInactiveAfterNDays = DEFAULT_KMS_KEY_INACTIVE_AFTER_N_DAYS;

    private final KmsService kmsService;

    private final AwsIamRoleDao awsIamRoleDao;

    private final DateTimeSupplier dateTimeSupplier;

    @Inject
    public CleanUpService(KmsService kmsService,
                          AwsIamRoleDao awsIamRoleDao,
                          DateTimeSupplier dateTimeSupplier) {
        this.kmsService = kmsService;
        this.awsIamRoleDao = awsIamRoleDao;
        this.dateTimeSupplier = dateTimeSupplier;
    }

//    public void scheduleKmsKeyCleanUp() {
//        // schedule one task to:
//            // clean up inactive and orphaned kms keys
//
//        throw new UnsupportedOperationException("Not yet implemented.");
//    }
//
//    public void scheduleIamRoleCleanUp() {
//
//        // schedule one task to:
//            // clean up orphaned iam roles
//
//        throw new UnsupportedOperationException("Not yet implemented.");
//    }

    /**
     * Delete all KMS keys and DB records for keys that have not been used recently and are no longer associated with an SDB.
     */
    private void cleanUpInactiveAndOrphanedKmsKeys() {

        // get orphaned and inactive kms keys (not used in 'n' days)
        final OffsetDateTime inactiveDateTime = dateTimeSupplier.get().minusDays(DEFAULT_KMS_KEY_INACTIVE_AFTER_N_DAYS);
        final List<AwsIamRoleKmsKeyRecord> inactiveAndOrphanedKmsKeys = awsIamRoleDao.getInactiveOrOrphanedKmsKeys(inactiveDateTime);

        // delete inactive and orphaned kms key records from DB
        inactiveAndOrphanedKmsKeys.forEach(kmsKeyRecord -> {
            try {
                awsIamRoleDao.deleteKmsKeyById(kmsKeyRecord.getAwsKmsKeyId());
                kmsService.deleteKmsKeyInAws(kmsKeyRecord.getAwsKmsKeyId(), kmsKeyRecord.getAwsRegion());
                TimeUnit.SECONDS.sleep(DEFAULT_SLEEP_BETWEEN_KMS_CALLS);
            } catch (InterruptedException ie) {
                logger.error("Timeout between KMS key deletion was interrupted");
            } catch(Exception e) {
                logger.error("There was a problem deleting KMS key with id: {}, region: {}",
                        kmsKeyRecord.getAwsIamRoleId(),
                        kmsKeyRecord.getAwsRegion());
            }
        });
    }

    /**
     * Delete all IAM role records that are no longer associated with an SDB.
     */
    private void cleanUpOrphanedIamRoles() {

        // get orphaned iam role ids
        List<AwsIamRoleRecord> orphanedIamRoleIds = awsIamRoleDao.getOrphanedIamRoles();

        // delete orphaned iam role records from DB
        orphanedIamRoleIds.forEach(awsIamRoleRecord -> awsIamRoleDao.deleteIamRoleById(awsIamRoleRecord.getId()));
    }
}

