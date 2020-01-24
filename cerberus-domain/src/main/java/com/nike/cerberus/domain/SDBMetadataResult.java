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

public class SDBMetadataResult {

  private boolean hasNext = false;
  private int nextOffset = 0;
  private int limit = 0;
  private int offset = 0;
  private int sdbCountInResult;
  private int totalSDBCount;
  private List<SDBMetadata> safeDepositBoxMetadata;

  public boolean isHasNext() {
    return hasNext;
  }

  public void setHasNext(boolean hasNext) {
    this.hasNext = hasNext;
  }

  public int getNextOffset() {
    return nextOffset;
  }

  public void setNextOffset(int nextOffset) {
    this.nextOffset = nextOffset;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public int getSdbCountInResult() {
    return sdbCountInResult;
  }

  public void setSdbCountInResult(int sdbCountInResult) {
    this.sdbCountInResult = sdbCountInResult;
  }

  public int getTotalSDBCount() {
    return totalSDBCount;
  }

  public void setTotalSDBCount(int totalSDBCount) {
    this.totalSDBCount = totalSDBCount;
  }

  public List<SDBMetadata> getSafeDepositBoxMetadata() {
    return safeDepositBoxMetadata;
  }

  public void setSafeDepositBoxMetadata(List<SDBMetadata> safeDepositBoxMetadata) {
    this.safeDepositBoxMetadata = safeDepositBoxMetadata;
  }
}
