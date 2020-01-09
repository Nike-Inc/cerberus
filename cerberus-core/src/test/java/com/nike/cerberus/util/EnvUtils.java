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
