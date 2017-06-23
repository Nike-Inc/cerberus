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
import com.amazonaws.services.kms.model.NotFoundException;
import com.amazonaws.services.kms.model.PutKeyPolicyRequest;
import com.amazonaws.services.kms.model.ScheduleKeyDeletionRequest;
import com.google.inject.name.Named;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.UuidSupplier;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.nike.cerberus.service.AuthenticationService.SYSTEM_USER;

/**
 * Abstracts interactions with the AWS KMS service.
 */
@Singleton
public class KmsService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String KMS_ALIAS_FORMAT = "alias/cerberus/%s";

    private static final String KMS_POLICY_VALIDATION_INTERVAL_OVERRIDE = "cms.kms.policy.validation.interval.millis.override";

    private static final Integer DEFAULT_KMS_VALIDATION_INTERVAL = 6000;  // in milliseconds

    public static final Integer SOONEST_A_KMS_KEY_CAN_BE_DELETED = 7;  // in days

    private final AwsIamRoleDao awsIamRoleDao;

    private final UuidSupplier uuidSupplier;

    private final KmsClientFactory kmsClientFactory;

    private final KmsPolicyService kmsPolicyService;

    private final DateTimeSupplier dateTimeSupplier;

    @com.google.inject.Inject(optional=true)
    @Named(KMS_POLICY_VALIDATION_INTERVAL_OVERRIDE)
    Integer kmsKeyPolicyValidationInterval = DEFAULT_KMS_VALIDATION_INTERVAL;

    @Inject
    public KmsService(final AwsIamRoleDao awsIamRoleDao,
                      final UuidSupplier uuidSupplier,
                      final KmsClientFactory kmsClientFactory,
                      final KmsPolicyService kmsPolicyService,
                      final DateTimeSupplier dateTimeSupplier) {
        this.awsIamRoleDao = awsIamRoleDao;
        this.uuidSupplier = uuidSupplier;
        this.kmsClientFactory = kmsClientFactory;
        this.kmsPolicyService = kmsPolicyService;
        this.dateTimeSupplier = dateTimeSupplier;
    }

    /**
     * Provisions a new KMS CMK in the specified region to be used by the specified role.
     *
     * @param iamRoleId        The IAM role that this CMK will be associated with
     * @param iamPrincipalArn  The AWS IAM principal ARN
     * @param awsRegion        The region to provision the key in
     * @param user             The user requesting it
     * @param dateTime         The date of creation
     * @return The AWS Key ID ARN
     */
    @Transactional
    public String provisionKmsKey(final String iamRoleId,
                                  final String iamPrincipalArn,
                                  final String awsRegion,
                                  final String user,
                                  final OffsetDateTime dateTime) {
        final AWSKMSClient kmsClient = kmsClientFactory.getClient(awsRegion);

        final String awsIamPrincipalKmsKeyId = uuidSupplier.get();

        final CreateKeyRequest request = new CreateKeyRequest();
        request.setKeyUsage(KeyUsageType.ENCRYPT_DECRYPT);
        request.setDescription("Key used by Cerberus for IAM role authentication.");
        request.setPolicy(kmsPolicyService.generateStandardKmsPolicy(iamPrincipalArn));
        final CreateKeyResult result = kmsClient.createKey(request);

        final CreateAliasRequest aliasRequest = new CreateAliasRequest();
        aliasRequest.setAliasName(getAliasName(awsIamPrincipalKmsKeyId));
        KeyMetadata keyMetadata = result.getKeyMetadata();
        String arn = keyMetadata.getArn();
        aliasRequest.setTargetKeyId(arn);
        kmsClient.createAlias(aliasRequest);

        final AwsIamRoleKmsKeyRecord awsIamRoleKmsKeyRecord = new AwsIamRoleKmsKeyRecord();
        awsIamRoleKmsKeyRecord.setId(awsIamPrincipalKmsKeyId);
        awsIamRoleKmsKeyRecord.setAwsIamRoleId(iamRoleId);
        awsIamRoleKmsKeyRecord.setAwsKmsKeyId(result.getKeyMetadata().getArn());
        awsIamRoleKmsKeyRecord.setAwsRegion(awsRegion);
        awsIamRoleKmsKeyRecord.setCreatedBy(user);
        awsIamRoleKmsKeyRecord.setLastUpdatedBy(user);
        awsIamRoleKmsKeyRecord.setCreatedTs(dateTime);
        awsIamRoleKmsKeyRecord.setLastUpdatedTs(dateTime);
        awsIamRoleKmsKeyRecord.setLastValidatedTs(dateTime);

        awsIamRoleDao.createIamRoleKmsKey(awsIamRoleKmsKeyRecord);

        return result.getKeyMetadata().getArn();
    }

    /**
     * Updates the KMS CMK record for the specified IAM role and region
     * @param awsIamRoleId    The IAM role that this CMK will be associated with
     * @param awsRegion       The region to provision the key in
     * @param user            The user requesting it
     * @param lastedUpdatedTs The date when the record was last updated
     * @param lastValidatedTs The date when the record was last validated
     */
    @Transactional
    public void updateKmsKey(final String awsIamRoleId,
                             final String awsRegion,
                             final String user,
                             final OffsetDateTime lastedUpdatedTs,
                             final OffsetDateTime lastValidatedTs) {
        final Optional<AwsIamRoleKmsKeyRecord> kmsKey = awsIamRoleDao.getKmsKey(awsIamRoleId, awsRegion);

        if (!kmsKey.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                    .withExceptionMessage("Unable to update a KMS key that does not exist.")
                    .build();
        }

        AwsIamRoleKmsKeyRecord kmsKeyRecord = kmsKey.get();

        AwsIamRoleKmsKeyRecord updatedKmsKeyRecord = new AwsIamRoleKmsKeyRecord();
        updatedKmsKeyRecord.setAwsIamRoleId(kmsKeyRecord.getAwsIamRoleId());
        updatedKmsKeyRecord.setLastUpdatedBy(user);
        updatedKmsKeyRecord.setLastUpdatedTs(lastedUpdatedTs);
        updatedKmsKeyRecord.setLastValidatedTs(lastValidatedTs);
        updatedKmsKeyRecord.setAwsRegion(kmsKeyRecord.getAwsRegion());
        awsIamRoleDao.updateIamRoleKmsKey(updatedKmsKeyRecord);
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
     * @param kmsKeyRecord - The CMK record to validate policy on
     * @param iamPrincipalArn - The principal ARN that should have decrypt permission
     */
    public void validatePolicy(AwsIamRoleKmsKeyRecord kmsKeyRecord, String iamPrincipalArn) {

        if (! kmsPolicyNeedsValidation(kmsKeyRecord)) {
            return;
        }

        String kmsCMKRegion = kmsKeyRecord.getAwsRegion();
        String awsKmsKeyArn = kmsKeyRecord.getAwsKmsKeyId();
        AWSKMSClient kmsClient = kmsClientFactory.getClient(kmsCMKRegion);
        try {
            GetKeyPolicyResult policyResult = kmsClient.getKeyPolicy(new GetKeyPolicyRequest().withKeyId(awsKmsKeyArn).withPolicyName("default"));

            if (!kmsPolicyService.isPolicyValid(policyResult.getPolicy(), iamPrincipalArn)) {
                logger.info("The KMS key: {} generated for IAM principal: {} contained an invalid policy, regenerating",
                        awsKmsKeyArn, iamPrincipalArn);
                String updatedPolicy = kmsPolicyService.generateStandardKmsPolicy(iamPrincipalArn);
                kmsClient.putKeyPolicy(new PutKeyPolicyRequest()
                        .withKeyId(awsKmsKeyArn)
                        .withPolicyName("default")
                        .withPolicy(updatedPolicy)
                );
            }

            // update last validated timestamp
            OffsetDateTime now = dateTimeSupplier.get();
            updateKmsKey(kmsKeyRecord.getAwsIamRoleId(), kmsCMKRegion, SYSTEM_USER, now, now);
        } catch(NotFoundException nfe) {
            logger.warn("Failed to validate KMS policy because the KMS key did not exist, but the key record did." +
                            "Deleting the key record to prevent this from failing again: keyId: {} for IAM principal: {} in region: {}",
                        awsKmsKeyArn, iamPrincipalArn, kmsCMKRegion, nfe);
            awsIamRoleDao.deleteKmsKeyById(kmsKeyRecord.getId());
        } catch (AmazonServiceException e) {
            logger.warn(String.format("Failed to validate KMS policy for keyId: %s for IAM principal: %s in region: %s. API limit" +
                    " may have been reached for validate call.", awsKmsKeyArn, iamPrincipalArn, kmsCMKRegion), e);
        }
    }

    /**
     * Delete a CMK in AWS
     * @param kmsKeyId - The AWS KMS Key ID
     * @param region - The KMS key region
     */
    public void scheduleKmsKeyDeletion(String kmsKeyId, String region, Integer pendingWindowInDays) {

        final AWSKMSClient kmsClient = kmsClientFactory.getClient(region);
        final ScheduleKeyDeletionRequest scheduleKeyDeletionRequest = new ScheduleKeyDeletionRequest()
                .withKeyId(kmsKeyId)
                .withPendingWindowInDays(pendingWindowInDays);

        kmsClient.scheduleKeyDeletion(scheduleKeyDeletionRequest);
    }

    /**
     * Determines if given KMS policy should be validated
     * @param kmsKeyRecord - KMS key record to check for validation
     * @return True if needs validation, False if not
     */
    protected boolean kmsPolicyNeedsValidation(AwsIamRoleKmsKeyRecord kmsKeyRecord) {

        OffsetDateTime now = dateTimeSupplier.get();
        long timeSinceLastValidatedInMillis = ChronoUnit.MILLIS.between(kmsKeyRecord.getLastValidatedTs(), now);

        return timeSinceLastValidatedInMillis >= kmsKeyPolicyValidationInterval;
    }
}
