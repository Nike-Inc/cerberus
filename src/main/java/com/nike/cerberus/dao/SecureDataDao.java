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

package com.nike.cerberus.dao;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.mapper.SecureDataMapper;
import com.nike.cerberus.record.SecureDataRecord;
import com.nike.cerberus.record.SecureDataVersionRecord;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Optional;

public class SecureDataDao {

    private final SecureDataMapper secureDataMapper;

    private final SecureDataVersionDao secureDataVersionDao;

    @Inject
    public SecureDataDao(SecureDataMapper secureDataMapper, SecureDataVersionDao secureDataVersionDao) {
        this.secureDataMapper = secureDataMapper;
        this.secureDataVersionDao = secureDataVersionDao;
    }

    public void writeSecureData(String sdbId, String path, String encryptedPayload, int topLevelKVPairCount,
                                String createdBy,
                                OffsetDateTime createdTs,
                                String lastUpdatedBy,
                                OffsetDateTime lastUpdatedTs) {
        secureDataMapper.writeSecureData(new SecureDataRecord()
                .setId(path.hashCode())
                .setPath(path)
                .setSdboxId(sdbId)
                .setEncryptedBlob(encryptedPayload)
                .setTopLevelKVCount(topLevelKVPairCount)
                .setCreatedBy(createdBy)
                .setCreatedTs(createdTs)
                .setLastUpdatedBy(lastUpdatedBy)
                .setLastUpdatedTs(lastUpdatedTs)
        );
    }

    public void updateSecureData(String sdbId, String path, String encryptedPayload, int topLevelKVPairCount,
                                 String createdBy,
                                 OffsetDateTime createdTs,
                                 String lastUpdatedBy,
                                 OffsetDateTime lastUpdatedTs) {
        secureDataVersionDao.writeSecureDataVersion(sdbId, path, encryptedPayload,
                SecureDataVersionRecord.SecretsAction.UPDATE,
                createdBy,
                createdTs,
                lastUpdatedBy,
                lastUpdatedTs
        );

        secureDataMapper.updateSecureData(new SecureDataRecord()
                .setId(path.hashCode())
                .setPath(path)
                .setSdboxId(sdbId)
                .setEncryptedBlob(encryptedPayload)
                .setTopLevelKVCount(topLevelKVPairCount)
                .setCreatedBy(createdBy)
                .setCreatedTs(createdTs)
                .setLastUpdatedTs(lastUpdatedTs)
                .setLastUpdatedBy(lastUpdatedBy)

        );
    }

    public Optional<SecureDataRecord> readSecureDataByPath(String path) {
        return Optional.ofNullable(secureDataMapper.readSecureDataByPath(path));
    }

    public String[] getPathsByPartialPath(String partialPath) {
        return secureDataMapper.getPathsByPartialPath(partialPath);
    }

    public int getTotalNumberOfDataNodes() {
        return secureDataMapper.getTotalNumberOfDataNodes();
    }

    public void deleteAllSecretsThatStartWithGivenPartialPath(String partialPath) {
        secureDataMapper.deleteAllSecretsThatStartWithGivenPartialPath(partialPath);
    }

    public void deleteSecret(String path, String lastUpdatedBy, OffsetDateTime lastUpdatedTs) {
        SecureDataRecord secureDataRecord = readSecureDataByPath(path)
                .orElseThrow(() ->
                    new ApiException(DefaultApiError.ENTITY_NOT_FOUND)
                );

        secureDataVersionDao.writeSecureDataVersion(
                secureDataRecord.getSdboxId(),
                secureDataRecord.getPath(),
                secureDataRecord.getEncryptedBlob(),
                SecureDataVersionRecord.SecretsAction.DELETE,
                secureDataRecord.getCreatedBy(),
                secureDataRecord.getCreatedTs(),
                lastUpdatedBy,
                lastUpdatedTs
        );

        secureDataMapper.deleteSecret(path);
    }

    public int getSumTopLevelKeyValuePairs() {
        Integer val = secureDataMapper.getSumTopLevelKeyValuePairs();
        return val == null ? 0 : val;
    }
}
