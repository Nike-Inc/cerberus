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

import com.nike.cerberus.mapper.SafeDepositBoxMapper;
import com.nike.cerberus.record.SafeDepositBoxRecord;
import com.nike.cerberus.record.SafeDepositBoxRoleRecord;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Data access layer for the safe deposit box data.
 */
public class SafeDepositBoxDao {

    private final SafeDepositBoxMapper safeDepositBoxMapper;

    @Inject
    public SafeDepositBoxDao(final SafeDepositBoxMapper safeDepositBoxMapper) {
        this.safeDepositBoxMapper = safeDepositBoxMapper;
    }

    public List<SafeDepositBoxRoleRecord> getUserAssociatedSafeDepositBoxRoles(final Set<String> userGroups) {
        return safeDepositBoxMapper.getUserAssociatedSafeDepositBoxRoles(userGroups);
    }

    public List<SafeDepositBoxRoleRecord> getIamRoleAssociatedSafeDepositBoxRoles(final String awsIamRoleArn,
                                                                                  final String iamRootArn) {
        return safeDepositBoxMapper.getIamRoleAssociatedSafeDepositBoxRoles(awsIamRoleArn, iamRootArn);
    }

    public List<SafeDepositBoxRecord> getUserAssociatedSafeDepositBoxes(final Set<String> userGroups) {
        return safeDepositBoxMapper.getUserAssociatedSafeDepositBoxes(userGroups);
    }

    public List<SafeDepositBoxRecord> getUserAssociatedSafeDepositBoxesIgnoreCase(final Set<String> userGroups) {
        return safeDepositBoxMapper.getUserAssociatedSafeDepositBoxesIgnoreCase(userGroups);
    }

    public List<SafeDepositBoxRecord> getIamPrincipalAssociatedSafeDepositBoxes(final String iamPrincipalArn) {
        return safeDepositBoxMapper.getIamPrincipalAssociatedSafeDepositBoxes(iamPrincipalArn);
    }

    public List<SafeDepositBoxRecord> getSafeDepositBoxes(final int limit, final int offset) {
        return safeDepositBoxMapper.getSafeDepositBoxes(limit, offset);
    }

    public Integer getSafeDepositBoxCount() {
        return safeDepositBoxMapper.count();
    }

    public Optional<SafeDepositBoxRecord> getSafeDepositBox(final String id) {
        return Optional.ofNullable(safeDepositBoxMapper.getSafeDepositBox(id));
    }

    public boolean isPathInUse(final String path) {
        return safeDepositBoxMapper.countByPath(path) > 0;
    }

    public int createSafeDepositBox(final SafeDepositBoxRecord safeDepositBox) {
        return safeDepositBoxMapper.createSafeDepositBox(safeDepositBox);
    }

    public int updateSafeDepositBox(final SafeDepositBoxRecord safeDepositBox) {
        return safeDepositBoxMapper.updateSafeDepositBox(safeDepositBox);
    }

    public int fullUpdateSafeDepositBox(final SafeDepositBoxRecord safeDepositBox) {
        return safeDepositBoxMapper.fullUpdateSafeDepositBox(safeDepositBox);
    }

    public int deleteSafeDepositBox(final String id) {
        return safeDepositBoxMapper.deleteSafeDepositBox(id);
    }

    public String getSafeDepositBoxIdByName(String name) {
        return safeDepositBoxMapper.getSafeDepositBoxIdByName(name);
    }

    public String getSafeDepositBoxNameById(String id) {
        return safeDepositBoxMapper.getSafeDepositBoxNameById(id);
    }

    public String getSafeDepositBoxIdByPath(String path) {
        return safeDepositBoxMapper.getSafeDepositBoxIdByPath(path);
    }
}
