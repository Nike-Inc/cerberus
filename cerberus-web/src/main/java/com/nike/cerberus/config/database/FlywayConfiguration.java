package com.nike.cerberus.config.database;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfiguration {

  @Autowired
  public FlywayConfiguration(
      DataSource dataSource,
      @Value("${flyway.schemas}") String flywaySchemas,
      @Value("${flyway.locations}") String flywayLocations) {

    FluentConfiguration conf =
        new FluentConfiguration()
            .dataSource(dataSource)
            .schemas(flywaySchemas)
            .table(
                "schema_version") // For some reason in guice land this is not flyway_schema_history
            .locations(flywayLocations.split(";"));

    Flyway flyway = new Flyway(conf);
    flyway.migrate();
  }
}
