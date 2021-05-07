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

import java.time.OffsetDateTime;
import java.util.Arrays;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SecureFileVersion implements SecureFile {

  private String id;
  private String sdboxId;
  private String path;
  private byte[] data;
  private String name;
  private int sizeInBytes;
  private String action;
  private String versionCreatedBy;
  private OffsetDateTime versionCreatedTs;
  private String actionPrincipal;
  private OffsetDateTime actionTs;

  public SecureFileVersion(
      String id,
      String sdboxId,
      String path,
      byte[] data,
      String name,
      int sizeInBytes,
      String action,
      String versionCreatedBy,
      OffsetDateTime versionCreatedTs,
      String actionPrincipal,
      OffsetDateTime actionTs) {
    this.id = id;
    this.sdboxId = sdboxId;
    this.path = path;
    this.data = Arrays.copyOf(data, data.length);
    this.name = name;
    this.sizeInBytes = sizeInBytes;
    this.action = action;
    this.versionCreatedBy = versionCreatedBy;
    this.versionCreatedTs = versionCreatedTs;
    this.actionPrincipal = actionPrincipal;
    this.actionTs = actionTs;
  }

  public byte[] getData() {
    return Arrays.copyOf(data, data.length);
  }

  public void setData(byte[] data) {
    this.data = Arrays.copyOf(data, data.length);
  }

  public static class SecureFileVersionBuilder {
    private byte[] data;

    public SecureFileVersionBuilder data(byte[] data) {
      this.data = Arrays.copyOf(data, data.length);
      ;
      return this;
    }
  }

  public enum SecretsAction {
    CREATE,
    UPDATE,
    DELETE
  }
}
