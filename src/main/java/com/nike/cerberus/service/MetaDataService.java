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
import com.nike.cerberus.domain.SDBMetaData;
import com.nike.cerberus.domain.SDBMetaDataResult;
import com.nike.cerberus.record.AwsIamRolePermissionRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.record.CategoryRecord;
import com.nike.cerberus.record.RoleRecord;
import com.nike.cerberus.record.SafeDepositBoxRecord;
import com.nike.cerberus.record.UserGroupPermissionRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Provides general stats about safe deposit boxes.
 */
public class MetaDataService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final SafeDepositBoxDao safeDepositBoxDao;
    private final UserGroupDao userGroupDao;
    private final CategoryDao categoryDao;
    private final RoleDao roleDao;
    private final AwsIamRoleDao awsIamRoleDao;

    @Inject
    public MetaDataService(SafeDepositBoxDao safeDepositBoxDao,
                           UserGroupDao userGroupDao,
                           CategoryDao categoryDao,
                           RoleDao roleDao,
                           AwsIamRoleDao awsIamRoleDao) {

        this.safeDepositBoxDao = safeDepositBoxDao;
        this.userGroupDao = userGroupDao;
        this.categoryDao = categoryDao;
        this.roleDao = roleDao;
        this.awsIamRoleDao = awsIamRoleDao;
    }

    /**
     * Method for retrieving meta data about SDBs sorted by created date.
     *
     * @param limit  The int limit for paginating.
     * @param offset The int offset for paginating.
     * @return SDBMetaDataResult of meta data.
     */
    public SDBMetaDataResult getSDBMetaData(int limit, int offset) {
        SDBMetaDataResult result = new SDBMetaDataResult();
        result.setLimit(limit);
        result.setOffset(offset);
        result.setTotalSDBCount(safeDepositBoxDao.getSafeDepositBoxCount());
        result.setHasNext(result.getTotalSDBCount() > (offset + limit));
        if (result.isHasNext()) {
            result.setNextOffset(offset + limit);
        }
        List<SDBMetaData> sdbMetaDataList = getSDBMetaDataList(limit, offset);
        result.setSafeDepositBoxMetaData(sdbMetaDataList);
        result.setSdbCountInResult(sdbMetaDataList.size());

        return result;
    }

    protected Map<String,String> getCategoryIdToStringMap() {
        List<CategoryRecord> categoryRecords = categoryDao.getAllCategories();
        Map<String, String> catIdToStringMap = new HashMap<>(categoryRecords.size());
        categoryRecords.forEach(categoryRecord ->
                catIdToStringMap.put(categoryRecord.getId(), categoryRecord.getDisplayName())
        );
        return catIdToStringMap;
    }

    protected Map<String,String> getRoleIdToStringMap() {
        List<RoleRecord> roleRecords = roleDao.getAllRoles();
        Map<String, String> roleIdToStringMap = new HashMap<>(roleRecords.size());
        roleRecords.forEach(roleRecord -> roleIdToStringMap.put(roleRecord.getId(), roleRecord.getName()));
        return roleIdToStringMap;
    }

    protected List<SDBMetaData> getSDBMetaDataList(int limit, int offset) {
        List<SDBMetaData> sdbs = new LinkedList<>();

        // Collect the categories.
        Map<String, String> catIdToStringMap = getCategoryIdToStringMap();
        // Collect the roles
        Map<String, String> roleIdToStringMap = getRoleIdToStringMap();
        // Collect The SDB Records
        final List<SafeDepositBoxRecord> safeDepositBoxRecords = safeDepositBoxDao.getSafeDepositBoxes(limit, offset);

        // for each SDB collect the user and iam permissions and add to result
        safeDepositBoxRecords.forEach(sdb -> {
            SDBMetaData data = new SDBMetaData();
            data.setName(sdb.getName());
            data.setPath(sdb.getPath());
            data.setDescription(sdb.getDescription());
            data.setCategory(catIdToStringMap.get(sdb.getCategoryId()));
            data.setCreatedBy(sdb.getCreatedBy());
            data.setCreatedTs(sdb.getCreatedTs());
            data.setLastUpdatedBy(sdb.getLastUpdatedBy());
            data.setLastUpdatedTs(sdb.getLastUpdatedTs());

            // set the owner and map group to roles
            processGroupData(roleIdToStringMap, data, sdb.getId());

            data.setIamRolePermissions(getIamRolePermissionMap(roleIdToStringMap, sdb.getId()));

            sdbs.add(data);
        });

        return sdbs;
    }

    protected void processGroupData(Map<String, String> roleIdToStringMap, SDBMetaData data, String sdbId) {
        List<UserGroupPermissionRecord> userPerms = userGroupDao.getUserGroupPermissions(sdbId);
        Map<String, String> groupRoleMap = new HashMap<>(userPerms.size() - 1);
        userPerms.forEach(record -> {
            String group = userGroupDao.getUserGroup(record.getUserGroupId()).get().getName();
            String role = roleIdToStringMap.get(record.getRoleId());

            if (StringUtils.equals(role, RoleRecord.ROLE_OWNER)) {
                data.setOwner(group);
            } else {
                groupRoleMap.put(group, role);
            }
        });

        data.setUserGroupPermissions(groupRoleMap);
    }

    protected Map<String,String> getIamRolePermissionMap(Map<String, String> roleIdToStringMap, String sdbId) {
        List<AwsIamRolePermissionRecord> iamPerms = awsIamRoleDao.getIamRolePermissions(sdbId);
        Map<String, String> iamRoleMap = new HashMap<>(iamPerms.size());
        iamPerms.forEach(record -> {
            AwsIamRoleRecord iam = awsIamRoleDao.getIamRole(record.getAwsIamRoleId()).get();
            String role = roleIdToStringMap.get(record.getRoleId());

            iamRoleMap.put(String.format("arn:aws:iam::%s:role/%s",
                    iam.getAwsAccountId(), iam.getAwsIamRoleName()), role);
        });
        return iamRoleMap;
    }
}
