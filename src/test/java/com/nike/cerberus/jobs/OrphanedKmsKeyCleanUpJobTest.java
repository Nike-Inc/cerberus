package com.nike.cerberus.jobs;

import com.amazonaws.regions.Regions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.nike.cerberus.domain.AuthKmsKeyMetadata;
import com.nike.cerberus.service.KmsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;

import static com.nike.cerberus.service.KmsService.SOONEST_A_KMS_KEY_CAN_BE_DELETED;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class OrphanedKmsKeyCleanUpJobTest {

    @Mock
    private KmsService kmsService;

    private final String ENVIRONMENT = "unit-test";

    private OrphanedKmsKeyCleanUpJob job;

    @Before
    public void before() {
        initMocks(this);
        job = new OrphanedKmsKeyCleanUpJob(kmsService, false, ENVIRONMENT);
    }

    @Test
    public void test_that_executeLockableCode_iterates_over_available_regions_and_calls_process_region_for_each() {
        OrphanedKmsKeyCleanUpJob spyJob = spy(job);
        doReturn(ImmutableSet.of("kms-cmk-id-2")).when(spyJob).processRegion(anyList(), anyString());
        spyJob.executeLockableCode();
        verify(spyJob, times(Regions.values().length - 2)).processRegion(anyList(), anyString());
    }

    @Test
    public void test_that_processRegion_obeys_dry_mode_flag() {
        String region = "us-west-2";
        List<AuthKmsKeyMetadata> authKmsKeyMetadataList = ImmutableList.of(
                new AuthKmsKeyMetadata()
                        .setAwsRegion(region)
                        .setAwsKmsKeyId("arn:aws:kms:us-west-2:111122223333:key/kms-cmk-id-1")
        );
        Set<String> kmsKeyIdsForRegion = ImmutableSet.of("kms-cmk-id-1", "kms-cmk-id-2", "kms-cmk-id-3", "kms-cmk-id-4");
        Set<String> keysCreatedByKmsService = ImmutableSet.of("kms-cmk-id-1", "kms-cmk-id-2");
        when(kmsService.getKmsKeyIdsForRegion(region)).thenReturn(kmsKeyIdsForRegion);
        when(kmsService.filterKeysCreatedByKmsService(kmsKeyIdsForRegion, region)).thenReturn(keysCreatedByKmsService);

        Set<String> keys = job.processRegion(authKmsKeyMetadataList, region);

        verify(kmsService, times(1))
                .scheduleKmsKeyDeletion("kms-cmk-id-2", region, SOONEST_A_KMS_KEY_CAN_BE_DELETED);

        assertEquals(1, keys.size());

        reset(kmsService);

        when(kmsService.getKmsKeyIdsForRegion(region)).thenReturn(kmsKeyIdsForRegion);
        when(kmsService.filterKeysCreatedByKmsService(kmsKeyIdsForRegion, region)).thenReturn(keysCreatedByKmsService);

        job = new OrphanedKmsKeyCleanUpJob(kmsService, true, ENVIRONMENT);

        Set<String> keys2 = job.processRegion(authKmsKeyMetadataList, region);

        verify(kmsService, never())
                .scheduleKmsKeyDeletion(anyString(), anyString(), anyInt());

        assertEquals(1, keys2.size());
    }

}
