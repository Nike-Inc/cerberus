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

package com.nike.cerberus.config;

import static com.nike.cerberus.service.EncryptionService.initializeKeyProvider;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoMaterialsManager;
import com.amazonaws.encryptionsdk.DefaultCryptoMaterialsManager;
import com.amazonaws.encryptionsdk.MasterKeyProvider;
import com.amazonaws.encryptionsdk.caching.CachingCryptoMaterialsManager;
import com.amazonaws.encryptionsdk.caching.CryptoMaterialsCache;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.codahale.metrics.Slf4jReporter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.benmanes.caffeine.cache.Cache;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.cerberus.cache.MetricReportingCache;
import com.nike.cerberus.cache.MetricReportingCryptoMaterialsCache;
import com.nike.cerberus.domain.AwsIamKmsAuthRequest;
import com.nike.cerberus.domain.EncryptedAuthDataWrapper;
import com.nike.cerberus.error.DefaultApiErrorsImpl;
import com.nike.cerberus.metric.LoggingMetricsService;
import com.nike.cerberus.metric.MetricsService;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Validation;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.filter.OncePerRequestFilter;

@SuppressWarnings("SpringComponentScan")
@Slf4j
@Configuration
@ComponentScan({
  "com.netflix.spinnaker.kork.secrets",
  "com.nike.backstopper", // error management
  "com.nike.cerberus.error",
  "com.nike.cerberus.auth.connector.config",
  "com.nike.cerberus.aws",
  "com.nike.cerberus.config",
  "com.nike.cerberus.controller",
  "com.nike.cerberus.dao",
  "com.nike.cerberus.event.filter",
  "com.nike.cerberus.external", // Hook for external stuff (plugins)
  "com.nike.cerberus.jobs",
  "com.nike.cerberus.security",
  "com.nike.cerberus.service",
  "com.nike.cerberus.util",
  "com.nike.wingtips.springboot"
})
@EnableAutoConfiguration(
    exclude = {
      FlywayAutoConfiguration
          .class // Have no idea how this magic works, but we will manually configure this ourselves
      // so that it works the way it works in the old guice config
    })
@EnableAsync
@EnableScheduling
public class ApplicationConfiguration {

  // TODO temp hack for unit tests, will need to re-visit this.
  public static ObjectMapper getObjectMapper() {
    ObjectMapper om = new ObjectMapper();
    om.findAndRegisterModules();
    om.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    om.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
    om.enable(SerializationFeature.INDENT_OUTPUT);
    return om;
  }

  @Bean
  public ObjectMapper objectMapper() {
    return getObjectMapper();
  }

  /**
   * TODO un enum-ify this so errors can be created in individuals modules and gathered dynamically
   * here
   */
  @Bean
  public ProjectApiErrors getProjectApiErrors() {
    return new DefaultApiErrorsImpl();
  }

