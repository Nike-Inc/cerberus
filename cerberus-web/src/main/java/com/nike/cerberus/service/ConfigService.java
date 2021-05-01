/*
 * Copyright (c) 2021 Nike, Inc.
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name = "cerberus.auth.jwt.secret.local.enabled", havingValue = "false")
@Component
public class ConfigService {

  private static final String JWT_SECRETS_PATH = "cms/jwt-secrets.json";

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final AmazonS3 s3Client;

  private final String bucketName;

  private final AwsCrypto awsCrypto;

  private final Region currentRegion;

  @Autowired
  public ConfigService(
      @Value("${cerberus.auth.jwt.secret.bucket}") final String bucketName,
      final String region,
      AwsCrypto awsCrypto) {

    currentRegion = Region.getRegion(Regions.fromName(region));
    this.s3Client = AmazonS3Client.builder().withRegion(region).build();

    this.bucketName = bucketName;
    this.awsCrypto = awsCrypto;
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
