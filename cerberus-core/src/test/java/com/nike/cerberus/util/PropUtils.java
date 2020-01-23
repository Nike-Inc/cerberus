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

public class PropUtils {
  private PropUtils() {}

  public static String getPropWithDefaultValue(String key, String defaultValue) {
    return EnvUtils.getEnvWithDefault(key, System.getProperty(key, defaultValue));
  }

  public static String getRequiredProperty(String key) {
    return getRequiredProperty(key, null);
  }

  public static String getRequiredProperty(String key, String msg) {
    String value = getPropWithDefaultValue(key, null);
    if (StringUtils.isBlank(value)) {
      StringBuilder sb =
          (new StringBuilder("The key: "))
              .append(key)
              .append(
                  " was not set or is blank. Check the environment variables and system properties");
      if (StringUtils.isNotBlank(msg)) {
        sb.append(" Msg: ").append(msg);
      }

      throw new IllegalStateException(sb.toString());
    } else {
      return value;
    }
  }
}
