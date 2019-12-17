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

package com.nike.cerberus.domain;

import java.util.List;

public class SecureFileSummaryResult {

  private boolean hasNext = false;
  private Integer nextOffset = null;
  private int limit = 0;
  private int offset = 0;
  private int fileCountInResult;
  private int totalFileCount;
  private List<SecureFileSummary> secureFileSummaries;

  public boolean isHasNext() {
    return hasNext;
  }

  public SecureFileSummaryResult setHasNext(boolean hasNext) {
    this.hasNext = hasNext;
    return this;
  }

  public Integer getNextOffset() {
    return nextOffset;
  }

  public SecureFileSummaryResult setNextOffset(Integer nextOffset) {
    this.nextOffset = nextOffset;
    return this;
  }

  public int getLimit() {
    return limit;
  }

  public SecureFileSummaryResult setLimit(int limit) {
    this.limit = limit;
    return this;
  }

  public int getOffset() {
    return offset;
  }

  public SecureFileSummaryResult setOffset(int offset) {
    this.offset = offset;
    return this;
  }

  public int getFileCountInResult() {
    return fileCountInResult;
  }

  public SecureFileSummaryResult setFileCountInResult(int fileCountInResult) {
    this.fileCountInResult = fileCountInResult;
    return this;
  }

  public int getTotalFileCount() {
    return totalFileCount;
  }

  public SecureFileSummaryResult setTotalFileCount(int totalFileCount) {
    this.totalFileCount = totalFileCount;
    return this;
  }

  public List<SecureFileSummary> getSecureFileSummaries() {
    return secureFileSummaries;
  }

  public SecureFileSummaryResult setSecureFileSummaries(
      List<SecureFileSummary> secureFileSummaries) {
    this.secureFileSummaries = secureFileSummaries;
    return this;
  }
}
