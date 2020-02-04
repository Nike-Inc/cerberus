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

package com.nike.cerberus.util;

import org.apache.commons.lang3.StringUtils;

public class EnvUtils {
  private EnvUtils() {}

  public static String getRequiredEnv(String key) {
    return getRequiredEnv(key, null);
  }

  public static String getRequiredEnv(String key, String msg) {
    String value = System.getenv(key);
    if (StringUtils.isBlank(value)) {
      StringBuilder sb =
          (new StringBuilder("The required environment variable "))
              .append(key)
              .append(" was not set or is blank.");
      if (StringUtils.isNotBlank(msg)) {
        sb.append(" Msg: ").append(msg);
      }

      throw new IllegalStateException(sb.toString());
    } else {
      return value;
    }
  }

  public static String getEnvWithDefault(String key, String defaultValue) {
    String value = System.getenv(key);
    return StringUtils.isNotBlank(value) ? value : defaultValue;
  }
}
