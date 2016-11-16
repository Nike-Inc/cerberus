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

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.nike.cerberus.config.CmsEnvPropertiesLoader;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice module for loading the runtime properties for CMS from a Cerberus environment's config bucket.
 */
public class CmsEnvPropertiesModule extends AbstractModule {

    private static final String KMS_KEY_ID_KEY = "CONFIG_KEY_ID";

    private static final String REGION_KEY = "EC2_REGION";

    private static final String BUCKET_NAME_KEY = "CONFIG_S3_BUCKET";

    private static final String CMS_DISABLE_ENV_LOAD_FLAG = "cms.env.load.disable";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Config appConfig;

    public CmsEnvPropertiesModule(Config appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    protected void configure() {
        if (appConfig.hasPath(CMS_DISABLE_ENV_LOAD_FLAG) && appConfig.getBoolean(CMS_DISABLE_ENV_LOAD_FLAG)) {
            logger.warn("CMS environment property loading disabled.");
        } else {
            final CmsEnvPropertiesLoader cmsEnvPropertiesLoader = new CmsEnvPropertiesLoader(
                    System.getenv(BUCKET_NAME_KEY),
                    System.getenv(REGION_KEY),
                    System.getenv(KMS_KEY_ID_KEY)
            );
            Names.bindProperties(binder(), cmsEnvPropertiesLoader.getProperties());
        }
    }
}
