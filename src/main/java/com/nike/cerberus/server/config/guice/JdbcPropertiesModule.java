package com.nike.cerberus.server.config.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.nike.cerberus.service.ConfigService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;

public class JdbcPropertiesModule extends AbstractModule {

  /**
   * Attempts to fetch the region specific JDBC URL first, if not present fall back to the globally defined
   * JDBC url (The write cluster for the primary region).
   *
   * @return The resolved jdbc url
   */
  @Provides
  @Singleton
  @Named("JDBC.url")
  public String jdbcUrl() {
    ConfigService configService = ConfigService.getInstance();
    Config config = configService.getAppConfigMergedWithCliGeneratedProperties();
    String regionKey = Optional.ofNullable(configService.getSetRegion()).orElse("region_env_var_not_set");
    try {
      return config.getString(String.format("JDBC.%s.url", regionKey));
    } catch (ConfigException e) {
      return config.getString("JDBC.url");
    }
  }
}
