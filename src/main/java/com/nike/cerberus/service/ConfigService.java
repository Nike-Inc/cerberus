/*
 * Copyright (c) 2018 Nike, Inc.
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

package com.nike.cerberus.service;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.nike.cerberus.config.CmsEnvPropertiesLoader;
import com.nike.internal.util.Pair;
import com.nike.riposte.typesafeconfig.util.TypesafeConfigUtil;
import com.nike.riposte.util.MainClassUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuration Service for retrieving the TypeSafe Config merged with S3 cli generated properties
 */
public class ConfigService {

    private static ConfigService instance = null;

    private static final String REGION_KEY = "EC2_REGION";
    private static final String BUCKET_NAME_KEY = "CONFIG_S3_BUCKET";
    private static final String CMS_DISABLE_ENV_LOAD_FLAG = "cms.env.load.disable";
    private static final String AUDIT_LOGGING_ENABLED_KEY = "cms.event.processors.com.nike.cerberus.event.processor.AuditLogProcessor";
    private static final String AUDIT_BUCKET_CONFIG_KEY = "cms.audit.bucket";
    private static final String AUDIT_BUCKET_REGION_CONFIG_KEY = "cms.audit.bucket_region";

    private final Config mergedConfig;
    private CmsEnvPropertiesLoader cmsEnvPropertiesLoader;
    private final boolean s3ConfigEnabled;

    private ConfigService() {
        Pair<String, String> appIdAndEnvironmentPair = MainClassUtils.getAppIdAndEnvironmentFromSystemProperties();
        Config appConfig = TypesafeConfigUtil
                .loadConfigForAppIdAndEnvironment(appIdAndEnvironmentPair.getLeft(), appIdAndEnvironmentPair.getRight());

        Properties cliGeneratedProperties;
        if (s3ConfigEnabled =
                appConfig.hasPath(CMS_DISABLE_ENV_LOAD_FLAG) && appConfig.getBoolean(CMS_DISABLE_ENV_LOAD_FLAG)) {
            cliGeneratedProperties = new Properties();
        } else {
            cmsEnvPropertiesLoader = new CmsEnvPropertiesLoader(
                    System.getenv(BUCKET_NAME_KEY),
                    System.getenv(REGION_KEY),
                    new AwsCrypto());
            cliGeneratedProperties = cmsEnvPropertiesLoader.getProperties();
        }
        mergedConfig = ConfigFactory.parseProperties(cliGeneratedProperties).withFallback(appConfig);
    }

    public static ConfigService getInstance() {
        if (instance == null) {
            instance = new ConfigService();
        }
        return instance;
    }

    /**
     * Returns whether or not config can be loaded from S3
     */
    public boolean isS3ConfigDisabled() {
        return s3ConfigEnabled;
    }

    /**
     * @return Typesafe app config merged into CLI generated properties
     */
    public Config getAppConfigMergedWithCliGeneratedProperties() {
        return mergedConfig;
    }

    /**
     * Get the value of the Certificate from S3
     * @param certificateName
     */
    public String getCertificate(String certificateName) {
        return cmsEnvPropertiesLoader.getCertificate(certificateName);
    }

    /**
     * Get the value of the PKCS8 Private Key from S3.
     *
     * The SslContextBuilder and Netty´s SslContext implementations only support PKCS8 keys.
     *
     * http://netty.io/wiki/sslcontextbuilder-and-private-key.html
     * @param certificateName
     */
    public String getPrivateKey(String certificateName) {
        return cmsEnvPropertiesLoader.getPrivateKey(certificateName);
    }

    /**
     * @return Whether or not the appropriate settings have been set to the enable audit logging appender and audit logging event processor.
     */
    public boolean isAuditLoggingEnabled() {
        return mergedConfig.hasPath(AUDIT_LOGGING_ENABLED_KEY) && mergedConfig.getBoolean(AUDIT_LOGGING_ENABLED_KEY);
    }

    /**
     * @return Whether or not the appropriate setting have been set to upload the audit log files as they get rolled by the appender
     */
    public boolean isS3AuditLogCopyingEnabled() {
        String bucket = mergedConfig.hasPath(AUDIT_BUCKET_CONFIG_KEY) ? mergedConfig.getString(AUDIT_BUCKET_CONFIG_KEY) : "";
        String bucketRegion = mergedConfig.hasPath(AUDIT_BUCKET_REGION_CONFIG_KEY) ? mergedConfig.getString(AUDIT_BUCKET_REGION_CONFIG_KEY) : "";
        return StringUtils.isNotBlank(bucket) && StringUtils.isNotBlank(bucketRegion);
    }

    /**
     * Looks through the merged config to get the set of class names of the event processors to create
     */
    public Set<String> getEnabledEventProcessors() {
        if (mergedConfig.hasPath("cms.event.processors")) {
            return mergedConfig.getConfig("cms.event.processors").entrySet().stream()
                    .map(e -> org.apache.commons.lang.StringUtils.removeStart(e.getKey(), "\"cms.event.processors\""))
                    .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }
}
