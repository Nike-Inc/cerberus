package com.nike.cerberus.jobs;

import com.amazonaws.regions.Regions;
import com.google.common.collect.Sets;
import com.nike.cerberus.domain.AuthKmsKeyMetadata;
import com.nike.cerberus.service.KmsService;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This scans through all KMS keys it can access in all regions and parses the Key Policies to see if it was a key
 * that was created by CMS for the current running env. If it is a key that was created by itself for this env,
 * it cross references it with the data store to check if it is orphaned.
 * If it is orphaned (Created by CMS for the current env, but not in the database), it schedules it for deletion.
 *
 * Orphaned keys can be created due to a race condition from lazily creating KMS CMKs for auth.
 */
public class OrphanedKmsKeyCleanUpJob extends LockingJob {

    private final KmsService kmsService;
    private final boolean isDeleteOrphanKeysInDryMode;

    @Inject
    public OrphanedKmsKeyCleanUpJob(KmsService kmsService,
                                    @Named("cms.kms.delete_orphaned_keys_job.dry_mode")
                                            boolean isDeleteOrphanKeysInDryMode) {

        this.kmsService = kmsService;
        this.isDeleteOrphanKeysInDryMode = isDeleteOrphanKeysInDryMode;
    }

    @Override
    protected void executeLockableCode() {

        log.info("Fetching the the keys that are in the database");
        List<AuthKmsKeyMetadata> authKmsKeyMetadataList = kmsService.getAuthenticationKmsMetadata();

        // For each region that has KMS
//        Arrays.stream(Regions.values()).forEach(region -> {
//            String regionName = region.getName();
        String regionName = "ap-southeast-1";

            // skip china
            if (regionName.startsWith("cn")) {
                log.debug("KMS isn't in china, skipping...");
                return;
            }
            // skip us gov
            if (regionName.startsWith("us-gov")) {
                log.debug("Cerberus isn't in us-gov, as z requires special credentials, skipping...");
                return;
            }

            log.info("Processing region: {}", regionName);
            // Get the KMS Key Ids that are in the db for the current region
            Set<String> currentKmsCmkIdsForRegion = authKmsKeyMetadataList.stream()
                    .filter(authKmsKeyMetadata -> StringUtils.equalsIgnoreCase(authKmsKeyMetadata.getAwsRegion(), regionName))
                    .map(AuthKmsKeyMetadata::getAwsKmsKeyId)
                    .collect(Collectors.toSet());

            log.info("Fetching all KMS CMK ids keys for the region: {}", regionName);
            Set<String> allKmsCmkIdsForRegion = kmsService.getKmsKeyIdsForRegion(regionName);
            log.info("Found {} keys to process for region: {}", allKmsCmkIdsForRegion.size(), regionName);

            log.info("Filtering out the keys that were not created by this environment");
            Set<String> kmsCmksCreatedByKmsService = kmsService.filterKeysCreatedByKmsService(allKmsCmkIdsForRegion, regionName);
            log.info("Found {} keys to created by this environment process for region: {}", kmsCmksCreatedByKmsService.size(), regionName);

            log.info("Calculating difference between the set of keys created by this env to the set of keys in the db");
            Set<String> orphanedKmsKeysForRegion = Sets.difference(kmsCmksCreatedByKmsService, currentKmsCmkIdsForRegion);
            log.info("Found {} keys that were orphaned for region: {}", orphanedKmsKeysForRegion.size(), regionName);

            // Delete the orphaned keys
            orphanedKmsKeysForRegion.forEach(kmsCmkId -> {
                log.info("Determined that KMS CMK id: {}, in region: {} was created by this Cerberus Environment but is not stored in the data store as an active key so it should be deleted", kmsCmkId, regionName);
                if (! isDeleteOrphanKeysInDryMode) {
                    kmsService.deleteKmsKeyById(kmsCmkId);
                }
            });
//        });
    }




}
