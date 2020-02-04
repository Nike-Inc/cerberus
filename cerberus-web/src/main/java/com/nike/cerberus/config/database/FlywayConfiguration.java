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
