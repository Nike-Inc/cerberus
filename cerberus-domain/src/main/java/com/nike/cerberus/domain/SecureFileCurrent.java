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
}
