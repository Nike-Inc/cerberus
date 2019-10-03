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

import com.amazonaws.encryptionsdk.CryptoMaterialsManager;
import com.amazonaws.encryptionsdk.DefaultCryptoMaterialsManager;
import com.amazonaws.encryptionsdk.MasterKeyProvider;
import com.amazonaws.encryptionsdk.caching.CachingCryptoMaterialsManager;
import com.amazonaws.encryptionsdk.caching.CryptoMaterialsCache;
import com.amazonaws.encryptionsdk.caching.LocalCryptoMaterialsCache;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.inject.*;
import com.google.inject.name.Names;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.cerberus.auth.connector.AuthConnector;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.cache.MetricReportingCache;
import com.nike.cerberus.cache.MetricReportingCryptoMaterialsCache;
import com.nike.cerberus.domain.IamPrincipalCredentials;
import com.nike.cerberus.domain.IamRoleAuthResponse;
import com.nike.cerberus.endpoints.*;
import com.nike.cerberus.endpoints.authentication.*;
import com.nike.cerberus.endpoints.authentication.CodeHandlingMfaCheck;
import com.nike.cerberus.error.DefaultApiErrorsImpl;
import com.nike.cerberus.event.processor.EventProcessor;
import com.nike.cerberus.hystrix.HystrixKmsClientFactory;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.service.*;
import com.nike.cerberus.util.UuidSupplier;
import com.nike.riposte.client.asynchttp.ning.AsyncHttpClientHelper;
import com.nike.riposte.server.config.AppInfo;
import com.nike.riposte.server.hooks.ServerShutdownHook;
import com.nike.riposte.server.http.Endpoint;
import com.nike.riposte.util.AwsUtil;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.client.AuthenticationClients;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLException;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.nike.cerberus.service.EncryptionService.*;
import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;

public class CmsGuiceModule extends AbstractModule {

    private static final String AUTH_CONNECTOR_IMPL_KEY = "cms.auth.connector";

    private static final String DASHBOARD_DIRECTORY_RELATIVE_PATH = "/dashboard/";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ConfigService configService = ConfigService.getInstance();

    private final ObjectMapper objectMapper;

    private boolean addS3LoggerToShutdownHooks = false;

    public CmsGuiceModule(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void configure() {
        bind(ObjectMapper.class).toInstance(objectMapper);
        bind(ConfigService.class).toInstance(configService);

        String className = configService.getAppConfigMergedWithCliGeneratedProperties()
                .getString(AUTH_CONNECTOR_IMPL_KEY);
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

        configureAuditLogging();
    }

    private void configureAuditLogging() {
        boolean isAuditLoggingEnabled = configService.isAuditLoggingEnabled();
        boolean isS3AuditLogCopyingEnabled = configService.isS3AuditLogCopyingEnabled();
        logger.info("Configuring Audit Logging. isAuditLoggingEnabled: {}, isS3AuditLogCopyingEnabled: {}",
                isAuditLoggingEnabled, isS3AuditLogCopyingEnabled);
        if (isAuditLoggingEnabled && isS3AuditLogCopyingEnabled) {
            bind(S3LogUploaderService.class).asEagerSingleton();
            addS3LoggerToShutdownHooks = true;
        }
    }

    @Provides
    @Singleton
    @Named("shutdownHooks")
    public List<ServerShutdownHook> shutdownHooks(Injector injector) {
        List<ServerShutdownHook> shutdownHooks = new LinkedList<>();
        if (addS3LoggerToShutdownHooks) {
            shutdownHooks.add(injector.getInstance(S3LogUploaderService.class));
        }
        shutdownHooks.add(injector.getInstance(DistributedLockService.class));

        return shutdownHooks;
    }

    @Provides
    @Singleton
    @Named("appEndpoints")
    public Set<Endpoint<?>> appEndpoints(Injector injector) {
        Reflections packageReflections = new Reflections("com.nike.cerberus");
        Set<Class<?>> riposteEndpoints = packageReflections.getTypesAnnotatedWith(RiposteEndpoint.class);
        Set<Endpoint<?>> endpoints = new HashSet<>();
        riposteEndpoints.forEach(c -> endpoints.add((Endpoint) injector.getInstance(c)));
        return endpoints;
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
    public AuthenticationClient authenticationClient(@Named("auth.connector.okta.base_url") String oktaUrl,
                                                     @Named("auth.connector.okta.api_key") String oktaApiKey) {

        System.setProperty("okta.client.token", oktaApiKey);
        return AuthenticationClients.builder()
                .setOrgUrl(oktaUrl)
                .build();
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
                || i instanceof RobotsEndpoint
                || i instanceof AuthenticateUser
                || i instanceof CodeHandlingMfaCheck
                || i instanceof AuthenticateIamRole
                || i instanceof AuthenticateIamPrincipal
                || i instanceof AuthenticateStsIdentity
                || i instanceof GetDashboardRedirect
                || i instanceof GetDashboard)).collect(Collectors.toList());
    }

