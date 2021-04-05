package ch.qos.logback.core.rolling;

import com.nike.cerberus.audit.logger.service.S3LogUploaderService;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AuditLogsS3TimeBasedRollingPolicyTest {

  private AuditLogsS3TimeBasedRollingPolicy auditLogsS3TimeBasedRollingPolicy;

  @Before
  public void setup() {
    auditLogsS3TimeBasedRollingPolicy =
        new AuditLogsS3TimeBasedRollingPolicy("bucket", "bucketRegion");
  }

  @Test
  public void testLogUploaderServiceIfLogChunkFileS3QueueIsEmpty() {
    S3LogUploaderService s3LogUploaderService = Mockito.mock(S3LogUploaderService.class);
    auditLogsS3TimeBasedRollingPolicy.setS3LogUploaderService(s3LogUploaderService);
    Mockito.verify(s3LogUploaderService, Mockito.never()).ingestLog(Mockito.anyString());
  }

  @Test
  public void testLogUploaderServiceIfLogChunkFileS3QueueNotEmpty() {
    S3LogUploaderService s3LogUploaderService = Mockito.mock(S3LogUploaderService.class);
    LinkedBlockingQueue<String> logChunkFileS3Queue = new LinkedBlockingQueue<>();
    logChunkFileS3Queue.add("sampleFilename");
    auditLogsS3TimeBasedRollingPolicy.setLogChunkFileS3Queue(logChunkFileS3Queue);
    auditLogsS3TimeBasedRollingPolicy.setS3LogUploaderService(s3LogUploaderService);
    Mockito.verify(s3LogUploaderService).ingestLog("sampleFilename");
  }
}
