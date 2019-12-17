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

package com.nike.cerberus.record;

import com.nike.cerberus.domain.SecureDataType;
import java.time.OffsetDateTime;
import java.util.Arrays;

public class SecureDataRecord {

  private Integer id;
  private String sdboxId;
  private String path;
  private byte[] encryptedBlob;
  private SecureDataType type;
  private int sizeInBytes;
  private Integer topLevelKVCount;
  private OffsetDateTime createdTs;
  private String createdBy;
  private OffsetDateTime lastUpdatedTs;
  private String lastUpdatedBy;
  private OffsetDateTime lastRotatedTs;

  public Integer getId() {
    return id;
  }

  public SecureDataRecord setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getSdboxId() {
    return sdboxId;
  }

  public SecureDataRecord setSdboxId(String sdboxId) {
    this.sdboxId = sdboxId;
    return this;
  }

  public String getPath() {
    return path;
  }

  public SecureDataRecord setPath(String path) {
    this.path = path;
    return this;
  }

  public byte[] getEncryptedBlob() {
    return encryptedBlob != null ? Arrays.copyOf(encryptedBlob, encryptedBlob.length) : null;
  }

  public SecureDataRecord setEncryptedBlob(byte[] encryptedBlob) {
    this.encryptedBlob =
        encryptedBlob != null ? Arrays.copyOf(encryptedBlob, encryptedBlob.length) : null;
    return this;
  }

  public SecureDataType getType() {
    return type;
  }

  public SecureDataRecord setType(SecureDataType type) {
    this.type = type;
    return this;
  }

  public int getSizeInBytes() {
    return sizeInBytes;
  }

  public SecureDataRecord setSizeInBytes(int sizeInBytes) {
    this.sizeInBytes = sizeInBytes;
    return this;
  }

  public Integer getTopLevelKVCount() {
    return topLevelKVCount;
  }

  public SecureDataRecord setTopLevelKVCount(Integer topLevelKVCount) {
    this.topLevelKVCount = topLevelKVCount;
    return this;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  public SecureDataRecord setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
    return this;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public SecureDataRecord setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public OffsetDateTime getLastUpdatedTs() {
    return lastUpdatedTs;
  }

  public SecureDataRecord setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
    this.lastUpdatedTs = lastUpdatedTs;
    return this;
  }

  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public SecureDataRecord setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
    return this;
  }

  public OffsetDateTime getLastRotatedTs() {
    return lastRotatedTs;
  }

  public SecureDataRecord setLastRotatedTs(OffsetDateTime lastRotatedTs) {
    this.lastRotatedTs = lastRotatedTs;
    return this;
  }
}
