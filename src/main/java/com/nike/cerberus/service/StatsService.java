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

package com.nike.cerberus.service;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.dao.SafeDepositBoxDao;
import com.nike.cerberus.dao.UserGroupDao;
import com.nike.cerberus.domain.Role;
import com.nike.cerberus.domain.SafeDepositBoxStats;
import com.nike.cerberus.domain.Stats;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.RoleRecord;
import com.nike.cerberus.record.SafeDepositBoxRecord;
import com.nike.cerberus.record.UserGroupRecord;
import com.nike.cerberus.util.DateTimeSupplier;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Provides general stats about safe deposit boxes.
 */
public class StatsService {

    private final RoleService roleService;

    private final SafeDepositBoxDao safeDepositBoxDao;

    private final UserGroupDao userGroupDao;

    private final DateTimeSupplier dateTimeSupplier;

    @Inject
    public StatsService(final RoleService roleService,
                        final SafeDepositBoxDao safeDepositBoxDao,
                        final UserGroupDao userGroupDao,
                        final DateTimeSupplier dateTimeSupplier) {
        this.roleService = roleService;
        this.safeDepositBoxDao = safeDepositBoxDao;
        this.userGroupDao = userGroupDao;
        this.dateTimeSupplier = dateTimeSupplier;
    }

    public Stats getStats() {
        final Optional<Role> ownerRole = roleService.getRoleByName(RoleRecord.ROLE_OWNER);

        if (!ownerRole.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.MISCONFIGURED_APP)
                    .withExceptionMessage("Owner role doesn't exist!")
                    .build();
        }

        final Set<SafeDepositBoxStats> safeDepositBoxStats = new HashSet<>();
        final List<SafeDepositBoxRecord> safeDepositBoxRecords = safeDepositBoxDao.getSafeDepositBoxes();

        safeDepositBoxRecords.forEach(r -> {
            final List<UserGroupRecord> userGroupOwnerRecords =
                    userGroupDao.getUserGroupsByRole(r.getId(), ownerRole.get().getId());

            if (userGroupOwnerRecords.size() != 1) {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.SDB_TOO_MANY_OWNERS)
                        .withExceptionMessage("SDB has more than one owner!")
                        .build();
            }

            final SafeDepositBoxStats sdbStats = new SafeDepositBoxStats();
            sdbStats.setName(r.getName());
            sdbStats.setOwner(userGroupOwnerRecords.get(0).getName());
            sdbStats.setLastUpdatedTs(r.getLastUpdatedTs());
            safeDepositBoxStats.add(sdbStats);
        });

        return new Stats().setSafeDepositBoxStats(safeDepositBoxStats).setGeneratedTs(dateTimeSupplier.get());
    }
}
