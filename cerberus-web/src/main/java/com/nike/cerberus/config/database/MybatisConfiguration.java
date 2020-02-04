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

package com.nike.cerberus.config.database;

import com.nike.cerberus.cache.DatabaseCache;
import com.nike.cerberus.metric.MetricsService;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
@MapperScan("com.nike.cerberus.mapper")
public class MybatisConfiguration {

  protected static final String GLOBAL_DATA_TTL_IN_SECONDS =
      "cerberus.mybatis.cache.global.dataTtlInSeconds";
  protected static final String DATA_TTL_IN_SECONDS_OVERRIDE_PATH_TEMPLATE =
      "cerberus.mybatis.cache.%s.dataTtlInSeconds";
  protected static final String GLOBAL_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS =
      "cerberus.mybatis.cache.global.repeatReadCounterResetInSeconds";
  protected static final String REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS_OVERRIDE_PATH_TEMPLATE =
      "cerberus.mybatis.cache.%s.repeatReadCounterResetInSeconds";
  protected static final String GLOBAL_REPEAT_READ_THRESHOLD =
      "cerberus.mybatis.cache.global.repeatReadThreshold";
  protected static final String REPEAT_READ_THRESHOLD_OVERRIDE_PATH_TEMPLATE =
      "cerberus.mybatis.cache.%s.repeatReadThreshold";
  protected static final int DEFAULT_GLOBAL_DATA_TTL_IN_SECONDS = 10;
  protected static final int DEFAULT_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS = 2;
  protected static final int DEFAULT_REPEAT_READ_THRESHOLD = 2;

  /**
   * @param id The id for the mapper
   * @return The amount of time in seconds that the mapper cache will keep an item in memory before
   *     it purges itself.
   */
  protected int getExpireTimeInSeconds(Environment environment, String id) {
    var globalExpireTimeInSeconds =
        environment.getProperty(
            GLOBAL_DATA_TTL_IN_SECONDS, Integer.class, DEFAULT_GLOBAL_DATA_TTL_IN_SECONDS);
    String globalDataTtlInSecondsOverridePathTemplate =
        String.format(DATA_TTL_IN_SECONDS_OVERRIDE_PATH_TEMPLATE, id);
    return environment.getProperty(
        globalDataTtlInSecondsOverridePathTemplate, Integer.class, globalExpireTimeInSeconds);
  }

  /**
   * @param id The id for the mapper
   * @return The amount of time in seconds that must pass without consecutive reads to reset the
   *     counter.
   */
  protected int getRepeatReadCounterExpireTimeInSeconds(Environment environment, String id) {
    int globalCounterExpireTimeInSeconds =
        environment.getProperty(
            GLOBAL_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS,
            Integer.class,
            DEFAULT_REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS);
    String counterMapperOverrideTtlPath =
        String.format(REPEAT_READ_COUNTER_EXPIRE_IN_SECONDS_OVERRIDE_PATH_TEMPLATE, id);
    return environment.getProperty(
        counterMapperOverrideTtlPath, Integer.class, globalCounterExpireTimeInSeconds);
  }

  /**
   * @param id The id for the mapper
   * @return The number of reads that must be exceeding while counts are being chained before
   *     caching of that object is enabled.
   */
  protected int getRepeatReadThreshold(Environment environment, String id) {
    int globalRepeatReadThreshold =
        environment.getProperty(
            GLOBAL_REPEAT_READ_THRESHOLD, Integer.class, DEFAULT_REPEAT_READ_THRESHOLD);
    String repeatReadThresholdOverridePath =
        String.format(REPEAT_READ_THRESHOLD_OVERRIDE_PATH_TEMPLATE, id);
    return environment.getProperty(
        repeatReadThresholdOverridePath, Integer.class, globalRepeatReadThreshold);
  }

  @Bean
  ConfigurationCustomizer mybatisConfigurationCustomizer(
      @Value("${cerberus.mybatis.cache.enabled:#{false}}") boolean isCacheEnabled,
      MetricsService metricsService,
      Environment environment) {
    return configuration -> {
      configuration.setCacheEnabled(isCacheEnabled);
      // https://github.com/mybatis/mybatis-3/issues/1751
      configuration
          .getTypeHandlerRegistry()
          .register(OffsetDateTime.class, new OffsetDateTimeTypeHandler());

      // TODO, Im sure there is a dynamic way to create this list.
      List.of(
              "awsIamRoleMapper",
              "categoryMapper",
              "permissionsMapper",
              "roleMapper",
              "safeDepositBoxMapper",
              "secureDataMapper",
              "secureDataVersionMapper",
              "userGroupMapper")
          .stream()
          .forEach(
              id -> {
                var expireTimeInSeconds = getExpireTimeInSeconds(environment, id);
                var counterExpireTimeInSeconds =
                    getRepeatReadCounterExpireTimeInSeconds(environment, id);
                var repeatReadThreshold = getRepeatReadThreshold(environment, id);
                var cache =
                    new DatabaseCache(
                        id,
                        metricsService,
                        expireTimeInSeconds,
                        counterExpireTimeInSeconds,
                        repeatReadThreshold);
                configuration.addCache(cache);
              });
    };
  }
}
