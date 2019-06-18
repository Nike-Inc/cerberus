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

package com.nike.cerberus.dao;

import com.nike.cerberus.domain.SecureDataType;
import com.nike.cerberus.mapper.SecureDataVersionMapper;
import com.nike.cerberus.record.SecureDataVersionRecord;
import com.nike.cerberus.util.UuidSupplier;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SecureDataVersionDao {

    private final SecureDataVersionMapper secureDataVersionMapper;

    private final UuidSupplier uuidSupplier;

    @Inject
    public SecureDataVersionDao(SecureDataVersionMapper secureDataVersionMapper, UuidSupplier uuidSupplier) {
        this.secureDataVersionMapper = secureDataVersionMapper;
        this.uuidSupplier = uuidSupplier;
    }

    public void writeSecureDataVersion(String sdbId,
                                       String path,
                                       byte[] encryptedPayload,
                                       SecureDataVersionRecord.SecretsAction action,
                                       SecureDataType type,
                                       int sizeInBytes,
                                       String versionCreatedBy,
                                       OffsetDateTime versionCreatedTs,
                                       String actionPrincipal,
                                       OffsetDateTime actionTs) {

        secureDataVersionMapper.writeSecureDataVersion(new SecureDataVersionRecord()
                    .setId(uuidSupplier.get())
                    .setPath(path)
                    .setSdboxId(sdbId)
                    .setEncryptedBlob(encryptedPayload)
                    .setType(type)
                    .setSizeInBytes(sizeInBytes)
                    .setAction(action.name())
                    .setActionPrincipal(actionPrincipal)
                    .setActionTs(actionTs)
                    .setVersionCreatedBy(versionCreatedBy)
                    .setVersionCreatedTs(versionCreatedTs)
        );
    }

    public void updateSecureDataVersion(SecureDataVersionRecord secureDataVersionRecord) {
        secureDataVersionMapper.updateSecureDataVersion(secureDataVersionRecord);
    }

    public Integer getTotalNumVersionsForPath(String path) {
        return secureDataVersionMapper.getTotalNumVersionsForPath(path);
    }

    public List<SecureDataVersionRecord> listSecureDataVersionByPath(String path, int limit, int offset) {
        return secureDataVersionMapper.listSecureDataVersionsByPath(path, limit, offset);
    }

    public Optional<SecureDataVersionRecord> readSecureDataVersionById(String id) {
        return Optional.ofNullable(secureDataVersionMapper.readSecureDataVersionById(id));
    }

    public String[] getVersionPathsByPartialPath(String partialPath) {
        return secureDataVersionMapper.getVersionPathsByPartialPath(partialPath);
    }

    public Set<String> getVersionPathsBySdbId(String sdbId) {
        return secureDataVersionMapper.getVersionPathsBySdbId(sdbId);
    }

    public void deleteAllVersionsThatStartWithPartialPath(String partialPath) {
        secureDataVersionMapper.deleteAllVersionsThatStartWithPartialPath(partialPath);
    }

    public List<SecureDataVersionRecord> getOldestSecureDataVersion(OffsetDateTime dateTime, int limit) {
        return secureDataVersionMapper.getOldestSecureDataVersion(dateTime, limit);
    }
}
