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

import static com.nike.cerberus.service.KmsService.SOONEST_A_KMS_KEY_CAN_BE_DELETED;

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
    private final String environmentName;

    @Inject
    public OrphanedKmsKeyCleanUpJob(KmsService kmsService,
                                    @Named("cms.kms.delete_orphaned_keys_job.dry_mode")
                                            boolean isDeleteOrphanKeysInDryMode,
                                    @Named("cms.env.name") String environmentName) {

        this.kmsService = kmsService;
        this.isDeleteOrphanKeysInDryMode = isDeleteOrphanKeysInDryMode;
        this.environmentName = environmentName;
    }

    @Override
    protected void executeLockableCode() {

        log.info("Fetching the the keys that are in the database");
        List<AuthKmsKeyMetadata> authKmsKeyMetadataList = kmsService.getAuthenticationKmsMetadata();

        Map<String, Set<String>> orphanedKeysByRegion = new HashMap<>();

        // For each region that has KMS
        Arrays.stream(Regions.values()).forEach(region -> {
            String regionName = region.getName();

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
                    .map(authKmsKeyMetadata -> {
                        String fullArn = authKmsKeyMetadata.getAwsKmsKeyId();
                        return fullArn.split("/")[1];
                    })
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

            printRegionSummary(regionName, kmsCmksCreatedByKmsService, currentKmsCmkIdsForRegion, orphanedKmsKeysForRegion);

            orphanedKeysByRegion.put(regionName, orphanedKmsKeysForRegion);

            // Delete the orphaned keys
            if (! isDeleteOrphanKeysInDryMode) {
                orphanedKmsKeysForRegion.forEach(kmsCmkId -> kmsService
                        .scheduleKmsKeyDeletion(kmsCmkId, regionName, SOONEST_A_KMS_KEY_CAN_BE_DELETED));
            }
        });

        printCompleteSummary(orphanedKeysByRegion);
    }

    private void printRegionSummary(String regionName,
                                    Set<String> kmsCmksCreatedByKmsService,
                                    Set<String> currentKmsCmkIdsForRegion,
                                    Set<String> orphanedKmsKeysForRegion) {

        log.info("---------- Orphan KMS Key cleanup job summary for region: {} ------------", regionName);
        log.debug("The following keys where determined to be created by CMS service for env: {}", environmentName);
        kmsCmksCreatedByKmsService.forEach(log::debug);
        log.debug("The following keys were in the data-store for this region");
        currentKmsCmkIdsForRegion.forEach(log::debug);
        log.info("The following keys were determined to be orphaned and have been scheduled for deletion? {}", !isDeleteOrphanKeysInDryMode);
        orphanedKmsKeysForRegion.forEach(log::info);
        log.info("--------------------------------------------------------------------------------");

    }

    private void printCompleteSummary(Map<String, Set<String>> orphanedKeysByRegion) {
        log.info("----------- Orphan Kms Key Cleanup Job summary ------------");
        orphanedKeysByRegion.forEach((regionName, keys) -> {
            log.info("Region: {}, number of orphaned keys: {}", regionName, keys.size());
        });
        if (isDeleteOrphanKeysInDryMode) {
            log.info("The job was in dry mode so the keys were not deleted");
        } else {
            log.info("The job was not in dry mode so the keys have been scheduled for deletion");
        }
        log.info("--------------------------------------------------------------------------------");
    }
}
