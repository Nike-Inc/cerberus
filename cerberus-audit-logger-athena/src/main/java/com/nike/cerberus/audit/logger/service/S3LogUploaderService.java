/*
 * Copyright (c) 2019 Nike, Inc.
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

package com.nike.cerberus.audit.logger.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.AuditLogsS3TimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.FiveMinuteRollingFileAppender;
import com.amazonaws.services.s3.AmazonS3;
import com.nike.cerberus.audit.logger.S3ClientFactory;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Service that is intended to be created eagerly by Guice and inject itself into the LogBack
 * rolling policy so that it can ingest filenames that the roller creates when it rolls audit logs
 * and upload them to S3
 *
 * <p>Because the Appenders and other Logback stuff are created before Guice, we get the policy from
 * the LoggerFactory and inject this into it manually using the setter method.
 */
@Slf4j
@Component
public class S3LogUploaderService {

  private static final String ATHENA_LOG_NAME = "athena-audit-logger";
  private static final String ATHENA_LOG_APPENDER = "athena-log-appender";

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final AmazonS3 amazonS3;
  private final String bucket;
  private final String bucketRegion;
  private final boolean athenaLoggingEventListenerEnabled;
  private final AthenaService athenaService;
  private Logger logger;

  @Autowired
  public S3LogUploaderService(
      @Value("${cerberus.audit.athena.bucket}") String bucket,
      @Value("${cerberus.audit.athena.bucketRegion}") String bucketRegion,
      @Value("${cerberus.audit.athena.enabled:false}") boolean athenaLoggingEventListenerEnabled,
      AthenaService athenaService,
      S3ClientFactory s3ClientFactory,
      ch.qos.logback.classic.Logger logger) {
    this.bucket = bucket;
    this.bucketRegion = bucketRegion;
    this.athenaLoggingEventListenerEnabled = athenaLoggingEventListenerEnabled;
    this.athenaService = athenaService;
    this.logger = logger;

    amazonS3 = s3ClientFactory.getClient(bucketRegion);

    // Inject this into the logback rolling policy which was created before guice land exists
    getRollingPolicy()
        .ifPresent(
            policy -> {
              log.info("S3 Rolling Policy detected injecting S3 Log Uploader Service");
              policy.setS3LogUploaderService(this);
            });
  }

  /** Convenience method for sleeping */
  private void sleep(int time, TimeUnit timeUnit) {
    try {
      Thread.sleep(timeUnit.toMillis(time));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Gets the folder structure to use in S3 to enable dt dynamic partitioning */
  protected String getPartition(String fileName) {
    Pattern dtPattern =
        Pattern.compile(".*?(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})_(?<hour>\\d{2}).*");
    Matcher matcher = dtPattern.matcher(fileName);
    if (matcher.find()) {
      String year = matcher.group("year");
      String month = matcher.group("month");
      String day = matcher.group("day");
      String hour = matcher.group("hour");
      athenaService.addPartitionIfMissing(bucketRegion, bucket, year, month, day, hour);
      return String.format("partitioned/year=%s/month=%s/day=%s/hour=%s", year, month, day, hour);
    } else {
      return "un-partitioned";
    }
  }

  /**
   * Asynchronously ingests logfiles and uploads them to S3
   *
   * @param filename The log file that has been rolled and is ready to be uploaded to S3
   */
  public void ingestLog(String filename) {
    executor.execute(() -> processLogFile(filename, 0));
  }

  /**
   * Uploads a file to S3
   *
   * @param filename The file to upload to s3
   * @param retryCount The retry count
   */
  private void processLogFile(String filename, int retryCount) {
    String filteredFilename = FilenameUtils.getName(filename);
    log.info(
        "process log file called with filename: {}, retry count: {}", filteredFilename, retryCount);
    final File rolledLogFile = new File(filteredFilename);
    // poll for 30 seconds waiting for file to exist or bail
    int i = 0;
    do {
      sleep(1, TimeUnit.SECONDS);
      log.info(
          "Does '{}' exist: {}, length: {}, can read: {}, poll count: {}",
          filteredFilename,
          rolledLogFile.exists(),
          rolledLogFile.length(),
          rolledLogFile.canRead(),
          i);
      i++;
    } while (!rolledLogFile.exists() || i >= 30);

    // if file does not exist or empty, do nothing
    if (!rolledLogFile.exists() || rolledLogFile.length() == 0) {
      log.error("File '{}' does not exist or is empty returning", filteredFilename);
      return;
    }

    String partition = getPartition(rolledLogFile.getName());
    String key = String.format("audit-logs/%s/%s", partition, rolledLogFile.getName());

    try {
      log.info("Copying log chunk to s3://{}/{}", bucket, key);
      amazonS3.putObject(bucket, key, rolledLogFile);
      FileUtils.deleteQuietly(rolledLogFile);
      log.info("File: '{}' successfully copied and deleted.", rolledLogFile.getName());
    } catch (Exception e) {
      log.error(
          "Failed to copy log chunk to s3 Bucket: {} key: {} files: {}",
          bucket,
          key,
          rolledLogFile.getName(),
          e);
      if (retryCount < 10) {
        sleep(1, TimeUnit.SECONDS);
        processLogFile(filteredFilename, retryCount + 1);
      }
      throw e;
    }
  }

  /** Shutdown hook, for ensuring the the remaining log data gets shipped to s3 on shutdown */
  @PreDestroy
  public void executeServerShutdownHook() {
    log.info("Shutdown event detected, telling appender to roll current log");
    getRollingPolicy().ifPresent(AuditLogsS3TimeBasedRollingPolicy::rollover);
    log.info("Letting thread pool finish uploading remaining queued logs, with 10 minute timeout");
    executor.shutdown();
    log.info("Finished processing log upload queue");
    try {
      executor.awaitTermination(10, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      log.error("Failed to wait and allow executor to finish jobs, shutting down now");
      executor.shutdownNow();
    }
  }

  /**
   * @return the AuditLogsS3FixedWindowRollingPolicy from the audit logger that implements
   *     ServerShutdownHook so that it can be registered with the shutdown hooks
   */
  private Optional<AuditLogsS3TimeBasedRollingPolicy<ILoggingEvent>> getRollingPolicy() {

    if (athenaLoggingEventListenerEnabled) {
      ch.qos.logback.classic.Logger auditLogger = this.logger;

      FiveMinuteRollingFileAppender<ILoggingEvent> appender =
          (FiveMinuteRollingFileAppender<ILoggingEvent>)
              auditLogger.getAppender(ATHENA_LOG_APPENDER);
      // TODO figure out if appender is always null and when it should not be null
      if (appender != null) {
        return Optional.ofNullable(
            (AuditLogsS3TimeBasedRollingPolicy<ILoggingEvent>) appender.getRollingPolicy());
      }
    }
    return Optional.empty();
  }
}
