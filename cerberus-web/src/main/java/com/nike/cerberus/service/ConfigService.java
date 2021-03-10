package com.nike.cerberus.service;

import static com.nike.cerberus.service.EncryptionService.decrypt;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.nike.cerberus.util.CiphertextUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigService {

  private static final String JWT_SECRETS_PATH = "cms/jwt-secrets.json";

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final AmazonS3 s3Client;

  private final String bucketName;

  private final AwsCrypto awsCrypto;

  private final Region currentRegion;

  private final boolean s3ConfigEnabled;

  @Autowired
  public ConfigService(
      @Value("${cerberus.auth.jwt.secret.bucket}") final String bucketName,
      final String region,
      AwsCrypto awsCrypto,
      @Value("${cerberus.s3.config.enabled: #{false}}") boolean s3ConfigEnabled) {

    currentRegion = Region.getRegion(Regions.fromName(region));
    this.s3Client = AmazonS3Client.builder().withRegion(region).build();

    this.bucketName = bucketName;
    this.awsCrypto = awsCrypto;
    this.s3ConfigEnabled = s3ConfigEnabled;
  }

  /** Returns whether or not config can be loaded from S3 */
  public boolean isS3ConfigDisabled() {
    return s3ConfigEnabled;
  }

  public String getJwtSecrets() {
    return getPlainText(JWT_SECRETS_PATH);
  }

  private String getPlainText(String path) {
    try {
      return decrypt(CiphertextUtils.parse(getCipherText(path)), awsCrypto, currentRegion);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to download and decrypt environment specific properties from s3", e);
    }
  }

  private String getCipherText(String path) {
    final GetObjectRequest request = new GetObjectRequest(bucketName, path);

    try {
      S3Object s3Object = s3Client.getObject(request);
      InputStream object = s3Object.getObjectContent();
      return IOUtils.toString(object);
    } catch (AmazonServiceException ase) {
      if (StringUtils.equalsIgnoreCase(ase.getErrorCode(), "NoSuchKey")) {
        final String errorMessage =
            String.format(
                "The S3 object doesn't exist. Bucket: %s, Key: %s", bucketName, request.getKey());
        logger.debug(errorMessage);
        throw new IllegalStateException(errorMessage);
      } else {
        logger.error("Unexpected error communicating with AWS.", ase);
        throw ase;
      }
    } catch (IOException e) {
      String errorMessage =
          String.format(
              "Unable to read contents of S3 object. Bucket: %s, Key: %s, Expected Encoding: %s",
              bucketName, request.getKey(), Charset.defaultCharset());
      logger.error(errorMessage);
      throw new IllegalStateException(errorMessage, e);
    }
  }
}
