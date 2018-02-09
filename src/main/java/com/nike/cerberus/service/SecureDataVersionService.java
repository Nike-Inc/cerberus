/*
 * Copyright (c) 2018 Nike, Inc.
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
 *
 */

package com.nike.cerberus.service;


import com.google.common.collect.Lists;
import com.nike.cerberus.dao.SecureDataVersionDao;
import com.nike.cerberus.domain.SecureDataVersionSummary;
import com.nike.cerberus.record.SecureDataRecord;
import com.nike.cerberus.record.SecureDataVersionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class SecureDataVersionService {

    // the default ID for current versions of secure data
    public static final String DEFAULT_ID_FOR_CURRENT_VERSIONS = "CURRENT";

    private final SecureDataVersionDao secureDataVersionDao;
    private final SecureDataService secureDataService;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public SecureDataVersionService(SecureDataVersionDao secureDataVersionDao,
                                    SecureDataService secureDataService) {
        this.secureDataVersionDao = secureDataVersionDao;
        this.secureDataService = secureDataService;
    }

    public List<SecureDataVersionSummary> getSecureDataVersionSummariesByPath(String pathToSecureData) {
        List<SecureDataVersionSummary> secureDataVersionSummaries = Lists.newArrayList();

        // retrieve previous secrets versions from the secure data versions table
        List<SecureDataVersionRecord> secureDataVersions = secureDataVersionDao.listSecureDataVersionByPath(pathToSecureData);

        // retrieve current secrets versions from secure data table
        Optional<SecureDataVersionRecord> currentSecureDataVersionOpt = getCurrentSecureDataVersion(pathToSecureData);
        if (currentSecureDataVersionOpt.isPresent()) {
            SecureDataVersionRecord currentSecureDataVersion = currentSecureDataVersionOpt.get();
            secureDataVersions.add(currentSecureDataVersion);
        }  // else, the secret has been deleted and the last version should already be in the list

        secureDataVersions.forEach(sdbRecord -> {
            secureDataVersionSummaries.add(new SecureDataVersionSummary()
                    .setAction(sdbRecord.getAction())
                    .setActionPrincipal(sdbRecord.getActionPrincipal())
                    .setActionTs(sdbRecord.getActionTs())
                    .setId(sdbRecord.getId())
                    .setPath(sdbRecord.getPath())
                    .setSdboxId(sdbRecord.getSdboxId())
                    .setVersionCreatedBy(sdbRecord.getVersionCreatedBy())
                    .setVersionCreatedTs(sdbRecord.getVersionCreatedTs())
            );
        });

        return secureDataVersionSummaries;
    }

    private Optional<SecureDataVersionRecord> getCurrentSecureDataVersion(String pathToSecureData) {
        Optional<SecureDataRecord> currentSecureDataRecordOpt = secureDataService.getSecureDataRecordForPath(pathToSecureData);
        SecureDataVersionRecord newSecureDataVersionRecord = null;

        if (currentSecureDataRecordOpt.isPresent()) {
            SecureDataRecord currentSecureDataRecord = currentSecureDataRecordOpt.get();
            SecureDataVersionRecord.SecretsAction action =
                    secureDataService.hasSecureDataBeenUpdated(currentSecureDataRecord) ?
                        SecureDataVersionRecord.SecretsAction.UPDATE :
                        SecureDataVersionRecord.SecretsAction.CREATE;

            newSecureDataVersionRecord = new SecureDataVersionRecord()
                    .setId(DEFAULT_ID_FOR_CURRENT_VERSIONS)
                    .setAction(action.name())
                    .setVersionCreatedBy(currentSecureDataRecord.getLastUpdatedBy())
                    .setVersionCreatedTs(currentSecureDataRecord.getLastUpdatedTs())
                    .setActionPrincipal(currentSecureDataRecord.getLastUpdatedBy())
                    .setActionTs(currentSecureDataRecord.getLastUpdatedTs())
                    .setSdboxId(currentSecureDataRecord.getSdboxId())
                    .setPath(currentSecureDataRecord.getPath());
        }

        return Optional.ofNullable(newSecureDataVersionRecord);
    }
}
