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
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.GetKeyPolicyRequest;
import com.amazonaws.services.kms.model.KeyUsageType;
import com.amazonaws.services.kms.model.NotFoundException;
import com.amazonaws.services.kms.model.PutKeyPolicyRequest;
import com.amazonaws.services.kms.model.ScheduleKeyDeletionRequest;
import com.amazonaws.services.kms.model.Tag;
import com.google.inject.name.Named;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.Slugger;
import com.nike.cerberus.util.UuidSupplier;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static com.nike.cerberus.service.AuthenticationService.SYSTEM_USER;

/**
 * Abstracts interactions with the AWS KMS service.
 */
@Singleton
public class KmsService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String ALIAS_DELIMITER = "/";
    private static final int MAX_KMS_ALIAS_LENGTH = 256;

    private static final String KMS_POLICY_VALIDATION_INTERVAL_OVERRIDE = "cms.kms.policy.validation.interval.millis.override";

    private static final Integer DEFAULT_KMS_VALIDATION_INTERVAL = (int) TimeUnit.MINUTES.toMillis(5);  // in milliseconds

    public static final Integer SOONEST_A_KMS_KEY_CAN_BE_DELETED = 7;  // in days

    private final AwsIamRoleDao awsIamRoleDao;
    private final UuidSupplier uuidSupplier;
    private final KmsClientFactory kmsClientFactory;
    private final KmsPolicyService kmsPolicyService;
    private final DateTimeSupplier dateTimeSupplier;
    private final AwsIamRoleArnParser awsIamRoleArnParser;
    private final Slugger slugger;

    private final String cmsVersion;
    private final String environmentName;

    @com.google.inject.Inject(optional=true)
    @Named(KMS_POLICY_VALIDATION_INTERVAL_OVERRIDE)
    Integer kmsKeyPolicyValidationInterval = DEFAULT_KMS_VALIDATION_INTERVAL;

    @Inject
    public KmsService(final AwsIamRoleDao awsIamRoleDao,
                      final UuidSupplier uuidSupplier,
                      final KmsClientFactory kmsClientFactory,
                      final KmsPolicyService kmsPolicyService,
                      final DateTimeSupplier dateTimeSupplier,
                      AwsIamRoleArnParser awsIamRoleArnParser,
                      @Named("service.version") String cmsVersion,
                      @Named("cms.env.name") String environmentName,
                      final Slugger slugger) {
        this.awsIamRoleDao = awsIamRoleDao;
        this.uuidSupplier = uuidSupplier;
        this.kmsClientFactory = kmsClientFactory;
        this.kmsPolicyService = kmsPolicyService;
        this.dateTimeSupplier = dateTimeSupplier;
        this.awsIamRoleArnParser = awsIamRoleArnParser;
        this.cmsVersion = cmsVersion;
        this.environmentName = environmentName;
        this.slugger = slugger;
    }

    /**
     * Provisions a new KMS CMK in the specified region to be used by the specified role.
     *
     * @param iamRoleRecordId        The IAM role that this CMK will be associated with
     * @param iamPrincipalArn  The AWS IAM principal ARN
     * @param awsRegion        The region to provision the key in
     * @param user             The user requesting it
     * @param dateTime         The date of creation
     * @return The AWS Key ID ARN
     */
    public AwsIamRoleKmsKeyRecord provisionKmsKey(final String iamRoleRecordId,
                                                  final String iamPrincipalArn,
                                                  final String awsRegion,
                                                  final String user,
                                                  final OffsetDateTime dateTime) {
        final String kmsKeyRecordId = uuidSupplier.get();

        final String awsKmsKeyArn = createKmsKeyInAws(iamPrincipalArn, kmsKeyRecordId, awsRegion);
        return createKmsKeyRecord(iamRoleRecordId, kmsKeyRecordId, awsKmsKeyArn,
                awsRegion,
                user,
                dateTime);
    }

    @Transactional
    private AwsIamRoleKmsKeyRecord createKmsKeyRecord(final String iamRoleRecordId,
                                                      final String kmsKeyRecordId,
                                                      final String awsKmsKeyArn,
                                                      final String awsRegion,
                                                      final String user,
                                                      final OffsetDateTime dateTime) {
        final AwsIamRoleKmsKeyRecord awsIamRoleKmsKeyRecord = new AwsIamRoleKmsKeyRecord();

        awsIamRoleKmsKeyRecord.setId(kmsKeyRecordId)
                .setAwsIamRoleId(iamRoleRecordId)
                .setAwsKmsKeyId(awsKmsKeyArn)
                .setAwsRegion(awsRegion)
                .setCreatedBy(user)
                .setLastUpdatedBy(user)
                .setCreatedTs(dateTime)
                .setLastUpdatedTs(dateTime)
                .setLastValidatedTs(dateTime);

        awsIamRoleDao.createIamRoleKmsKey(awsIamRoleKmsKeyRecord);

        return awsIamRoleKmsKeyRecord;
    }

    private String createKmsKeyInAws(String iamPrincipalArn, String kmsKeyRecordId, String awsRegion) {
        final AWSKMSClient kmsClient = kmsClientFactory.getClient(awsRegion);

        final CreateKeyRequest request = new CreateKeyRequest()
                .withKeyUsage(KeyUsageType.ENCRYPT_DECRYPT)
                .withDescription("Key used by Cerberus " + environmentName + " for IAM role authentication. " + iamPrincipalArn)
                .withPolicy(kmsPolicyService.generateStandardKmsPolicy(iamPrincipalArn))
                .withTags(
                    createTag("created_by", "cms" + cmsVersion),
                    createTag("created_for", "cerberus_auth"),
                    createTag("auth_principal", iamPrincipalArn),
                    createTag("cerberus_env", environmentName)
                );

        final CreateKeyResult result = kmsClient.createKey(request);

        String kmsKeyAliasName = getAliasName(kmsKeyRecordId, iamPrincipalArn);
        String kmsKeyArn = result.getKeyMetadata().getArn();
        try {
            // alias is only used to provide extra description in AWS console
            final CreateAliasRequest aliasRequest = new CreateAliasRequest()
                    .withAliasName(kmsKeyAliasName)
                    .withTargetKeyId(result.getKeyMetadata().getArn());
            kmsClient.createAlias(aliasRequest);
        } catch (RuntimeException re) {
            logger.error("Failed to create KMS alias: {}, for keyId: {}", kmsKeyAliasName, kmsKeyArn);
        }

        return kmsKeyArn;
    }

    /**
     * Create a KMS tag truncating the key/value as needed to ensure successful operation
     */
    private Tag createTag(String key, String value) {
        // extra safety measures here are probably not needed but just in case there are any weird edge cases
        return new Tag()
                .withTagKey(StringUtils.substring(key, 0, 128))
                .withTagValue(StringUtils.substring(value != null ? value : "unknown", 0, 256));
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

    @Transactional
    public void deleteKmsKeyById(final String kmsKeyId) {
        awsIamRoleDao.deleteKmsKeyById(kmsKeyId);
    }

    /**
     * Generate a unique and descriptive alias name for a KMS key
     * @param awsIamRoleKmsKeyId UUID
     * @param iamPrincipalArn the main ARN this key is being created for.
     */
    protected String getAliasName(String awsIamRoleKmsKeyId, String iamPrincipalArn) {
        // create a descriptive text for the alias including as much info as possible
        String descriptiveText = "alias/cerberus/" + environmentName + ALIAS_DELIMITER + awsIamRoleArnParser.stripOutDescription(iamPrincipalArn);
        String validAliasText = slugger.slugifyKmsAliases(descriptiveText);

        // if the descriptive text is too long then truncate it (this seems very unlikely to happen)
        if (validAliasText.length() + ALIAS_DELIMITER.length() + awsIamRoleKmsKeyId.length() > MAX_KMS_ALIAS_LENGTH) {
            validAliasText = StringUtils.substring(validAliasText, 0, MAX_KMS_ALIAS_LENGTH - ALIAS_DELIMITER.length() - awsIamRoleKmsKeyId.length());
            // remove final '/' if it exists
            validAliasText = StringUtils.stripEnd(validAliasText, ALIAS_DELIMITER);
        }

        // tack on the UUID onto the end to make sure the alias is unique
        return validAliasText + ALIAS_DELIMITER + awsIamRoleKmsKeyId;
    }

    /**
     * Perform validation and fix some issues.
     *
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
    public void validateKeyAndPolicy(AwsIamRoleKmsKeyRecord kmsKeyRecord, String iamPrincipalArn) {

        if (! kmsPolicyNeedsValidation(kmsKeyRecord, iamPrincipalArn)) {
            // Avoiding extra calls to AWS so that we don't get rate limited.
            return;
        }

        String kmsCMKRegion = kmsKeyRecord.getAwsRegion();
        String awsKmsKeyArn = kmsKeyRecord.getAwsKmsKeyId();
        try {
            String keyPolicy = getKmsKeyPolicy(awsKmsKeyArn, kmsCMKRegion);

            if (!kmsPolicyService.isPolicyValid(keyPolicy)) {
                logger.info("The KMS key: {} generated for IAM principal: {} contained an invalid policy, regenerating",
                        awsKmsKeyArn, iamPrincipalArn);

                String updatedPolicy = kmsPolicyService.generateStandardKmsPolicy(iamPrincipalArn);

                updateKmsKeyPolicy(updatedPolicy, awsKmsKeyArn, kmsCMKRegion);
            }

            // update last validated timestamp
            OffsetDateTime now = dateTimeSupplier.get();
            updateKmsKey(kmsKeyRecord.getAwsIamRoleId(), kmsCMKRegion, SYSTEM_USER, now, now);
        } catch(NotFoundException nfe) {
            logger.warn("Failed to validate KMS policy because the KMS key did not exist, but the key record did." +
                            "Deleting the key record to prevent this from failing again: keyId: {} for IAM principal: {} in region: {}",
                        awsKmsKeyArn, iamPrincipalArn, kmsCMKRegion, nfe);
            deleteKmsKeyById(kmsKeyRecord.getId());
        } catch (AmazonServiceException | HystrixRuntimeException e) {
            logger.warn("Failed to validate KMS policy for keyId: {} for IAM principal: {} in region: {} due to {}",
                    awsKmsKeyArn, iamPrincipalArn, kmsCMKRegion, e.toString());
        } catch (RejectedExecutionException e) {
            logger.warn("Hystrix rejected policy lookup, thread pool full, {}", e.toString());
        } catch (RuntimeException e) {
            logger.warn("Failed to look up policy, Hystrix is probably short circuited", e);
        }
    }

    /**
     * Validate the the CMS IAM role has permissions to ScheduleKeyDeletion and CancelKeyDeletion for the give CMK
     * if not, then update the policy to give the appropriate permissions.
     * @param awsKmsKeyId - The ARN of the CMK to validate
     * @param kmsCMKRegion - The region that the CMK exists in
     */
    protected void validatePolicyAllowsCMSToDeleteCMK(String awsKmsKeyId, String kmsCMKRegion) {

        try {
            String policyJson = getKmsKeyPolicy(awsKmsKeyId, kmsCMKRegion);

            if (!kmsPolicyService.cmsHasKeyDeletePermissions(policyJson)) {
                // Overwrite the policy statement for CMS only, instead of regenerating the entire policy because regenerating
                // the full policy would require unnecessarily looking up the associated IAM principal in the DB
                String updatedPolicy = kmsPolicyService.overwriteCMSPolicy(policyJson);

                // If the consumer IAM principal has been deleted then the policy will contain a principal 'ID' instead
                // of and ARN, rendering the policy invalid. So delete the consumer statement here just in case
                String updatedPolicyWithNoConsumer = kmsPolicyService.removeConsumerPrincipalFromPolicy(updatedPolicy);

                updateKmsKeyPolicy(updatedPolicyWithNoConsumer, awsKmsKeyId, kmsCMKRegion);
            }
        } catch (AmazonServiceException ase) {
            logger.error("Failed to validate that CMS can delete the given KMS key, ARN: {}, region: {}", awsKmsKeyId, kmsCMKRegion, ase);
        } catch (IllegalArgumentException iae) {
            logger.error("Failed to add ScheduleKeyDeletion to key policy for CMK: {}, because the policy does" +
                    "not contain any allow statements for the CMS role", awsKmsKeyId, iae);
        }

    }

    /**
     * Gets the KMS key policy from AWS for the given CMK
     */
    protected String getKmsKeyPolicy(String kmsKeyId, String kmsCMKRegion) {

        AWSKMSClient kmsClient = kmsClientFactory.getClient(kmsCMKRegion);

        GetKeyPolicyRequest request = new GetKeyPolicyRequest().withKeyId(kmsKeyId).withPolicyName("default");

        return kmsClient.getKeyPolicy(request).getPolicy();
    }

    /**
     * Updates the KMS key policy in AWS for the given CMK
     */
    protected void updateKmsKeyPolicy(String updatedPolicyJson, String awsKmsKeyArn, String kmsCMKRegion) {

        AWSKMSClient kmsClient = kmsClientFactory.getClient(kmsCMKRegion);

        kmsClient.putKeyPolicy(new PutKeyPolicyRequest()
                .withKeyId(awsKmsKeyArn)
                .withPolicyName("default")
                .withPolicy(updatedPolicyJson)
        );
    }

    /**
     * Get the state of the KMS key
     * @param kmsKeyId - The AWS KMS Key ID
     * @param region - The KMS key region
     * @return - KMS key state
     */
    protected String getKmsKeyState(String kmsKeyId, String region) {

        AWSKMSClient kmsClient = kmsClientFactory.getClient(region);
        DescribeKeyRequest request = new DescribeKeyRequest().withKeyId(kmsKeyId);

        return kmsClient.describeKey(request)
                .getKeyMetadata()
                .getKeyState();
    }

    /**
     * Delete a CMK in AWS
     * @param kmsKeyId - The AWS KMS Key ID
     * @param region - The KMS key region
     */
    protected void scheduleKmsKeyDeletion(String kmsKeyId, String region, Integer pendingWindowInDays) {

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
    protected boolean kmsPolicyNeedsValidation(AwsIamRoleKmsKeyRecord kmsKeyRecord, String arn) {

        OffsetDateTime now = dateTimeSupplier.get();
        long timeSinceLastValidatedInMillis = ChronoUnit.MILLIS.between(kmsKeyRecord.getLastValidatedTs(), now);

        boolean needValidation = timeSinceLastValidatedInMillis >= kmsKeyPolicyValidationInterval;

        logger.debug("last validated: {}, time since last validated in millis: {}, " +
                        "kmsKeyPolicyValidationInterval: {}, needs validation: {}",
                kmsKeyRecord.getLastValidatedTs(),
                timeSinceLastValidatedInMillis,
                kmsKeyPolicyValidationInterval,
                needValidation);

        if (needValidation) {
            logger.info("Re-validating kms policy for arn: {}", arn);
        }

        return needValidation;
    }
}