    /**
     * Process the list of fully qualified class names under cms.event.enabledProcessors.
     * Using just to get an instance of the class and create a list of processors for the event processing service.
     *
     * @param injector The guice injector
     * @return List of enabled processors
     */
    @Provides
    @Singleton
    @Named("eventProcessors")
    public List<EventProcessor> eventProcessors(Injector injector) {
        List<EventProcessor> eventProcessors = new LinkedList<>();
        configService.getEnabledEventProcessors().forEach(processorClassname -> {
            try {
                EventProcessor processor = (EventProcessor)
                        injector.getInstance(Class.forName(processorClassname));
                eventProcessors.add(processor);
            } catch (ClassNotFoundException e) {
                logger.error("Failed to get instance of Event Processor: {}", e);
            }
        });
        return eventProcessors;
    }

    @Provides
    @Singleton
    public EventProcessorService eventProcessorService(@Named("eventProcessors") List<EventProcessor> eventProcessors) {

        EventProcessorService eventProcessorService = new EventProcessorService();
        eventProcessors.forEach(eventProcessorService::registerProcessor);

        return eventProcessorService;
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

    /**
     * The SslContextBuilder and Netty´s SslContext implementations only support PKCS8 keys.
     *
     * http://netty.io/wiki/sslcontextbuilder-and-private-key.html
     */
    @Provides
    @Singleton
    public SslContext sslContext(@Named("cms.ssl.protocolsEnabled") String protocolsEnabled, Injector injector) throws SSLException, CertificateException {
        Validate.notBlank(protocolsEnabled, "cms.ssl.protocolsEnabled requires a list of SSL protocols, e.g. TLSv1.2");
        logger.info("ssl protocols enabled: " + protocolsEnabled);
        if (configService.isS3ConfigDisabled()) {
            logger.info("initializing SslContext by creating a self-signed certificate");
            SelfSignedCertificate ssc = new SelfSignedCertificate("localhost");
            return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                    .protocols(StringUtils.split(protocolsEnabled, ","))
                    .build();
        } else {
            logger.info("initializing SslContext using certificate from S3");
            String certificateName = injector.getInstance(Key.get(String.class, Names.named("cms.ssl.certificateName")));
            logger.info("Perparing to download and use certificate with identity management name: {}", certificateName);
            InputStream certificate = IOUtils.toInputStream(configService.getCertificate(certificateName), Charset.defaultCharset());
            InputStream privateKey = IOUtils.toInputStream(configService.getPrivateKey(certificateName), Charset.defaultCharset());
            return SslContextBuilder.forServer(certificate, privateKey)
                    .protocols(StringUtils.split(protocolsEnabled, ","))
                    .build();
        }

    }

    @Provides
    @Singleton
    @Named("dashboardAssetManager")
    public StaticAssetManager dashboardStaticAssetManager() {
        int maxDepthOfFileTraversal = 2;
        return new StaticAssetManager(DASHBOARD_DIRECTORY_RELATIVE_PATH, maxDepthOfFileTraversal);
    }

    @Provides
    @Singleton
    @Named("encryptCryptoMaterialsManager")
    public CryptoMaterialsManager encryptCryptoMaterialsManager(@Named("cms.encryption.cmk.arns") String cmkArns,
                                                                @Named("cms.encryption.cache.enabled") boolean cacheEnabled,
                                                                KmsDataKeyCachingOptionalPropertyHolder kmsDataKeyCachingOptionalPropertyHolder,
                                                                Region currentRegion,
                                                                MetricsService metricsService) {
        MasterKeyProvider<KmsMasterKey> keyProvider = initializeKeyProvider(cmkArns, currentRegion);
        if (cacheEnabled) {
            int maxSize = kmsDataKeyCachingOptionalPropertyHolder.encryptMaxSize;
            int maxAge = kmsDataKeyCachingOptionalPropertyHolder.encryptMaxAge;
            int messageUseLimit = kmsDataKeyCachingOptionalPropertyHolder.encryptMessageUseLimit;
            logger.info("Initializing caching encryptCryptoMaterialsManager with CMK: {}, maxSize: {}, maxAge: {}, " +
                    "messageUseLimit: {}", cmkArns, maxSize, maxAge, messageUseLimit);
            CryptoMaterialsCache cache = new MetricReportingCryptoMaterialsCache(maxSize, metricsService);
            CryptoMaterialsManager cachingCmm =
                    CachingCryptoMaterialsManager.newBuilder().withMasterKeyProvider(keyProvider)
                            .withCache(cache)
                            .withMaxAge(maxAge, TimeUnit.SECONDS)
                            .withMessageUseLimit(messageUseLimit)
                            .build();
            return cachingCmm;
        } else {
            logger.info("Initializing encryptCryptoMaterialsManager with CMK: {}", cmkArns);
            return new DefaultCryptoMaterialsManager(keyProvider);
        }
    }

    @Provides
    @Singleton
    @Named("decryptCryptoMaterialsManager")
    public CryptoMaterialsManager decryptCryptoMaterialsManager(@Named("cms.encryption.cmk.arns") String cmkArns,
                                                                @Named("cms.encryption.cache.enabled") boolean cacheEnabled,
                                                                KmsDataKeyCachingOptionalPropertyHolder kmsDataKeyCachingOptionalPropertyHolder,
                                                                Region currentRegion,
                                                                MetricsService metricsService) {
        MasterKeyProvider<KmsMasterKey> keyProvider = initializeKeyProvider(cmkArns, currentRegion);
        if (cacheEnabled) {
            int maxSize = kmsDataKeyCachingOptionalPropertyHolder.decryptMaxSize;
            int maxAge = kmsDataKeyCachingOptionalPropertyHolder.decryptMaxAge;
            logger.info("Initializing caching decryptCryptoMaterialsManager with CMK: {}, maxSize: {}, maxAge: {}",
                    cmkArns, maxSize, maxAge);
            CryptoMaterialsCache cache = new MetricReportingCryptoMaterialsCache(maxSize, metricsService);
            CryptoMaterialsManager cachingCmm =
                    CachingCryptoMaterialsManager.newBuilder().withMasterKeyProvider(keyProvider)
                            .withCache(cache)
                            .withMaxAge(maxAge, TimeUnit.SECONDS)
                            .build();
            return cachingCmm;
        } else {
            logger.info("Initializing decryptCryptoMaterialsManager with CMK: {}", cmkArns);
            return new DefaultCryptoMaterialsManager(keyProvider);
        }
    }

    /**
     * Returns the current region
     * @return current region
     */
    @Provides
    @Singleton
    public Region currentRegion() {
        Region region = Regions.getCurrentRegion();
        Region currentRegion = region == null ? Region.getRegion(Regions.DEFAULT_REGION ) : region;
        return currentRegion;
    }

    @Provides
    @Singleton
    @Named("kmsAuthCache")
    public Cache<IamPrincipalCredentials, IamRoleAuthResponse> kmsAuthCache(MetricsService metricsService,
                                                                            KmsAuthCachingOptionalPropertyHolder kmsAuthCachingOptionalPropertyHolder) {
        return new MetricReportingCache("auth.kms",
                kmsAuthCachingOptionalPropertyHolder.maxAge,
                metricsService, null);
    }

    /**
     * This 'holder' class allows optional injection of KMS-data-key-caching-specific properties that are only necessary when
     * KMS data key caching is enabled.
     *
     * The 'optional=true' parameter to Guice @Inject cannot be used in combination with the @Provides annotation
     * or with constructor injection.
     *
     * https://github.com/google/guice/wiki/FrequentlyAskedQuestions
     */
    static class KmsDataKeyCachingOptionalPropertyHolder {
        @Inject(optional=true)
        @com.google.inject.name.Named("cms.encryption.cache.encrypt.maxSize")
        int encryptMaxSize = 0;

        @Inject(optional=true)
        @com.google.inject.name.Named("cms.encryption.cache.encrypt.maxAgeInSeconds")
        int encryptMaxAge = 0;

        @Inject(optional=true)
        @com.google.inject.name.Named("cms.encryption.cache.encrypt.messageUseLimit")
        int encryptMessageUseLimit = 0;

        @Inject(optional=true)
        @com.google.inject.name.Named("cms.encryption.cache.decrypt.maxSize")
        int decryptMaxSize = 0;

        @Inject(optional=true)
        @com.google.inject.name.Named("cms.encryption.cache.decrypt.maxAgeInSeconds")
        int decryptMaxAge = 0;
    }

    /**
     * This 'holder' class allows optional injection of KMS-auth-caching-specific properties that are only necessary when
     * KMS-auth caching is enabled.
     *
     * The 'optional=true' parameter to Guice @Inject cannot be used in combination with the @Provides annotation
     * or with constructor injection.
     *
     * https://github.com/google/guice/wiki/FrequentlyAskedQuestions
     */
    static class KmsAuthCachingOptionalPropertyHolder {
        @Inject(optional=true)
        @com.google.inject.name.Named("cms.iam.token.cache.maxAgeInSeconds")
        int maxAge = 10;
    }
}
