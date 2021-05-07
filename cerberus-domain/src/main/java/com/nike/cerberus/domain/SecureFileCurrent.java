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

package com.nike.cerberus.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;
import java.util.Arrays;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SecureFileCurrent implements SecureFile {

  @JsonIgnore private String id;
  private String sdboxId;
  private String path;
  @JsonIgnore private byte[] data;
  private int sizeInBytes;
  private String name;
  private String createdBy;
  private OffsetDateTime createdTs;
  private String lastUpdatedBy;
  private OffsetDateTime lastUpdatedTs;

  public SecureFileCurrent() {}

  public SecureFileCurrent(
      String id,
      String sdboxId,
      String path,
      byte[] data,
      int sizeInBytes,
      String name,
      String createdBy,
      OffsetDateTime createdTs,
      String lastUpdatedBy,
      OffsetDateTime lastUpdatedTs) {
    this.id = id;
    this.sdboxId = sdboxId;
    this.path = path;
    this.data = data != null ? Arrays.copyOf(data, data.length) : null;
    ;
    this.sizeInBytes = sizeInBytes;
    this.name = name;
    this.createdBy = createdBy;
    this.createdTs = createdTs;
    this.lastUpdatedBy = lastUpdatedBy;
    this.lastUpdatedTs = lastUpdatedTs;
  }

  public byte[] getData() {
    return data != null ? Arrays.copyOf(data, data.length) : null;
  }

  public void setData(byte[] data) {
    this.data = data != null ? Arrays.copyOf(data, data.length) : null;
  }

  public static class SecureFileCurrentBuilder {
    private byte[] data;

    public SecureFileCurrentBuilder data(byte[] data) {
      this.data = Arrays.copyOf(data, data.length);
      return this;
    }
  }
}
