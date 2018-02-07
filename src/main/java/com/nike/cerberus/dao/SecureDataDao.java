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

import com.nike.cerberus.mapper.SecureDataMapper;
import com.nike.cerberus.record.SecureDataRecord;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

public class SecureDataDao {

    private final SecureDataMapper secureDataMapper;

    @Inject
    public SecureDataDao(SecureDataMapper secureDataMapper) {
        this.secureDataMapper = secureDataMapper;
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

    public void updateSecureData(String sdbId,
                                 String path,
                                 String encryptedPayload,
                                 int topLevelKVPairCount,
                                 String createdBy,
                                 OffsetDateTime createdTs,
                                 String lastUpdatedBy,
                                 OffsetDateTime lastUpdatedTs) {

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

    public Set<String> getPathsBySdbId(String sdbId) {
        return secureDataMapper.getPathsBySdbId(sdbId);
    }

    public int getTotalNumberOfDataNodes() {
        return secureDataMapper.getTotalNumberOfDataNodes();
    }

    public void deleteAllSecretsThatStartWithGivenPartialPath(String partialPath) {
        secureDataMapper.deleteAllSecretsThatStartWithGivenPartialPath(partialPath);
    }

    public void deleteSecret(String path) {
        secureDataMapper.deleteSecret(path);
    }

    public int getSumTopLevelKeyValuePairs() {
        Integer val = secureDataMapper.getSumTopLevelKeyValuePairs();
        return val == null ? 0 : val;
    }
}