  @Bean
  public Validator getJsr303Validator() { // todo is this already available as a bean
    return Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Bean
  public Cache<AwsIamKmsAuthRequest, EncryptedAuthDataWrapper> kmsAuthCache(
      MetricsService metricsService,
      @Value("${cerberus.auth.iam.kms.cache.maxAgeInSeconds:10}") int maxAge) {

    return new MetricReportingCache<>("auth.kms", maxAge, metricsService, null);
  }

  @Bean
  public Region currentRegion() {
    // TODO, this adds a long wait to app boot when local, spring way to avoid this when env =
    // local?
    return Optional.ofNullable(Regions.getCurrentRegion())
        .orElse(Region.getRegion(Regions.DEFAULT_REGION));
  }

  @Bean("region")
  public String currentRegionAsString() {
    return currentRegion().getName();
  }

  @Bean("encryptCryptoMaterialsManager")
  public CryptoMaterialsManager encryptCryptoMaterialsManager(
      @Value("${cerberus.encryption.cmk.arns}") String cmkArns,
      @Value("${cerberus.encryption.cache.enabled:false}") boolean cacheEnabled,
      @Value("${cerberus.encryption.cache.encrypt.maxSize:100}") int encryptMaxSize,
      @Value("${cerberus.encryption.cache.encrypt.maxAgeInSeconds:60}") int encryptMaxAge,
      @Value("${cerberus.encryption.cache.encrypt.messageUseLimit:100}") int encryptMessageUseLimit,
      Region currentRegion,
      MetricsService metricsService) {
    MasterKeyProvider<KmsMasterKey> keyProvider = initializeKeyProvider(cmkArns, currentRegion);
    if (cacheEnabled) {
      log.info(
          "Initializing caching encryptCryptoMaterialsManager with CMK: {}, maxSize: {}, maxAge: {}, "
              + "messageUseLimit: {}",
          cmkArns,
          encryptMaxSize,
          encryptMaxAge,
          encryptMessageUseLimit);
      CryptoMaterialsCache cache =
          new MetricReportingCryptoMaterialsCache(encryptMaxSize, metricsService);
      CryptoMaterialsManager cachingCmm =
          CachingCryptoMaterialsManager.newBuilder()
              .withMasterKeyProvider(keyProvider)
              .withCache(cache)
              .withMaxAge(encryptMaxAge, TimeUnit.SECONDS)
              .withMessageUseLimit(encryptMessageUseLimit)
              .build();
      return cachingCmm;
    } else {
      log.info("Initializing encryptCryptoMaterialsManager with CMK: {}", cmkArns);
      return new DefaultCryptoMaterialsManager(keyProvider);
    }
  }

  @Bean("decryptCryptoMaterialsManager")
  public CryptoMaterialsManager decryptCryptoMaterialsManager(
      @Value("${cerberus.encryption.cmk.arns}") String cmkArns,
      @Value("${cerberus.encryption.cache.enabled:#{false}}") boolean cacheEnabled,
      @Value("${cerberus.encryption.cache.decrypt.maxSize:1000}") int decryptMaxSize,
      @Value("${cerberus.encryption.cache.decrypt.maxAgeInSeconds:60}") int decryptMaxAge,
      Region currentRegion,
      MetricsService metricsService) {
    MasterKeyProvider<KmsMasterKey> keyProvider = initializeKeyProvider(cmkArns, currentRegion);
    if (cacheEnabled) {
      log.info(
          "Initializing caching decryptCryptoMaterialsManager with CMK: {}, maxSize: {}, maxAge: {}",
          cmkArns,
          decryptMaxSize,
          decryptMaxAge);
      CryptoMaterialsCache cache =
          new MetricReportingCryptoMaterialsCache(decryptMaxAge, metricsService);
      CryptoMaterialsManager cachingCmm =
          CachingCryptoMaterialsManager.newBuilder()
              .withMasterKeyProvider(keyProvider)
              .withCache(cache)
              .withMaxAge(decryptMaxAge, TimeUnit.SECONDS)
              .build();
      return cachingCmm;
    } else {
      log.info("Initializing decryptCryptoMaterialsManager with CMK: {}", cmkArns);
      return new DefaultCryptoMaterialsManager(keyProvider);
    }
  }

  @Bean
  public AwsCrypto awsCrypto() {
    return new AwsCrypto();
  }

  /** TODO, we can probably delete this, but the API tests from Highlander check for this. */
  @Bean
  public OncePerRequestFilter addXRefreshTokenHeaderFilter() {
    return new LambdaFilter(
        (request, response) -> response.addHeader("X-Refresh-Token", Boolean.FALSE.toString()));
  }

  /**
   * This filter maps null responses for PUT and POST requests to 204's rather than 200's This is
   * done in order to maintain backwards compatibility from the pre-spring API.
   */
  @Bean
  public OncePerRequestFilter nullOkResponsesShouldReturnNoContentFilter() {
    return new LambdaFilter(
        true,
        (request, response) -> {
          var typeOptional =
              Optional.ofNullable(response.getContentType()).filter(Predicate.not(String::isBlank));
          if (typeOptional.isEmpty() && response.getStatus() == HttpStatus.OK.value()) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
          }
        });
  }

  /**
   * We need to accept double slashes to maintain backwards compatibility with the Highlander API
   * behavior.
   */
  @Bean
  public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
    StrictHttpFirewall firewall = new StrictHttpFirewall();
    firewall.setAllowUrlEncodedDoubleSlash(true);
    return firewall;
  }

  /**
   * This filter is to duplicate what could be considered buggy behavior, but Highlander Cerberus
   * supports requests with repeating slashes such as `//v2/sts-auth` So we will just trim extra
   * slashes and do the chain with the sanitized uri.
   */
  @Bean
  public OncePerRequestFilter trimExtraSlashesFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {
        var req = request.getRequestURI();
        if (req.contains("//")) {
          var sanitizedUri = StringUtils.replace(req, "//", "/");
          filterChain.doFilter(
              new HttpServletRequestWrapper(request) {
                @Override
                public String getRequestURI() {
                  return sanitizedUri;
                }
              },
              response);
        } else {
          filterChain.doFilter(request, response);
        }
      }
    };
  }

  @Bean
  @ConditionalOnMissingBean(MetricsService.class)
  public MetricsService defaultLoggingMetricsService(
      @Value("${cerberus.metricsService.loggingMetricsService.level:INFO}") String levelString,
      @Value("${cerberus.metricsService.loggingMetricsService.period:1}") String periodString,
      @Value("${cerberus.metricsService.loggingMetricsService.timeUnit:MINUTES}")
          String timeUnitString) {
    var level = Slf4jReporter.LoggingLevel.valueOf(levelString.toUpperCase());
    var period = Long.parseLong(periodString);
    var timeUnit = TimeUnit.valueOf(timeUnitString.toUpperCase());
    return new LoggingMetricsService(level, period, timeUnit);
  }
}
