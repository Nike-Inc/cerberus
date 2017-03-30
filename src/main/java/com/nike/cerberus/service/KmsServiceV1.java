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

package com.nike.cerberus.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.CreateAliasRequest;
import com.amazonaws.services.kms.model.CreateKeyRequest;
import com.amazonaws.services.kms.model.CreateKeyResult;
import com.amazonaws.services.kms.model.GetKeyPolicyRequest;
import com.amazonaws.services.kms.model.GetKeyPolicyResult;
import com.amazonaws.services.kms.model.KeyMetadata;
import com.amazonaws.services.kms.model.KeyUsageType;
import com.amazonaws.services.kms.model.PutKeyPolicyRequest;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.util.UuidSupplier;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.OffsetDateTime;

/**
 * Abstracts interactions with the AWS KMS service.
 */
@Singleton
public class KmsServiceV1 {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String KMS_ALIAS_FORMAT = "alias/cerberus/%s";

    private final AwsIamRoleDao awsIamRoleDao;

    private final UuidSupplier uuidSupplier;

    private final KmsClientFactory kmsClientFactory;

    private final KmsPolicyServiceV1 kmsPolicyService;

    @Inject
    public KmsServiceV1(final AwsIamRoleDao awsIamRoleDao,
                        final UuidSupplier uuidSupplier,
                        final KmsClientFactory kmsClientFactory,
                        final KmsPolicyServiceV1 kmsPolicyService) {
        this.awsIamRoleDao = awsIamRoleDao;
        this.uuidSupplier = uuidSupplier;
        this.kmsClientFactory = kmsClientFactory;
        this.kmsPolicyService = kmsPolicyService;
    }

    /**
     * Provisions a new KMS CMK in the specified region to be used by the specified role.
     *
     * @param iamRoleId        The IAM role that this CMK will be associated with
     * @param iamRoleArn       The AWS IAM role ARN
     * @param awsRegion        The region to provision the key in
     * @param user             The user requesting it
     * @param dateTime         The date of creation
     * @return The AWS Key ID ARN
     */
    @Transactional
    public String provisionKmsKey(final String iamRoleId,
                                  final String iamRoleArn,
                                  final String awsRegion,
                                  final String user,
                                  final OffsetDateTime dateTime) {
        final AWSKMSClient kmsClient = kmsClientFactory.getClient(awsRegion);

        final String awsIamRoleKmsKeyId = uuidSupplier.get();

        final CreateKeyRequest request = new CreateKeyRequest();
        request.setKeyUsage(KeyUsageType.ENCRYPT_DECRYPT);
        request.setDescription("Key used by Cerberus for IAM role authentication.");
        request.setPolicy(kmsPolicyService.generateStandardKmsPolicy(iamRoleArn));
        final CreateKeyResult result = kmsClient.createKey(request);

        final CreateAliasRequest aliasRequest = new CreateAliasRequest();
        aliasRequest.setAliasName(getAliasName(awsIamRoleKmsKeyId));
        KeyMetadata keyMetadata = result.getKeyMetadata();
        String arn = keyMetadata.getArn();
        aliasRequest.setTargetKeyId(arn);
        kmsClient.createAlias(aliasRequest);

        final AwsIamRoleKmsKeyRecord awsIamRoleKmsKeyRecord = new AwsIamRoleKmsKeyRecord();
        awsIamRoleKmsKeyRecord.setId(awsIamRoleKmsKeyId);
        awsIamRoleKmsKeyRecord.setAwsIamRoleId(iamRoleId);
        awsIamRoleKmsKeyRecord.setAwsKmsKeyId(result.getKeyMetadata().getArn());
        awsIamRoleKmsKeyRecord.setAwsRegion(awsRegion);
        awsIamRoleKmsKeyRecord.setCreatedBy(user);
        awsIamRoleKmsKeyRecord.setLastUpdatedBy(user);
        awsIamRoleKmsKeyRecord.setCreatedTs(dateTime);
        awsIamRoleKmsKeyRecord.setLastUpdatedTs(dateTime);

        awsIamRoleDao.createIamRoleKmsKey(awsIamRoleKmsKeyRecord);

        return result.getKeyMetadata().getArn();
    }

    protected String getAliasName(String awsIamRoleKmsKeyId) {
        return String.format(KMS_ALIAS_FORMAT, awsIamRoleKmsKeyId);
    }

    /**
     * When a KMS key policy statement is created and an AWS ARN is specified as a principal,
     * AWS behind the scene binds that ARNs ID to the statement and not the ARN.
     *
     * They do this so that when a role or user is deleted and another team or person recreates the identity
     * that this new identity does not get permissions that were not meant for it.
     *
     * In Cerberus's case we need to support a flow where teams use automation to automatically destroy and recreate IAM roles.
     *
     * We can detect that this event has happened because when an Identity that was referenced by an ARN in a KMS policy
     * statement has been deleted the ARN is replaced by the ID. We can validate that principal matches an ARN pattern
     * or recreate the policy.
     *
     * @param keyId - The CMK Id to validate the policies on.
     * @param iamRoleArn - The Role ARN that should have decrypt permission
     * @param kmsCMKRegion - The region that the key was provisioned for
     */
    public void validatePolicy(String keyId, String iamRoleArn, String kmsCMKRegion) {
        AWSKMSClient kmsClient = kmsClientFactory.getClient(kmsCMKRegion);
        GetKeyPolicyResult policyResult = null;
        try {
            policyResult = kmsClient.getKeyPolicy(new GetKeyPolicyRequest().withKeyId(keyId).withPolicyName("default"));
        } catch (AmazonServiceException e) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.FAILED_TO_VALIDATE_KMS_KEY_POLICY)
                    .withExceptionCause(e)
                    .withExceptionMessage(
                            String.format("Failed to validate KMS key policy for keyId: " +
                                    "%s for IAM role: %s in region: %s", keyId, iamRoleArn, kmsCMKRegion))
                    .build();
        }

        if (!kmsPolicyService.isPolicyValid(policyResult.getPolicy(), iamRoleArn)) {
            logger.info("The KMS key: {} generated for IAM Role: {} contained an invalid policy, regenerating",
                    keyId, iamRoleArn);
            String updatedPolicy = kmsPolicyService.generateStandardKmsPolicy(iamRoleArn);
            kmsClient.putKeyPolicy(new PutKeyPolicyRequest()
                    .withKeyId(keyId)
                    .withPolicyName("default")
                    .withPolicy(updatedPolicy)
            );
        }
    }
}
