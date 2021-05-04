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

package com.nike.cerberus.record;

import com.nike.cerberus.domain.SecureDataType;
import java.time.OffsetDateTime;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

  public byte[] getEncryptedBlob() {
    return encryptedBlob != null ? Arrays.copyOf(encryptedBlob, encryptedBlob.length) : null;
  }

  public SecureDataRecord setEncryptedBlob(byte[] encryptedBlob) {
    this.encryptedBlob =
        encryptedBlob != null ? Arrays.copyOf(encryptedBlob, encryptedBlob.length) : null;
    return this;
  }

  public static class SecureDataRecordBuilder {
    private byte[] encryptedBlob;

    public SecureDataRecordBuilder encryptedBlob(byte[] blob) {
      this.encryptedBlob = blob != null ? Arrays.copyOf(blob, blob.length) : null;
      return this;
    }
  }
}
