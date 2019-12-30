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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AthenaAuditLoggerConfiguration {

  private static final String ATHENA_AUDIT_LOGGER_NAME = "athena-audit-logger";
  private static final String ATHENA_LOG_APPENDER_NAME = "athena-log-appender";
  private static final String MESSAGE_PATTERN = "%msg%n";

  private final Logger athenaAuditLogger;

  @Autowired
  public AthenaAuditLoggerConfiguration() {
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

    AuditLogsS3TimeBasedRollingPolicy<ILoggingEvent> auditLogsS3TimeBasedRollingPolicy =
        new AuditLogsS3TimeBasedRollingPolicy<>();
    auditLogsS3TimeBasedRollingPolicy.setContext(loggerContext);
    auditLogsS3TimeBasedRollingPolicy.setFileNamePattern(
        hostname + "-audit.%d{yyyy-MM-dd-HH-mm, UTC}.log.gz");
    auditLogsS3TimeBasedRollingPolicy.setMaxHistory(100);
    auditLogsS3TimeBasedRollingPolicy.setParent(fiveMinuteRollingFileAppender);
    auditLogsS3TimeBasedRollingPolicy.setTotalSizeCap(FileSize.valueOf("10gb"));

    fiveMinuteRollingFileAppender.setTriggeringPolicy(auditLogsS3TimeBasedRollingPolicy);
    fiveMinuteRollingFileAppender.setRollingPolicy(auditLogsS3TimeBasedRollingPolicy);

    auditLogsS3TimeBasedRollingPolicy.start();
    fiveMinuteRollingFileAppender.start();

    var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ATHENA_AUDIT_LOGGER_NAME);
    logger.addAppender(fiveMinuteRollingFileAppender);
    logger.setLevel(Level.INFO);
    logger.setAdditive(false);
    athenaAuditLogger = logger;
  }

  @Bean
  public Logger getAthenaAuditLogger() {
    return athenaAuditLogger;
  }
}
