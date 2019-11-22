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

package com.nike.cerberus.mapper;

import com.nike.cerberus.record.SafeDepositBoxRecord;
import com.nike.cerberus.record.SafeDepositBoxRoleRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * MyBatis mapper for executing SQL queries related to safe deposit boxes.
 */
public interface SafeDepositBoxMapper {

    List<SafeDepositBoxRoleRecord> getUserAssociatedSafeDepositBoxRoles(@Param("userGroups") final Set<String> userGroups);

    List<SafeDepositBoxRoleRecord> getIamRoleAssociatedSafeDepositBoxRoles(@Param("awsIamRoleArn") final String awsIamRoleArn,
                                                                           @Param("iamRootArn") final String iamRootArn);

    List<SafeDepositBoxRoleRecord> getIamAssumedRoleAssociatedSafeDepositBoxRoles(@Param("iamAssumedRoleArn") final String iamAssumedRoleArn,
                                                                                  @Param("awsIamRoleArn") final String awsIamRoleArn,
                                                                                  @Param("iamRootArn") final String iamRootArn);

    List<SafeDepositBoxRecord> getUserAssociatedSafeDepositBoxes(@Param("userGroups") Set<String> userGroups);

    List<SafeDepositBoxRecord> getUserAssociatedSafeDepositBoxesIgnoreCase(@Param("userGroups") Set<String> userGroups);

    List<SafeDepositBoxRecord> getIamPrincipalAssociatedSafeDepositBoxes(@Param("iamPrincipalArn") final String iamPrincipalArn,
                                                                         @Param("iamRootArn") final String iamRootArn);

    List<SafeDepositBoxRecord> getIamAssumedRoleAssociatedSafeDepositBoxes(@Param("iamAssumedRoleArn") final String iamAssumedRoleArn,
                                                                           @Param("iamRoleArn") final String iamRoleArn,
                                                                           @Param("iamRootArn") final String iamRootArn);

    SafeDepositBoxRecord getSafeDepositBox(@Param("id") String id);

    int countByPath(@Param("path") String path);

    int countBySdbNameSlug(@Param("sdbNameSlug") String sdbNameSlug);

    int createSafeDepositBox(@Param("record") SafeDepositBoxRecord record);

    int updateSafeDepositBox(@Param("record") SafeDepositBoxRecord record);

    int fullUpdateSafeDepositBox(@Param("record") SafeDepositBoxRecord record);

    int deleteSafeDepositBox(@Param("id") String id);

    List<SafeDepositBoxRecord> getSafeDepositBoxes(@Param("limit") int limit, @Param("offset") int offset);

    int count();

    String getSafeDepositBoxIdByName(@Param("name") String name);

    String getSafeDepositBoxNameById(@Param("id") String id);

    String getSafeDepositBoxIdByPath(@Param("path") String path);
}
