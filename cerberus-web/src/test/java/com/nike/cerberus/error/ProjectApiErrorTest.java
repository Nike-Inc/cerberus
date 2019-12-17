/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Tests the functionality of {@link ProjectApiError}
 *
 * @author Nic Munroe
 */
public class ProjectApiErrorTest {

  @Test
  public void make_code_coverage_happy() {
    // Some code coverage tools force you to exercise valueOf() (for example) or you get uncovered
    // lines.
    for (DefaultApiError error : DefaultApiError.values()) {
      assertThat(DefaultApiError.valueOf(error.getName())).isEqualTo(error);
    }
  }
}
