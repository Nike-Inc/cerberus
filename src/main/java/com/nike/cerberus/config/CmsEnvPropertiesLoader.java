/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.config;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.nike.cerberus.ServerInitializationError;
import com.nike.cerberus.util.CiphertextUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Properties;

import static com.nike.cerberus.service.EncryptionService.decrypt;

/**
 * Reads configuration from the Cerberus config bucket in S3
 */
public class CmsEnvPropertiesLoader {

    private static final String ENV_PATH = "cms/environment.properties";
    private static final String CERTIFICATE_PATH = "certificates/%s/cert.pem";
    private static final String PRIVATE_KEY_PATH = "certificates/%s/pkcs8-key.pem";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AmazonS3 s3Client;

    private final String bucketName;

    private final AwsCrypto awsCrypto;

    private final Region currentRegion;


    public CmsEnvPropertiesLoader(final String bucketName,
                                  final String region,
                                  AwsCrypto awsCrypto) {

        currentRegion = Region.getRegion(Regions.fromName(region));
        this.s3Client = AmazonS3Client.builder().withRegion(region).build();

        this.bucketName = bucketName;
        this.awsCrypto = awsCrypto;
    }

    public Properties getProperties() {
        final String propertyContents = getPlainText(ENV_PATH);

        if (StringUtils.isBlank(propertyContents)) {
            throw new IllegalStateException(ENV_PATH + " file was blank!");
        }

        final Properties props = new Properties();
        try {
            props.load(new StringReader(propertyContents));
        } catch (IOException e) {
            throw new ServerInitializationError("Failed to read " + ENV_PATH + " contents!", e);
        }

        return props;
    }

    /**
     * Get the value of the Certificate from S3
     * @param certificateName
     */
    public String getCertificate(String certificateName) {
        return getPlainText(String.format(CERTIFICATE_PATH, certificateName));
    }

    /**
     * Get the value of the PKCS8 Private Key from S3.
     *
     * The SslContextBuilder and Netty´s SslContext implementations only support PKCS8 keys.
     *
     * http://netty.io/wiki/sslcontextbuilder-and-private-key.html
     * @param certificateName
     */
    public String getPrivateKey(String certificateName) {
        return getPlainText(String.format(PRIVATE_KEY_PATH, certificateName));
    }

    private String getPlainText(String path) {
        try {
            return decrypt(CiphertextUtils.parse(getCipherText(path)), awsCrypto, currentRegion);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to download and decrypt environment specific properties from s3", e);
        }
    }

    private String getCipherText(String path) {
        final GetObjectRequest request = new GetObjectRequest(bucketName, path);

        try {
            S3Object s3Object = s3Client.getObject(request);
            InputStream object = s3Object.getObjectContent();
            return IOUtils.toString(object, Charset.defaultCharset());
        } catch (AmazonServiceException ase) {
            if (StringUtils.equalsIgnoreCase(ase.getErrorCode(), "NoSuchKey")) {
                final String errorMessage = String.format("The S3 object doesn't exist. Bucket: %s, Key: %s",
                        bucketName, request.getKey());
                logger.debug(errorMessage);
                throw new IllegalStateException(errorMessage);
            } else {
                logger.error("Unexpected error communicating with AWS.", ase);
                throw ase;
            }
        } catch (IOException e) {
            String errorMessage =
                    String.format("Unable to read contents of S3 object. Bucket: %s, Key: %s, Expected Encoding: %s",
                            bucketName, request.getKey(), Charset.defaultCharset());
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage, e);
        }
    }
}
