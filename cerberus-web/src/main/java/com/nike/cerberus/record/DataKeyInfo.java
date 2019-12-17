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

import com.nike.cerberus.domain.Source;
import java.time.OffsetDateTime;

public class DataKeyInfo {

  private String id;
  private Source source;
  private OffsetDateTime lastRotatedTs;

  public String getId() {
    return id;
  }

  public DataKeyInfo setId(String id) {
    this.id = id;
    return this;
  }

  public Source getSource() {
    return source;
  }

  public DataKeyInfo setSource(Source source) {
    this.source = source;
    return this;
  }

  public OffsetDateTime getLastRotatedTs() {
    return lastRotatedTs;
  }

  public DataKeyInfo setLastRotatedTs(OffsetDateTime lastRotatedTs) {
    this.lastRotatedTs = lastRotatedTs;
    return this;
  }
}
