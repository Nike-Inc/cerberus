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

package com.nike.cerberus.server.config.guice;

import com.nike.cerberus.service.ConfigService;
import com.typesafe.config.Config;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.mybatis.guice.MyBatisModule;
import org.mybatis.guice.datasource.c3p0.C3p0DataSourceProvider;

/**
 * Guice module for configuring the JDBC data source and MyBatis.
 */
public class CmsMyBatisModule extends MyBatisModule {

    private final Config config = ConfigService.getInstance().getAppConfigMergedWithCliGeneratedProperties();

    @Override
    protected void initialize() {
        bindDataSourceProviderType(C3p0DataSourceProvider.class);
        bindTransactionFactoryType(JdbcTransactionFactory.class);

        addSimpleAliases("com.nike.cerberus.record");
        addMapperClasses("com.nike.cerberus.mapper");
        useCacheEnabled(config.getBoolean("cms.mybatis.cache.enabled"));
        failFast(true);
    }
}
