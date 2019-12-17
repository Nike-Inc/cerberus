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
