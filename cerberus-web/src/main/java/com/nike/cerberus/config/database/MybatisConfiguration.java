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

import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@MapperScan("com.nike.cerberus.mapper")
public class MybatisConfiguration {

  @Bean
  ConfigurationCustomizer mybatisConfigurationCustomizer(
      @Value("${cerberus.mybatis.cache.enabled:#{false}}") boolean isCacheEnabled) {
    return configuration -> {
      configuration.setCacheEnabled(isCacheEnabled);
      // https://github.com/mybatis/mybatis-3/issues/1751
      configuration
          .getTypeHandlerRegistry()
          .register(OffsetDateTime.class, new OffsetDateTimeTypeHandler());
    };
  }
}
