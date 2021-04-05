package ch.qos.logback.core.rolling;

import com.nike.cerberus.audit.logger.service.S3LogUploaderService;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class AuditLogsS3TimeBasedRollingPolicyTest {

  private AuditLogsS3TimeBasedRollingPolicy auditLogsS3TimeBasedRollingPolicy;

  @Test
  public void testLogUploaderServiceIfLogChunkFileS3QueueIsEmpty() {
    auditLogsS3TimeBasedRollingPolicy =
        new AuditLogsS3TimeBasedRollingPolicy("bucket", "bucketRegion");
    S3LogUploaderService s3LogUploaderService = Mockito.mock(S3LogUploaderService.class);
    auditLogsS3TimeBasedRollingPolicy.setS3LogUploaderService(s3LogUploaderService);
    Mockito.verify(s3LogUploaderService, Mockito.never()).ingestLog(Mockito.anyString());
  }

  @Test
  public void testRollOverIfAuditCopyIsNotEnabled() {
    auditLogsS3TimeBasedRollingPolicy = Mockito.spy(new AuditLogsS3TimeBasedRollingPolicy("", ""));
    Mockito.doNothing().when(auditLogsS3TimeBasedRollingPolicy).superRollOver();
    S3LogUploaderService s3LogUploaderService = Mockito.mock(S3LogUploaderService.class);
    auditLogsS3TimeBasedRollingPolicy.setS3LogUploaderService(s3LogUploaderService);
    TimeBasedFileNamingAndTriggeringPolicy timeBasedFileNamingAndTriggeringPolicy =
        Mockito.spy(TimeBasedFileNamingAndTriggeringPolicy.class);
    auditLogsS3TimeBasedRollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(
        timeBasedFileNamingAndTriggeringPolicy);
    auditLogsS3TimeBasedRollingPolicy.rollover();
    Mockito.verify(timeBasedFileNamingAndTriggeringPolicy, Mockito.never())
        .getElapsedPeriodsFileName();
    Mockito.verify(s3LogUploaderService, Mockito.never()).ingestLog(Mockito.anyString());
  }

  @Test
  public void testRollOverIfAuditCopyIsEnabled() {
    auditLogsS3TimeBasedRollingPolicy =
        Mockito.spy(new AuditLogsS3TimeBasedRollingPolicy("bucket", "region"));
    Mockito.doNothing().when(auditLogsS3TimeBasedRollingPolicy).superRollOver();
    S3LogUploaderService s3LogUploaderService = Mockito.mock(S3LogUploaderService.class);
    auditLogsS3TimeBasedRollingPolicy.setS3LogUploaderService(s3LogUploaderService);
    TimeBasedFileNamingAndTriggeringPolicy timeBasedFileNamingAndTriggeringPolicy =
        Mockito.spy(TimeBasedFileNamingAndTriggeringPolicy.class);
    Mockito.when(timeBasedFileNamingAndTriggeringPolicy.getElapsedPeriodsFileName())
        .thenReturn("elapsedfilename");
    auditLogsS3TimeBasedRollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(
        timeBasedFileNamingAndTriggeringPolicy);
    LinkedBlockingQueue<String> logChunkFileS3Queue = new LinkedBlockingQueue<>();
    auditLogsS3TimeBasedRollingPolicy.setLogChunkFileS3Queue(logChunkFileS3Queue);
    auditLogsS3TimeBasedRollingPolicy.rollover();
    Mockito.verify(timeBasedFileNamingAndTriggeringPolicy).getElapsedPeriodsFileName();
    Mockito.verify(s3LogUploaderService).ingestLog("elapsedfilename.gz");
    Assert.assertTrue(logChunkFileS3Queue.size() == 0);
  }

  @Test
  public void testRollOverIfAuditCopyIsEnabledAndS3UploaderIsNull() {
    auditLogsS3TimeBasedRollingPolicy =
        Mockito.spy(new AuditLogsS3TimeBasedRollingPolicy("bucket", "region"));
    Mockito.doNothing().when(auditLogsS3TimeBasedRollingPolicy).superRollOver();
    TimeBasedFileNamingAndTriggeringPolicy timeBasedFileNamingAndTriggeringPolicy =
        Mockito.spy(TimeBasedFileNamingAndTriggeringPolicy.class);
    Mockito.when(timeBasedFileNamingAndTriggeringPolicy.getElapsedPeriodsFileName())
        .thenReturn("elapsedfilename");
    auditLogsS3TimeBasedRollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(
        timeBasedFileNamingAndTriggeringPolicy);
    LinkedBlockingQueue<String> logChunkFileS3Queue = new LinkedBlockingQueue<>();
    auditLogsS3TimeBasedRollingPolicy.setLogChunkFileS3Queue(logChunkFileS3Queue);
    auditLogsS3TimeBasedRollingPolicy.rollover();
    Mockito.verify(timeBasedFileNamingAndTriggeringPolicy).getElapsedPeriodsFileName();
    Assert.assertTrue(logChunkFileS3Queue.size() > 0);
  }
}
