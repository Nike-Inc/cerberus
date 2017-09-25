/*
 * Copyright (c) 2017 Nike, Inc.
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
 *
 */

package com.nike.cerberus.server.config.guice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.cerberus.auth.connector.AuthConnector;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.config.CmsEnvPropertiesLoader;
import com.nike.cerberus.domain.DashboardResourceFile;
import com.nike.cerberus.endpoints.GetDashboard;
import com.nike.cerberus.endpoints.GetDashboardRedirect;
import com.nike.cerberus.endpoints.HealthCheckEndpoint;
import com.nike.cerberus.endpoints.admin.CleanUpInactiveOrOrphanedRecords;
import com.nike.cerberus.endpoints.admin.GetSDBMetadata;
import com.nike.cerberus.endpoints.admin.PutSDBMetadata;
import com.nike.cerberus.endpoints.authentication.AuthenticateIamPrincipal;
import com.nike.cerberus.endpoints.authentication.AuthenticateIamRole;
import com.nike.cerberus.endpoints.authentication.AuthenticateUser;
import com.nike.cerberus.endpoints.authentication.MfaCheck;
import com.nike.cerberus.endpoints.authentication.RefreshUserToken;
import com.nike.cerberus.endpoints.authentication.RevokeToken;
import com.nike.cerberus.endpoints.category.CreateCategory;
import com.nike.cerberus.endpoints.category.DeleteCategory;
import com.nike.cerberus.endpoints.category.GetAllCategories;
import com.nike.cerberus.endpoints.category.GetCategory;
import com.nike.cerberus.endpoints.role.GetAllRoles;
import com.nike.cerberus.endpoints.role.GetRole;
import com.nike.cerberus.endpoints.sdb.CreateSafeDepositBoxV1;
import com.nike.cerberus.endpoints.sdb.CreateSafeDepositBoxV2;
import com.nike.cerberus.endpoints.sdb.DeleteSafeDepositBox;
import com.nike.cerberus.endpoints.sdb.GetSafeDepositBoxV1;
import com.nike.cerberus.endpoints.sdb.GetSafeDepositBoxV2;
import com.nike.cerberus.endpoints.sdb.GetSafeDepositBoxes;
import com.nike.cerberus.endpoints.sdb.UpdateSafeDepositBoxV1;
import com.nike.cerberus.endpoints.sdb.UpdateSafeDepositBoxV2;
import com.nike.cerberus.endpoints.secret.WriteSecureData;
import com.nike.cerberus.error.DefaultApiErrorsImpl;
import com.nike.cerberus.hystrix.HystrixKmsClientFactory;
import com.nike.cerberus.hystrix.HystrixMetricsLogger;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.service.AuthTokenService;
import com.nike.cerberus.util.ArchaiusUtils;
import com.nike.cerberus.util.DashboardResourceFileFactory;
import com.nike.cerberus.util.UuidSupplier;
import com.nike.riposte.client.asynchttp.ning.AsyncHttpClientHelper;
import com.nike.riposte.server.config.AppInfo;
import com.nike.riposte.server.http.Endpoint;
import com.nike.riposte.util.AwsUtil;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class CmsGuiceModule extends AbstractModule {

    private static final String KMS_KEY_ID_KEY = "CONFIG_KEY_ID";

    private static final String REGION_KEY = "EC2_REGION";

    private static final String BUCKET_NAME_KEY = "CONFIG_S3_BUCKET";

    private static final String CMS_DISABLE_ENV_LOAD_FLAG = "cms.env.load.disable";

    private static final String AUTH_CONNECTOR_IMPL_KEY = "cms.auth.connector";

    private static final String DASHBOARD_DIRECTORY_RELATIVE_PATH = "dashboard/";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Config appConfig;
    private final ObjectMapper objectMapper;

    public CmsGuiceModule(Config appConfig, ObjectMapper objectMapper) {
        if (appConfig == null)
            throw new IllegalArgumentException("appConfig cannot be null");

        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void configure() {
        loadEnvProperties();

        bind(ObjectMapper.class).toInstance(objectMapper);

        String className = this.appConfig.getString(AUTH_CONNECTOR_IMPL_KEY);
        try
        {
            Class<?> clazz = Class.forName(className);
            bind(AuthConnector.class)
                    .to(clazz.asSubclass(AuthConnector.class))
                    .asEagerSingleton();
        } catch(ClassNotFoundException nfe) {
            throw new IllegalArgumentException("invalid class: " + className, nfe);
        } catch(ClassCastException cce) {
            throw new IllegalArgumentException("class: " + className + " is the wrong type", cce);
        }

        bind(HystrixMetricsLogger.class).asEagerSingleton();
    }

    private void loadEnvProperties() {
        if (appConfig.hasPath(CMS_DISABLE_ENV_LOAD_FLAG) && appConfig.getBoolean(CMS_DISABLE_ENV_LOAD_FLAG)) {
            logger.warn("CMS environment property loading disabled.");
        } else {
            final CmsEnvPropertiesLoader cmsEnvPropertiesLoader = new CmsEnvPropertiesLoader(
                    System.getenv(BUCKET_NAME_KEY),
                    System.getenv(REGION_KEY),
                    System.getenv(KMS_KEY_ID_KEY)
            );
            Properties properties = cmsEnvPropertiesLoader.getProperties();

            // bind the props to named props for guice
            Names.bindProperties(binder(), properties);

            // properties from cms.conf may be overridden in environment.properties
            ArchaiusUtils.loadProperties(properties);

            for (String propertyName : properties.stringPropertyNames()) {
                logger.info("Successfully loaded: {} from the env data stored in S3", propertyName);
                appConfig = appConfig.withValue(propertyName, ConfigValueFactory.fromAnyRef(properties.getProperty(propertyName)));
            }
        }
    }

    @Provides
    @Singleton
    @Named("appEndpoints")
    public Set<Endpoint<?>> appEndpoints(
            HealthCheckEndpoint healthCheckEndpoint,
            // Cerberus endpoints
            GetAllCategories getAllCategories,
            GetCategory getCategory,
            CreateCategory createCategory,
            DeleteCategory deleteCategory,
            AuthenticateUser authenticateUser,
            MfaCheck mfaCheck,
            RefreshUserToken refreshUserToken,
            AuthenticateIamRole authenticateIamRole,
            AuthenticateIamPrincipal authenticateIamPrincipal,
            RevokeToken revokeToken,
            GetAllRoles getAllRoles,
            GetRole getRole,
            GetSafeDepositBoxes getSafeDepositBoxes,
            GetSafeDepositBoxV1 getSafeDepositBoxV1,
            GetSafeDepositBoxV2 getSafeDepositBoxV2,
            DeleteSafeDepositBox deleteSafeDepositBox,
            UpdateSafeDepositBoxV1 updateSafeDepositBoxV1,
            UpdateSafeDepositBoxV2 updateSafeDepositBoxV2,
            CreateSafeDepositBoxV1 createSafeDepositBoxV1,
            CreateSafeDepositBoxV2 createSafeDepositBoxV2,
            GetSDBMetadata getSDBMetadata,
            PutSDBMetadata putSDBMetadata,
            CleanUpInactiveOrOrphanedRecords cleanUpInactiveOrOrphanedRecords,
            GetDashboardRedirect getDashboardRedirect,
            GetDashboard getDashboard,
            WriteSecureData writeSecureData
    ) {
        return new LinkedHashSet<>(Arrays.<Endpoint<?>>asList(
                healthCheckEndpoint,
                // Cerberus endpoints
                getAllCategories, getCategory, createCategory, deleteCategory,
                authenticateUser, authenticateIamPrincipal, mfaCheck, refreshUserToken, authenticateIamRole, revokeToken,
                getAllRoles, getRole,
                getSafeDepositBoxes, getSafeDepositBoxV1, getSafeDepositBoxV2,
                deleteSafeDepositBox, updateSafeDepositBoxV1, updateSafeDepositBoxV2, createSafeDepositBoxV1, createSafeDepositBoxV2,
                getSDBMetadata, putSDBMetadata, cleanUpInactiveOrOrphanedRecords,
                getDashboardRedirect, getDashboard, writeSecureData
        ));
    }

    @Provides
    @Singleton
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Provides
    @Singleton
    public ProjectApiErrors projectApiErrors() {
        return new DefaultApiErrorsImpl();
    }

    @Provides
    @Singleton
    public AsyncHttpClientHelper asyncHttpClientHelper() {
        return new AsyncHttpClientHelper();
    }

    @Singleton
    @Provides
    public UuidSupplier uuidSupplier() {
        return new UuidSupplier();
    }

    @Provides
    @Singleton
    @Named("authProtectedEndpoints")
    public List<Endpoint<?>> authProtectedEndpoints(@Named("appEndpoints") Set<Endpoint<?>> endpoints) {
        return endpoints.stream().filter(i -> !(i instanceof HealthCheckEndpoint
                || i instanceof AuthenticateUser
                || i instanceof MfaCheck
                || i instanceof AuthenticateIamRole
                || i instanceof AuthenticateIamPrincipal
                || i instanceof GetDashboardRedirect
                || i instanceof GetDashboard)).collect(Collectors.toList());
    }

    @Provides
    @Singleton
    public CmsRequestSecurityValidator authRequestSecurityValidator(
            @Named("authProtectedEndpoints") List<Endpoint<?>> authProtectedEndpoints,
            AuthTokenService authTokenService) {

        return new CmsRequestSecurityValidator(authProtectedEndpoints, authTokenService);
    }

    @Provides
    @Singleton
    @Named("appInfoFuture")
    public CompletableFuture<AppInfo> appInfoFuture(AsyncHttpClientHelper asyncHttpClientHelper) {
        return AwsUtil.getAppInfoFutureWithAwsInfo(asyncHttpClientHelper);
    }

    @Provides
    @Singleton
    @Named("hystrixExecutor")
    public ScheduledExecutorService executor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Provides
    @Singleton
    public KmsClientFactory hystrixKmsClientFactory() {
        return new HystrixKmsClientFactory(new KmsClientFactory());
    }

    @Provides
    @Singleton
    @Named("dashboardAssetMap")
    public Map<String, DashboardResourceFile> dashboardAssetMap() {
        URL dashboardFilePath = getClass().getClassLoader().getResource(DASHBOARD_DIRECTORY_RELATIVE_PATH);
        if (dashboardFilePath == null) {
            throw new IllegalStateException("Failed to load dashboard resources, relative path: " + DASHBOARD_DIRECTORY_RELATIVE_PATH);
        }

        File dashboardDir = new File(dashboardFilePath.getPath());
        Map<String, DashboardResourceFile> dashboardAssets = Maps.newHashMap();
        Files.fileTreeTraverser()
                .breadthFirstTraversal(dashboardDir)
                .filter(File::isFile)
                .forEach(f -> {
                    DashboardResourceFile resource = DashboardResourceFileFactory.create(f, dashboardDir.getAbsolutePath());
                    dashboardAssets.put(resource.getRelativePath(), resource);
                });

        return ImmutableMap.<String, DashboardResourceFile>builder()
                .putAll(dashboardAssets)
                .build();
    }
}
