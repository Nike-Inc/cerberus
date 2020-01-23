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

import java.util.List;

public class SecureDataVersionsResult {

  private boolean hasNext = false;
  private Integer nextOffset = null;
  private int limit = 0;
  private int offset = 0;
  private int versionCountInResult;
  private int totalVersionCount;
  private List<SecureDataVersionSummary> secureDataVersionSummaries;

  public boolean isHasNext() {
    return hasNext;
  }

  public SecureDataVersionsResult setHasNext(boolean hasNext) {
    this.hasNext = hasNext;
    return this;
  }

  public Integer getNextOffset() {
    return nextOffset;
  }

  public SecureDataVersionsResult setNextOffset(Integer nextOffset) {
    this.nextOffset = nextOffset;
    return this;
  }

  public int getLimit() {
    return limit;
  }

  public SecureDataVersionsResult setLimit(int limit) {
    this.limit = limit;
    return this;
  }

  public int getOffset() {
    return offset;
  }

  public SecureDataVersionsResult setOffset(int offset) {
    this.offset = offset;
    return this;
  }

  public int getVersionCountInResult() {
    return versionCountInResult;
  }

  public SecureDataVersionsResult setVersionCountInResult(int versionCountInResult) {
    this.versionCountInResult = versionCountInResult;
    return this;
  }

  public int getTotalVersionCount() {
    return totalVersionCount;
  }

  public SecureDataVersionsResult setTotalVersionCount(int totalVersionCount) {
    this.totalVersionCount = totalVersionCount;
    return this;
  }

  public List<SecureDataVersionSummary> getSecureDataVersionSummaries() {
    return secureDataVersionSummaries;
  }

  public SecureDataVersionsResult setSecureDataVersionSummaries(
      List<SecureDataVersionSummary> secureDataVersionSummaries) {
    this.secureDataVersionSummaries = secureDataVersionSummaries;
    return this;
  }
}
