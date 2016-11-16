/*
 * Copyright (c) 2016 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.flyway;

import org.flywaydb.core.Flyway;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;

/**
 * Utilizes flyway to migrate the data source to the latest schema version at runtime.
 */
@Singleton
public class SchemaMigrator {
    private final DataSource dataSource;
    private final String flywaySchemas;
    private final String flywayLocations;

    @Inject
    public SchemaMigrator(DataSource dataSource,
                          @Named("flyway.schemas") String flywaySchemas,
                          @Named("flyway.locations") String flywayLocations) {
        this.dataSource = dataSource;
        this.flywaySchemas = flywaySchemas;
        this.flywayLocations = flywayLocations;
    }

    @Inject
    public void updateSchema() {
        Flyway flyway = new Flyway();
        flyway.setDataSource(this.dataSource);
        flyway.setSchemas(this.flywaySchemas);
        String[] parts = this.flywayLocations.split(";");
        flyway.setLocations(parts);
        flyway.migrate();
    }

}