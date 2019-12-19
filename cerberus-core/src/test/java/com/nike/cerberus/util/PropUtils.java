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
