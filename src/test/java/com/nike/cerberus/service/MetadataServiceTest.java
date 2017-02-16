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

import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.dao.CategoryDao;
import com.nike.cerberus.dao.RoleDao;
import com.nike.cerberus.dao.SafeDepositBoxDao;
import com.nike.cerberus.dao.UserGroupDao;
import com.nike.cerberus.domain.SdbMetadata;
import com.nike.cerberus.domain.SDBMetadataResult;
import com.nike.cerberus.record.CategoryRecord;
import com.nike.cerberus.record.RoleRecord;
import com.nike.cerberus.record.SafeDepositBoxRecord;
import com.nike.cerberus.record.UserGroupPermissionRecord;
import com.nike.cerberus.record.UserGroupRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MetadataServiceTest {

    @InjectMocks
    private MetadataService metadataService;

    private MetadataService metadataServiceSpy;

    @Mock
    private RoleService roleService;

    @Mock
    private SafeDepositBoxDao safeDepositBoxDao;

    @Mock
    private UserGroupDao userGroupDao;

    @Mock
    private DateTimeSupplier dateTimeSupplier;

    @Mock
    private CategoryDao categoryDao;

    @Mock
    private RoleDao roleDao;

    @Mock
    private AwsIamRoleDao awsIamRoleDao;

    @Before
    public void before() {
        initMocks(this);

        metadataServiceSpy = spy(metadataService);
    }

    @Test
    public void test_that_getSdbMetadata_properly_sets_result_meta_data() {
        int limit = 5;
        int offset = 0;
        int totalSDBs = 20;

        when(safeDepositBoxDao.getSafeDepositBoxCount()).thenReturn(totalSDBs);

        SdbMetadata sdbMD = new SdbMetadata();
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
    public void test_that_getSdbMetadata_set_has_next_to_non_when_done_paging() {
        int limit = 5;
        int offset = 15;
        int totalSDBs = 20;

        when(safeDepositBoxDao.getSafeDepositBoxCount()).thenReturn(totalSDBs);
        doReturn(Arrays.asList(new SdbMetadata())).when(metadataServiceSpy).getSDBMetadataList(limit, offset);

        SDBMetadataResult actual = metadataServiceSpy.getSDBMetadata(limit, offset);

        assertEquals("expected actual limit to be passed in limit", limit, actual.getLimit());
        assertEquals("expected actual offset to be passed in offset", offset, actual.getOffset());
        assertEquals("expected next offset to be 0 because no more paging", 0, actual.getNextOffset());
        assertEquals("expected there to be another page of results", false, actual.isHasNext());
        assertEquals("expected the sdb count to equal 1", 1, actual.getSdbCountInResult());
        assertEquals("expected total sdbs to equal the sdb total count", totalSDBs, actual.getTotalSDBCount());
    }

    @Test
    public void test_that_getCategoryIdToStringMap_returns_valid_map() {
        Map<String, String> expected = new HashMap<>();
        expected.put("abc", "foo");

        when(categoryDao.getAllCategories())
                .thenReturn(Arrays.asList(new CategoryRecord().setId("abc").setDisplayName("foo")));

        Map<String, String> actual = metadataService.getCategoryIdToStringMap();
        assertEquals(expected, actual);
    }

    @Test
    public void test_that_getRoleIdToStringMap_returns_valid_map() {
        Map<String, String> expected = new HashMap<>();
        expected.put("abc", "foo");

        when(roleDao.getAllRoles())
                .thenReturn(Arrays.asList(new RoleRecord().setId("abc").setName("foo")));

        Map<String, String> actual = metadataService.getRoleIdToStringMap();
        assertEquals(expected, actual);
    }

    @Test
    public void test_that_getSDBMetadataList_returns_valid_list() {
        String sdbId = "123";
        String categoryName = "foo";
        String categoryId = "321";
        String name = "test-name";
        String path = "app/test-name";
        String desc = "blah blah blah";
        String by = "justin.field@nike.com";
        OffsetDateTime offsetDateTime = OffsetDateTime.now();

        Map<String, String> catMap = new HashMap<>();
        catMap.put(categoryId, categoryName);
        Map<String, String> roleMap = new HashMap<>();

        doReturn(catMap).when(metadataServiceSpy).getCategoryIdToStringMap();
        doReturn(roleMap).when(metadataServiceSpy).getRoleIdToStringMap();

        when(safeDepositBoxDao.getSafeDepositBoxes(1,0)).thenReturn(Arrays.asList(new SafeDepositBoxRecord()
                .setId(sdbId)
                .setName(name)
                .setPath(path)
                .setDescription(desc)
                .setCategoryId(categoryId)
                .setCreatedBy(by)
                .setLastUpdatedBy(by)
                .setCreatedTs(offsetDateTime)
                .setLastUpdatedTs(offsetDateTime)));

        doNothing().when(metadataServiceSpy)
                .processGroupData(anyMap(), isA(SdbMetadata.class), anyString());

        HashMap<String, String> rPermMap = new HashMap<>();
        doReturn(rPermMap).when(metadataServiceSpy).getIamRolePermissionMap(roleMap, sdbId);

        List<SdbMetadata> actual = metadataServiceSpy.getSDBMetadataList(1,0);
        assertEquals("List should have 1 entry", 1, actual.size());
        SdbMetadata data = actual.get(0);
        assertEquals("Name should match record", name, data.getName());
        assertEquals("path  should match record", path, data.getPath());
        assertEquals("", categoryName, data.getCategory());
        assertEquals("desc  should match record", desc, data.getDescription());
        assertEquals("created by  should match record", by, data.getCreatedBy());
        assertEquals("last updated by should match record", by, data.getLastUpdatedBy());
        assertEquals("created ts should match record", offsetDateTime, data.getCreatedTs());
        assertEquals("updated ts should match record", offsetDateTime, data.getLastUpdatedTs());
        assertEquals("iam role perm map should match what is returned by getIamRolePermissionMap",
                rPermMap, data.getIamRolePermissions());
    }

    @Test
    public void test_that_processGroupData_sets_owner_and_adds_user_perms_map() {
        String careBearsGroup = "care-bears";
        String careBearsId = "000-abc";
        String grumpyBearsGroup = "grumpy-bears";
        String grumpyBearsId = "111-def";
        String ownerId = "000";
        String readId = "111";
        String sdbId = "abc-123-cdf";

        Map<String, String> roleIdToStringMap = new HashMap<>();
        roleIdToStringMap.put(ownerId, RoleRecord.ROLE_OWNER);
        roleIdToStringMap.put(readId, RoleRecord.ROLE_READ);

        SdbMetadata metadata = new SdbMetadata();

        when(userGroupDao.getUserGroupPermissions(sdbId)).thenReturn(Arrays.asList(
                new UserGroupPermissionRecord().setRoleId(ownerId).setUserGroupId(careBearsId),
                new UserGroupPermissionRecord().setRoleId(readId).setUserGroupId(grumpyBearsId)
        ));

        when(userGroupDao.getUserGroup(careBearsId))
                .thenReturn(Optional.of(new UserGroupRecord().setName(careBearsGroup)));
        when(userGroupDao.getUserGroup(grumpyBearsId))
                .thenReturn(Optional.of(new UserGroupRecord().setName(grumpyBearsGroup)));

        metadataService.processGroupData(roleIdToStringMap, metadata, sdbId);

        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put(grumpyBearsGroup, RoleRecord.ROLE_READ);

        assertEquals("Owner group should be care-bears", careBearsGroup, metadata.getOwner());
        assertEquals("The user group perms should match the expected map",
                expectedMap, metadata.getUserGroupPermissions());
    }

    @Test
    public void test_that_getIamRolePermissionMap_returns_valid_map() {
        Map<String, String> roleMap = new HashMap<>();
        roleMap.put("123", "read");
    }

}
