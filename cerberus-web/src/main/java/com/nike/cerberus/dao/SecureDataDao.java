/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.dao;

import com.nike.cerberus.domain.SecureDataType;
import com.nike.cerberus.mapper.SecureDataMapper;
import com.nike.cerberus.record.DataKeyInfo;
import com.nike.cerberus.record.SecureDataRecord;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecureDataDao {

  private final SecureDataMapper secureDataMapper;

  @Autowired
  public SecureDataDao(SecureDataMapper secureDataMapper) {
    this.secureDataMapper = secureDataMapper;
  }

  public void writeSecureData(
      String sdbId,
      String path,
      byte[] encryptedPayload,
      int topLevelKVPairCount,
      SecureDataType type,
      int sizeInBytes,
      String createdBy,
      OffsetDateTime createdTs,
      String lastUpdatedBy,
      OffsetDateTime lastUpdatedTs) {
    secureDataMapper.writeSecureData(
        new SecureDataRecord()
            .setId(path.hashCode())
            .setPath(path)
            .setSdboxId(sdbId)
            .setEncryptedBlob(encryptedPayload)
            .setTopLevelKVCount(topLevelKVPairCount)
            .setSizeInBytes(sizeInBytes)
            .setType(type)
            .setCreatedBy(createdBy)
            .setCreatedTs(createdTs)
            .setLastUpdatedBy(lastUpdatedBy)
            .setLastUpdatedTs(lastUpdatedTs)
            .setLastRotatedTs(lastUpdatedTs) // This is intentional
        );
  }

  public void updateSecureData(
      String sdbId,
      String path,
      byte[] encryptedPayload,
      int topLevelKVPairCount,
      SecureDataType type,
      int sizeInBytes,
      String createdBy,
      OffsetDateTime createdTs,
      String lastUpdatedBy,
      OffsetDateTime lastUpdatedTs,
      OffsetDateTime lastRotatedTs) {

    secureDataMapper.updateSecureData(
        new SecureDataRecord()
            .setId(path.hashCode())
            .setPath(path)
            .setSdboxId(sdbId)
            .setEncryptedBlob(encryptedPayload)
            .setTopLevelKVCount(topLevelKVPairCount)
            .setType(type)
            .setSizeInBytes(sizeInBytes)
            .setCreatedBy(createdBy)
            .setCreatedTs(createdTs)
            .setLastUpdatedTs(lastUpdatedTs)
            .setLastUpdatedBy(lastUpdatedBy)
            .setLastRotatedTs(lastRotatedTs));
  }

  public int updateSecureData(SecureDataRecord secureDataRecord) {
    return secureDataMapper.updateSecureData(secureDataRecord);
  }

  public Optional<SecureDataRecord> readSecureDataByPath(String sdbId, String path) {
    return Optional.ofNullable(secureDataMapper.readSecureDataByPath(sdbId, path));
  }

  public Optional<SecureDataRecord> readSecureDataByIdLocking(String id) {
    return Optional.ofNullable(secureDataMapper.readSecureDataByIdLocking(id));
  }

  public Optional<SecureDataRecord> readSecureDataByPathAndType(
      String sdbId, String path, SecureDataType type) {
    return Optional.ofNullable(secureDataMapper.readSecureDataByPathAndType(sdbId, path, type));
  }

  public Optional<SecureDataRecord> readMetadataByPathAndType(
      String sdbId, String path, SecureDataType type) {
    return Optional.ofNullable(secureDataMapper.readMetadataByPathAndType(sdbId, path, type));
  }

  public String[] getPathsByPartialPath(String sdbId, String partialPath) {
    return secureDataMapper.getPathsByPartialPath(sdbId, partialPath);
  }

  public String[] getPathsByPartialPathAndType(
      String sdbId, String partialPath, SecureDataType type) {
    return secureDataMapper.getPathsByPartialPathAndType(sdbId, partialPath, type);
  }

  public Set<String> getPathsBySdbId(String sdbId) {
    return secureDataMapper.getPathsBySdbId(sdbId);
  }

  public List<SecureDataRecord> listSecureDataByPartialPathAndType(
      String sdbId, String partialPath, SecureDataType type, int limit, int offset) {
    return secureDataMapper.listSecureDataByPartialPathAndType(
        sdbId, partialPath, type, limit, offset);
  }

  public int countByPartialPathAndType(String partialPath, SecureDataType type) {
    return secureDataMapper.countByPartialPathAndType(partialPath, type);
  }

  public int countByType(SecureDataType type) {
    return secureDataMapper.countByType(type);
  }

  public int getTotalNumberOfDataNodes() {
    return secureDataMapper.getTotalNumberOfDataNodes();
  }

  public void deleteAllSecretsThatStartWithGivenPartialPath(String sdbId, String partialPath) {
    secureDataMapper.deleteAllSecretsThatStartWithGivenPartialPath(sdbId, partialPath);
  }

  public void deleteSecret(String sdbId, String path) {
    secureDataMapper.deleteSecret(sdbId, path);
  }

  public int getSumTopLevelKeyValuePairs() {
    Integer val = secureDataMapper.getSumTopLevelKeyValuePairs();
    return val == null ? 0 : val;
  }

  public List<DataKeyInfo> getOldestDataKeyInfo(OffsetDateTime dateTime, int limit) {
    return secureDataMapper.getOldestDataKeyInfo(dateTime, limit);
  }
}
