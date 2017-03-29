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

package com.nike.cerberus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.domain.IamRolePermission;
import com.nike.cerberus.domain.Role;
import com.nike.cerberus.domain.SDBMetadata;
import com.nike.cerberus.domain.SDBMetadataResult;
import com.nike.cerberus.domain.SafeDepositBox;
import com.nike.cerberus.domain.UserGroupPermission;
import com.nike.cerberus.record.RoleRecord;
import com.nike.cerberus.server.config.CmsConfig;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.UuidSupplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MetadataServiceTest {

    @InjectMocks
    private MetadataService metadataService;

    private MetadataService metadataServiceSpy;

    @Mock
    private SafeDepositBoxService safeDepositBoxService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private RoleService roleService;

    @Mock
    private UuidSupplier uuidSupplier;

    @Mock
    private AwsIamRoleArnParser awsIamRoleArnParser;

    @Before
    public void before() {
        initMocks(this);

        metadataServiceSpy = spy(metadataService);
    }

    @Test
    public void test_that_get_sdb_metadata_properly_sets_result_metadata() {
        int limit = 5;
        int offset = 0;
        int totalSDBs = 20;

        when(safeDepositBoxService.getTotalNumberOfSafeDepositBoxes()).thenReturn(totalSDBs);

        SDBMetadata sdbMD = new SDBMetadata();
        doReturn(Arrays.asList(sdbMD)).when(metadataServiceSpy).getSDBMetadataList(limit, offset);

        SDBMetadataResult actual = metadataServiceSpy.getSDBMetadata(limit, offset);

        assertEquals("expected actual limit to be passed in limit", limit, actual.getLimit());
        assertEquals("expected actual offset to be passed in offset", offset, actual.getOffset());
        assertEquals("expected next offset to be limit + offset", limit + offset, actual.getNextOffset());
        assertEquals("expected there to be another page of results", true, actual.isHasNext());
        assertEquals("expected the sdb count to equal 1", 1, actual.getSdbCountInResult());
        assertEquals("expected total sdbs to equal the sdb total count", totalSDBs, actual.getTotalSDBCount());
    }

    @Test
    public void test_that_get_sdb_metadata_set_has_next_to_no_when_done_paging() {
        int limit = 5;
        int offset = 15;
        int totalSDBs = 20;

        when(safeDepositBoxService.getTotalNumberOfSafeDepositBoxes()).thenReturn(totalSDBs);
        doReturn(Arrays.asList(new SDBMetadata())).when(metadataServiceSpy).getSDBMetadataList(limit, offset);

        SDBMetadataResult actual = metadataServiceSpy.getSDBMetadata(limit, offset);

        assertEquals("expected actual limit to be passed in limit", limit, actual.getLimit());
        assertEquals("expected actual offset to be passed in offset", offset, actual.getOffset());
        assertEquals("expected next offset to be 0 because no more paging", 0, actual.getNextOffset());
        assertEquals("expected there to be another page of results", false, actual.isHasNext());
        assertEquals("expected the sdb count to equal 1", 1, actual.getSdbCountInResult());
        assertEquals("expected total sdbs to equal the sdb total count", totalSDBs, actual.getTotalSDBCount());
    }

    @Test
    public void test_that_get_sdb_metadata_list_returns_valid_list() {
        String sdbId = "123";
        String categoryName = "foo";
        String categoryId = "321";
        String name = "test-name";
        String path = "app/test-name";
        String desc = "blah blah blah";
        String by = "justin.field@nike.com";
        String careBearsGroup = "care-bears";
        String careBearsId = "000-abc";
        String grumpyBearsGroup = "grumpy-bears";
        String grumpyBearsId = "111-def";
        String ownerId = "000";
        String readId = "111";
        String acctId = "12345";
        String roleName = "foo-role";

        OffsetDateTime offsetDateTime = OffsetDateTime.now();

        Map<String, String> catMap = new HashMap<>();
        catMap.put(categoryId, categoryName);

        Map<String, String> roleIdToStringMap = new HashMap<>();
        roleIdToStringMap.put(ownerId, RoleRecord.ROLE_OWNER);
        roleIdToStringMap.put(readId, RoleRecord.ROLE_READ);

        when(roleService.getRoleIdToStringMap()).thenReturn(roleIdToStringMap);
        when(categoryService.getCategoryIdToCategoryNameMap()).thenReturn(catMap);

        SafeDepositBox box = new SafeDepositBox();
        box.setId(sdbId);
        box.setName(name);
        box.setPath(path);
        box.setDescription(desc);
        box.setCategoryId(categoryId);
        box.setCreatedBy(by);
        box.setLastUpdatedBy(by);
        box.setCreatedTs(offsetDateTime);
        box.setLastUpdatedTs(offsetDateTime);
        box.setOwner(careBearsGroup);

        Set<UserGroupPermission> userPerms = new HashSet<>();
        userPerms.add(new UserGroupPermission().withName(grumpyBearsGroup).withRoleId(readId));
        box.setUserGroupPermissions(userPerms);

        Set<IamRolePermission> iamPerms = new HashSet<>();
        iamPerms.add(new IamRolePermission().withAccountId(acctId).withIamRoleName(roleName).withRoleId(readId));
        box.setIamRolePermissions(iamPerms);

        when(safeDepositBoxService.getSafeDepositBoxes(1,0)).thenReturn(Arrays.asList(box));

        List<SDBMetadata> actual = metadataService.getSDBMetadataList(1,0);
        assertEquals("List should have 1 entry", 1, actual.size());
        SDBMetadata data = actual.get(0);
        assertEquals("Name should match record", name, data.getName());
        assertEquals("path  should match record", path, data.getPath());
        assertEquals("", categoryName, data.getCategory());
        assertEquals("desc  should match record", desc, data.getDescription());
        assertEquals("created by  should match record", by, data.getCreatedBy());
        assertEquals("last updated by should match record", by, data.getLastUpdatedBy());
        assertEquals("created ts should match record", offsetDateTime, data.getCreatedTs());
        assertEquals("updated ts should match record", offsetDateTime, data.getLastUpdatedTs());

        Map<String, String> expectedIamPermMap = new HashMap<>();
        expectedIamPermMap.put(String.format(AwsIamRoleArnParser.AWS_IAM_ROLE_ARN_TEMPLATE, acctId, roleName), RoleRecord.ROLE_READ);
        assertEquals("iam role perm map should match what is returned by getIamRolePermissionMap",
                expectedIamPermMap, data.getIamRolePermissions());

        Map<String, String> expectedGroupPermMap = new HashMap<>();
        expectedGroupPermMap.put(grumpyBearsGroup, RoleRecord.ROLE_READ);
        assertEquals("Owner group should be care-bears", careBearsGroup, data.getOwner());
        assertEquals("The user group perms should match the expected map",
                expectedGroupPermMap, data.getUserGroupPermissions());
    }

    @Test
    public void test_that_restore_metadata_calls_the_sdb_service_with_expected_sdb_box() throws IOException {
        String user = "unit-test-user";
        String id = "111";
        String categoryId = "222";
        String categoryName = "Applications";
        String readId = "333";
        String sdbName = "HEALTH CHECK BUCKET";

        ObjectMapper mapper = CmsConfig.configureObjectMapper();
        InputStream metadataStream = getClass().getClassLoader()
                .getResourceAsStream("com/nike/cerberus/service/sdb_metadata_backup.json");
        SDBMetadata sdbMetadata = mapper.readValue(metadataStream, SDBMetadata.class);

        when(safeDepositBoxService.getSafeDepositBoxIdByName(sdbName)).thenReturn(Optional.ofNullable(null));
        when(uuidSupplier.get()).thenReturn(id);
        when(categoryService.getCategoryIdByName(categoryName)).thenReturn(Optional.of(categoryId));
        Role readRole = new Role();
        readRole.setId(readId);
        when(roleService.getRoleByName(RoleRecord.ROLE_READ)).thenReturn(Optional.of(readRole));
        when(awsIamRoleArnParser.getAccountId(anyString())).thenCallRealMethod();
        when(awsIamRoleArnParser.getRoleName(anyString())).thenCallRealMethod();

        metadataService.restoreMetadata(sdbMetadata, user);

        SafeDepositBox expectedSdb = new SafeDepositBox();
        expectedSdb.setId(id);
        expectedSdb.setPath("app/health-check-bucket/");
        expectedSdb.setCategoryId(categoryId);
        expectedSdb.setName(sdbName);
        expectedSdb.setOwner("Lst-Squad.Carebears");
        expectedSdb.setDescription("This SDB is read by the Health Check Lambda...");
        expectedSdb.setCreatedTs(OffsetDateTime.parse("2016-09-08T15:39:31Z"));
        expectedSdb.setLastUpdatedTs(OffsetDateTime.parse("2016-12-13T17:28:00Z"));
        expectedSdb.setCreatedBy("justin.field@nike.com");
        expectedSdb.setLastUpdatedBy("todd.lisonbee@nike.com");

        Set<UserGroupPermission> userPerms = new HashSet<>();
        userPerms.add(new UserGroupPermission().withName("Foundation.Prod.Support").withRoleId(readId));
        userPerms.add(new UserGroupPermission().withName("Lst-NIKE.FOO.ISL").withRoleId(readId));
        expectedSdb.setUserGroupPermissions(userPerms);

        Set<IamRolePermission> iamPerms = new HashSet<>();
        String arn = String.format(AwsIamRoleArnParser.AWS_IAM_ROLE_ARN_TEMPLATE, "1111111111", "lambda_prod_healthcheck");
        iamPerms.add(new IamRolePermission().withAccountId("1111111111").withIamRoleName("lambda_prod_healthcheck").withIamRoleArn(arn).withRoleId(readId));
        expectedSdb.setIamRolePermissions(iamPerms);

        expectedSdb.setUserGroupPermissions(userPerms);
        expectedSdb.setIamRolePermissions(iamPerms);

        verify(safeDepositBoxService, times(1)).restoreSafeDepositBox(expectedSdb, user);
    }
}
