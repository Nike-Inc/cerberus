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
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.S3Object;
import com.nike.cerberus.ServerInitializationError;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * Polls the vault properties from the Cerberus config bucket and loads them into Archaius.
 */
public class CmsEnvPropertiesLoader {

    private static final String ENV_PATH = "data/cms/environment.properties";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AmazonS3EncryptionClient s3Client;

    private final String bucketName;

    public CmsEnvPropertiesLoader(final String bucketName, final String region, final String kmsKeyId) {
        final KMSEncryptionMaterialsProvider materialProvider =
                new KMSEncryptionMaterialsProvider(kmsKeyId);

        this.s3Client =
                new AmazonS3EncryptionClient(
                        new DefaultAWSCredentialsProviderChain(),
                        materialProvider,
                        new CryptoConfiguration()
                                .withAwsKmsRegion(Region.getRegion(
                                        Regions.fromName(region))))
                        .withRegion(Region.getRegion(Regions.fromName(region)));

        this.bucketName = bucketName;
    }

    public Properties getProperties() {
        final String vaultPropertiesContents = getObject(ENV_PATH);

        if (StringUtils.isBlank(vaultPropertiesContents)) {
            throw new IllegalStateException("Vault properties file was blank!");
        }

        final Properties vaultProperties = new Properties();
        try {
            vaultProperties.load(new StringReader(vaultPropertiesContents));
        } catch (IOException e) {
            throw new ServerInitializationError("Failed to read the vault properties contents!", e);
        }

        return vaultProperties;
    }

    private String getObject(String path) {
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
