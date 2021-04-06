package com.nike.cerberus.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.AuditLogsS3TimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.FiveMinuteRollingFileAppender;
import ch.qos.logback.core.util.FileSize;
import org.junit.Test;
import org.mockito.Mockito;

public class AthenaAuditLoggerConfigurationTest {

  @Test
  public void testRollingPolicyStarted() {
    AuditLogsS3TimeBasedRollingPolicy<ILoggingEvent> auditLogsS3TimeBasedRollingPolicy =
        Mockito.mock(AuditLogsS3TimeBasedRollingPolicy.class);
    AthenaAuditLoggerConfiguration athenaAuditLoggerConfiguration =
        new AthenaAuditLoggerConfiguration("", auditLogsS3TimeBasedRollingPolicy);
    Mockito.verify(auditLogsS3TimeBasedRollingPolicy).setContext(Mockito.any(LoggerContext.class));
    Mockito.verify(auditLogsS3TimeBasedRollingPolicy).setFileNamePattern(Mockito.anyString());
    Mockito.verify(auditLogsS3TimeBasedRollingPolicy).setMaxHistory(100);
    Mockito.verify(auditLogsS3TimeBasedRollingPolicy)
        .setParent(Mockito.any(FiveMinuteRollingFileAppender.class));
    Mockito.verify(auditLogsS3TimeBasedRollingPolicy).setTotalSizeCap(Mockito.any(FileSize.class));
    Mockito.verify(auditLogsS3TimeBasedRollingPolicy).start();
  }
}
