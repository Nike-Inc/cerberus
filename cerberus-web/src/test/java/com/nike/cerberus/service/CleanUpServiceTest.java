package com.nike.cerberus.service;

import static com.nike.cerberus.service.KmsService.SOONEST_A_KMS_KEY_CAN_BE_DELETED;
import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import java.time.OffsetDateTime;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/** Tests the CleanUpService class */
public class CleanUpServiceTest {

  // class under test
  private CleanUpService cleanUpService;

  // dependencies
  @Mock private KmsService kmsService;

  @Mock private AwsIamRoleDao awsIamRoleDao;

  @Mock private DateTimeSupplier dateTimeSupplier;

  private OffsetDateTime now = OffsetDateTime.now(UTC);

  @Before
  public void setup() {

    initMocks(this);

    cleanUpService = new CleanUpService(kmsService, awsIamRoleDao, dateTimeSupplier);
  }

  @Test
  public void test_that_cleanUpInactiveAndOrphanedKmsKeys_succeeds() {

    int inactivePeriod = 30;
    String keyRecordId = "key record id";
    String awsKeyId = "aws key id";
    String keyRegion = "key region";
    AwsIamRoleKmsKeyRecord keyRecord = mock(AwsIamRoleKmsKeyRecord.class);
    when(keyRecord.getId()).thenReturn(keyRecordId);
    when(keyRecord.getAwsKmsKeyId()).thenReturn(awsKeyId);
    when(keyRecord.getAwsRegion()).thenReturn(keyRegion);
    when(dateTimeSupplier.get()).thenReturn(now);

    OffsetDateTime inactiveCutoffDate = now.minusDays(inactivePeriod);
    when(awsIamRoleDao.getInactiveOrOrphanedKmsKeys(inactiveCutoffDate))
        .thenReturn(Lists.newArrayList(keyRecord));

    // perform the call
    cleanUpService.cleanUpInactiveAndOrphanedKmsKeys(inactivePeriod, 0);

    verify(awsIamRoleDao).getInactiveOrOrphanedKmsKeys(inactiveCutoffDate);
    verify(kmsService).deleteKmsKeyById(keyRecordId);
    verify(kmsService)
        .scheduleKmsKeyDeletion(awsKeyId, keyRegion, SOONEST_A_KMS_KEY_CAN_BE_DELETED);
  }

  @Test
  public void test_that_cleanUpInactiveAndOrphanedKmsKeys_does_not_throw_exception_on_failure() {

    int inactivePeriod = 30;
    String keyRecordId = "key record id";
    String awsKeyId = "aws key id";
    String keyRegion = "key region";
    AwsIamRoleKmsKeyRecord keyRecord = mock(AwsIamRoleKmsKeyRecord.class);
    when(keyRecord.getId()).thenReturn(keyRecordId);
    when(keyRecord.getAwsKmsKeyId()).thenReturn(awsKeyId);
    when(keyRecord.getAwsRegion()).thenReturn(keyRegion);
    when(dateTimeSupplier.get()).thenReturn(now);

    OffsetDateTime inactiveCutoffDate = now.minusDays(inactivePeriod);
    when(awsIamRoleDao.getInactiveOrOrphanedKmsKeys(inactiveCutoffDate))
        .thenReturn(Lists.newArrayList(keyRecord));

    when(awsIamRoleDao.deleteKmsKeyById(keyRecordId)).thenThrow(new NullPointerException());

    cleanUpService.cleanUpInactiveAndOrphanedKmsKeys(inactivePeriod, 0);
  }

  @Test
  public void test_that_cleanUpOrphanedIamRoles_succeeds() {

    String iamRoleRecordId = "iam role record id";
    AwsIamRoleRecord roleRecord = mock(AwsIamRoleRecord.class);
    when(roleRecord.getId()).thenReturn(iamRoleRecordId);

    when(awsIamRoleDao.getOrphanedIamRoles()).thenReturn(Lists.newArrayList(roleRecord));

    // perform the call
    cleanUpService.cleanUpOrphanedIamRoles();

    verify(awsIamRoleDao).getOrphanedIamRoles();
    verify(awsIamRoleDao).deleteIamRoleById(iamRoleRecordId);
  }

  @Test
  public void test_that_cleanUpOrphanedIamRoles_does_not_throw_exception_on_failure() {

    String iamRoleRecordId = "iam role record id";
    AwsIamRoleRecord roleRecord = mock(AwsIamRoleRecord.class);
    when(roleRecord.getId()).thenReturn(iamRoleRecordId);

    when(awsIamRoleDao.getOrphanedIamRoles()).thenReturn(Lists.newArrayList(roleRecord));

    when(awsIamRoleDao.deleteIamRoleById(iamRoleRecordId)).thenThrow(new NullPointerException());

    cleanUpService.cleanUpOrphanedIamRoles();
  }
}
