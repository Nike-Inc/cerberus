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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.AuditLogsS3TimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.FiveMinuteRollingFileAppender;
import ch.qos.logback.core.util.FileSize;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty("cerberus.audit.athena.enabled")
@ComponentScan({"com.nike.cerberus.audit.logger"})
@Import({AuditLogsS3TimeBasedRollingPolicy.class})
public class AthenaAuditLoggerConfiguration {

  private static final String ATHENA_AUDIT_LOGGER_NAME = "athena-audit-logger";
  private static final String ATHENA_LOG_APPENDER_NAME = "athena-log-appender";
  private static final String MESSAGE_PATTERN = "%msg%n";

  private final Logger athenaAuditLogger;
  private final AuditLogsS3TimeBasedRollingPolicy<ILoggingEvent> auditLogsS3TimeBasedRollingPolicy;

  @Autowired
  public AthenaAuditLoggerConfiguration(
      AuditLogsS3TimeBasedRollingPolicy<ILoggingEvent> auditLogsS3TimeBasedRollingPolicy) {

    this.auditLogsS3TimeBasedRollingPolicy = auditLogsS3TimeBasedRollingPolicy;

    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
    patternLayoutEncoder.setPattern(MESSAGE_PATTERN);
    patternLayoutEncoder.setContext(loggerContext);
    patternLayoutEncoder.start();

    String hostname;
    try {
      hostname =
          System.getenv("HOSTNAME") != null
              ? System.getenv("HOSTNAME")
              : InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new RuntimeException("Unable to find host name");
    }

    FiveMinuteRollingFileAppender<ILoggingEvent> fiveMinuteRollingFileAppender =
        new FiveMinuteRollingFileAppender<>();
    fiveMinuteRollingFileAppender.setName(ATHENA_LOG_APPENDER_NAME);
    fiveMinuteRollingFileAppender.setContext(loggerContext);
    fiveMinuteRollingFileAppender.setFile(hostname + "-audit.log");
    fiveMinuteRollingFileAppender.setEncoder(patternLayoutEncoder);

    this.auditLogsS3TimeBasedRollingPolicy.setContext(loggerContext);
    this.auditLogsS3TimeBasedRollingPolicy.setFileNamePattern(
        hostname + "-audit.%d{yyyy-MM-dd-HH-mm, UTC}.log.gz");
    this.auditLogsS3TimeBasedRollingPolicy.setMaxHistory(100);
    this.auditLogsS3TimeBasedRollingPolicy.setParent(fiveMinuteRollingFileAppender);
    this.auditLogsS3TimeBasedRollingPolicy.setTotalSizeCap(FileSize.valueOf("10gb"));

    fiveMinuteRollingFileAppender.setTriggeringPolicy(this.auditLogsS3TimeBasedRollingPolicy);
    fiveMinuteRollingFileAppender.setRollingPolicy(this.auditLogsS3TimeBasedRollingPolicy);

    this.auditLogsS3TimeBasedRollingPolicy.start();
    fiveMinuteRollingFileAppender.start();

    var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ATHENA_AUDIT_LOGGER_NAME);
    logger.addAppender(fiveMinuteRollingFileAppender);
    logger.setLevel(Level.INFO);
    logger.setAdditive(false);
    athenaAuditLogger = logger;
  }

  @Bean
  public ch.qos.logback.classic.Logger getAthenaAuditLogger() {
    return (ch.qos.logback.classic.Logger) athenaAuditLogger;
  }
}
