/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.dao;

import com.google.common.collect.Lists;
import com.nike.cerberus.mapper.AwsIamRoleMapper;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.record.AwsIamRolePermissionRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import org.junit.Before;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AwsIamRoleDaoTest {

    private final String awsIamRoleArn = "IAM_ROLE_ARN";

    private final String awsRegion = "us-west-2";

    private final String safeDepositBoxId = "SDB_ID";

    private final String roleId = "ROLE_ID";

    private final String iamRoleId = "IAM_ROLE_ID";

    private final String iamRolePermissionId = "IAM_ROLE_PERMISSION_ID";

    private final String iamRoleKmsKeyId = "IAM_ROLE_KMS_KEY_ID";

    private final String awsKmsKeyId = "arn:aws:kms:us-west-2:ACCOUNT_ID:key/GUID";

    private final String createdBy = "system";

    private final String lastUpdatedBy = "system";

    private final OffsetDateTime createdTs = OffsetDateTime.now(ZoneId.of("UTC"));

    private final OffsetDateTime lastUpdatedTs = OffsetDateTime.now(ZoneId.of("UTC"));

    private final AwsIamRoleRecord awsIamRoleRecord = new AwsIamRoleRecord()
            .setId(iamRoleId)
            .setAwsIamRoleArn(awsIamRoleArn)
            .setCreatedBy(createdBy)
            .setLastUpdatedBy(lastUpdatedBy)
            .setCreatedTs(createdTs)
            .setLastUpdatedTs(lastUpdatedTs);

    private final AwsIamRolePermissionRecord awsIamRolePermissionRecord = new AwsIamRolePermissionRecord()
            .setId(iamRolePermissionId)
            .setAwsIamRoleId(iamRoleId)
            .setSdboxId(safeDepositBoxId)
            .setRoleId(roleId)
            .setCreatedBy(createdBy)
            .setLastUpdatedBy(lastUpdatedBy)
            .setCreatedTs(createdTs)
            .setLastUpdatedTs(lastUpdatedTs);

    private final List<AwsIamRolePermissionRecord> awsIamRolePermissionRecordList =
            Lists.newArrayList(awsIamRolePermissionRecord);

    private final AwsIamRoleKmsKeyRecord awsIamRoleKmsKeyRecord = new AwsIamRoleKmsKeyRecord()
            .setId(iamRoleKmsKeyId)
            .setAwsIamRoleId(iamRoleId)
            .setAwsRegion(awsRegion)
            .setAwsKmsKeyId(awsKmsKeyId)
            .setCreatedBy(createdBy)
            .setLastUpdatedBy(lastUpdatedBy)
            .setCreatedTs(createdTs)
            .setLastUpdatedTs(lastUpdatedTs);

    private AwsIamRoleMapper awsIamRoleMapper;

    private AwsIamRoleDao subject;

    @Before
    public void setUp() throws Exception {
        awsIamRoleMapper = mock(AwsIamRoleMapper.class);
        subject = new AwsIamRoleDao(awsIamRoleMapper);
    }

    @Test
    public void getIamRole_by_id_returns_record_when_found() {
        when(awsIamRoleMapper.getIamRoleById(iamRoleId)).thenReturn(awsIamRoleRecord);

        final Optional<AwsIamRoleRecord> actual = subject.getIamRoleById(iamRoleId);

        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(awsIamRoleRecord);
    }

    @Test
    public void getIamRole_by_id_returns_empty_when_record_not_found() {
        when(awsIamRoleMapper.getIamRoleById(iamRoleId)).thenReturn(null);

        final Optional<AwsIamRoleRecord> actual = subject.getIamRoleById(iamRoleId);

        assertThat(actual.isPresent()).isFalse();
    }

    @Test
    public void getIamRole_returns_record_when_found() {
        String arn = "arn";
        when(awsIamRoleMapper.getIamRole(arn)).thenReturn(awsIamRoleRecord);

        final Optional<AwsIamRoleRecord> actual = subject.getIamRole(arn);

        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(awsIamRoleRecord);
    }

    @Test
    public void getIamRole_returns_empty_when_record_not_found() {
        when(awsIamRoleMapper.getIamRole(awsIamRoleArn)).thenReturn(null);

        final Optional<AwsIamRoleRecord> actual = subject.getIamRole(awsIamRoleArn);

        assertThat(actual.isPresent()).isFalse();
    }

    @Test
    public void getIamRole_with_arn_returns_record_when_found() {
        when(awsIamRoleMapper.getIamRole(awsIamRoleArn)).thenReturn(awsIamRoleRecord);

        final Optional<AwsIamRoleRecord> actual = subject.getIamRole(awsIamRoleArn);

        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(awsIamRoleRecord);
    }

    @Test
    public void getIamRole_with_arn_returns_empty_when_record_not_found() {
        String arn = "arn";
        when(awsIamRoleMapper.getIamRole(arn)).thenReturn(null);

        final Optional<AwsIamRoleRecord> actual = subject.getIamRole(arn);

        assertThat(actual.isPresent()).isFalse();
    }

    @Test
    public void createIamRole_returns_record_count() {
        final int recordCount = 1;
        when(awsIamRoleMapper.createIamRole(awsIamRoleRecord)).thenReturn(recordCount);

        final int actualCount = subject.createIamRole(awsIamRoleRecord);

        assertThat(actualCount).isEqualTo(recordCount);
    }

    @Test
    public void getIamRolePermissions_returns_list_of_records() {
        when(awsIamRoleMapper.getIamRolePermissions(safeDepositBoxId)).thenReturn(awsIamRolePermissionRecordList);

        List<AwsIamRolePermissionRecord> actual = subject.getIamRolePermissions(safeDepositBoxId);

        assertThat(actual).isNotEmpty();
        assertThat(actual).hasSameElementsAs(awsIamRolePermissionRecordList);
    }

    @Test
    public void createIamRolePermission_returns_record_count() {
        final int recordCount = 1;
        when(awsIamRoleMapper.createIamRolePermission(awsIamRolePermissionRecord)).thenReturn(recordCount);

        final int actualCount = subject.createIamRolePermission(awsIamRolePermissionRecord);

        assertThat(actualCount).isEqualTo(recordCount);
    }

    @Test
    public void updateIamRolePermission_returns_record_count() {
        final int recordCount = 1;
        when(awsIamRoleMapper.updateIamRolePermission(awsIamRolePermissionRecord)).thenReturn(recordCount);

        final int actualCount = subject.updateIamRolePermission(awsIamRolePermissionRecord);

        assertThat(actualCount).isEqualTo(recordCount);
    }

    @Test
    public void deleteIamRolePermission_returns_record_count() {
        final int recordCount = 1;
        when(awsIamRoleMapper.deleteIamRolePermission(safeDepositBoxId, iamRoleId)).thenReturn(recordCount);

        final int actualCount = subject.deleteIamRolePermission(safeDepositBoxId, iamRoleId);

        assertThat(actualCount).isEqualTo(recordCount);
    }

    @Test
    public void deleteIamRolePermissions_returns_record_count() {
        final int recordCount = 1;
        when(awsIamRoleMapper.deleteIamRolePermissions(safeDepositBoxId)).thenReturn(recordCount);

        final int actualCount = subject.deleteIamRolePermissions(safeDepositBoxId);

        assertThat(actualCount).isEqualTo(recordCount);
    }

    @Test
    public void getKmsKey_returns_record_when_found() {
        when(awsIamRoleMapper.getKmsKey(iamRoleId, awsRegion)).thenReturn(awsIamRoleKmsKeyRecord);

        final Optional<AwsIamRoleKmsKeyRecord> actual = subject.getKmsKey(iamRoleId, awsRegion);

        assertThat(actual.isPresent()).isTrue();
        assertThat(actual.get()).isEqualTo(awsIamRoleKmsKeyRecord);
    }

    @Test
    public void getKmsKey_returns_empty_when_record_not_found() {
        when(awsIamRoleMapper.getKmsKey(iamRoleId, awsRegion)).thenReturn(null);

        final Optional<AwsIamRoleKmsKeyRecord> actual = subject.getKmsKey(iamRoleId, awsRegion);

        assertThat(actual.isPresent()).isFalse();
    }

    @Test
    public void createIamRoleKmsKey_returns_record_count() {
        final int recordCount = 1;
        when(awsIamRoleMapper.createIamRoleKmsKey(awsIamRoleKmsKeyRecord)).thenReturn(recordCount);

        final int actualCount = subject.createIamRoleKmsKey(awsIamRoleKmsKeyRecord);

        assertThat(actualCount).isEqualTo(recordCount);
    }

    @Test
    public void updateIamRoleKmsKey_returns_record_count() {
        final int recordCount = 1;
        when(awsIamRoleMapper.updateIamRoleKmsKey(awsIamRoleKmsKeyRecord)).thenReturn(recordCount);

        final int actualCount = subject.updateIamRoleKmsKey(awsIamRoleKmsKeyRecord);

        assertThat(actualCount).isEqualTo(recordCount);
    }

    @Test
    public void deleteIamRoleById_returns_record_count() {
        final int recordCount = 1;
        when(awsIamRoleMapper.deleteIamRoleById(iamRoleId)).thenReturn(recordCount);

        final int actualCount = subject.deleteIamRoleById(iamRoleId);

        assertThat(actualCount).isEqualTo(recordCount);
    }

    @Test
    public void deleteKmsKeyById_returns_record_count() {
        final int recordCount = 1;
        when(awsIamRoleMapper.deleteKmsKeyById(iamRoleKmsKeyId)).thenReturn(recordCount);

        final int actualCount = subject.deleteKmsKeyById(iamRoleKmsKeyId);

        assertThat(actualCount).isEqualTo(recordCount);
    }
}