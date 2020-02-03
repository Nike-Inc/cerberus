/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.service;

import static com.nike.cerberus.service.AuthenticationService.SYSTEM_USER;
import static com.nike.cerberus.service.KmsPolicyService.CERBERUS_MANAGEMENT_SERVICE_SID;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.*;
import com.google.common.collect.ImmutableSet;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.domain.AuthKmsKeyMetadata;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.Slugger;
import com.nike.cerberus.util.UuidSupplier;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Abstracts interactions with the AWS KMS service. */
@Deprecated
@Component
public class KmsService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private static final String ALIAS_DELIMITER = "/";
  private static final int MAX_KMS_ALIAS_LENGTH = 256;

  public static final Integer SOONEST_A_KMS_KEY_CAN_BE_DELETED = 7; // in days

  private final AwsIamRoleDao awsIamRoleDao;
  private final UuidSupplier uuidSupplier;
  private final KmsClientFactory kmsClientFactory;
  private final KmsPolicyService kmsPolicyService;
  private final DateTimeSupplier dateTimeSupplier;
  private final AwsIamRoleArnParser awsIamRoleArnParser;
  private final Slugger slugger;
  private final BuildProperties buildProperties;

  private final String environmentName;
  private final Integer kmsKeyPolicyValidationInterval;

  @Autowired
  public KmsService(
      AwsIamRoleDao awsIamRoleDao,
      UuidSupplier uuidSupplier,
      KmsClientFactory kmsClientFactory,
      KmsPolicyService kmsPolicyService,
      DateTimeSupplier dateTimeSupplier,
      AwsIamRoleArnParser awsIamRoleArnParser,
      @Value("${cerberus.auth.kms.policy.validation.interval.millis:300000}")
          int kmsKeyPolicyValidationInterval,
      @Value("${cerberus.environmentName}") String environmentName,
      Slugger slugger,
      BuildProperties buildProperties) {

    this.awsIamRoleDao = awsIamRoleDao;
    this.uuidSupplier = uuidSupplier;
    this.kmsClientFactory = kmsClientFactory;
    this.kmsPolicyService = kmsPolicyService;
    this.dateTimeSupplier = dateTimeSupplier;
    this.awsIamRoleArnParser = awsIamRoleArnParser;
    this.kmsKeyPolicyValidationInterval = kmsKeyPolicyValidationInterval;
    this.environmentName = environmentName;
    this.slugger = slugger;
    this.buildProperties = buildProperties;
  }

  /**
   * Provisions a new KMS CMK in the specified region to be used by the specified role.
   *
   * @param iamRoleRecordId The IAM role that this CMK will be associated with
   * @param iamPrincipalArn The AWS IAM principal ARN
   * @param awsRegion The region to provision the key in
   * @param user The user requesting it
   * @param dateTime The date of creation
   * @return The AWS Key ID ARN
   */
  public AwsIamRoleKmsKeyRecord provisionKmsKey(
      final String iamRoleRecordId,
      final String iamPrincipalArn,
      final String awsRegion,
      final String user,
      final OffsetDateTime dateTime) {
    final String kmsKeyRecordId = uuidSupplier.get();

    final String awsKmsKeyArn = createKmsKeyInAws(iamPrincipalArn, kmsKeyRecordId, awsRegion);

    logger.info(
        "Created KMS Key with id: {} for ARN: {}, REGION: {}",
        awsKmsKeyArn,
        iamPrincipalArn,
        awsRegion);

    return createKmsKeyRecord(
        iamRoleRecordId, kmsKeyRecordId, awsKmsKeyArn, awsRegion, user, dateTime);
  }

  @Transactional
  protected AwsIamRoleKmsKeyRecord createKmsKeyRecord(
      final String iamRoleRecordId,
      final String kmsKeyRecordId,
      final String awsKmsKeyArn,
      final String awsRegion,
      final String user,
      final OffsetDateTime dateTime) {
    final AwsIamRoleKmsKeyRecord awsIamRoleKmsKeyRecord = new AwsIamRoleKmsKeyRecord();

    awsIamRoleKmsKeyRecord
        .setId(kmsKeyRecordId)
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

  private String createKmsKeyInAws(
      String iamPrincipalArn, String kmsKeyRecordId, String awsRegion) {
    final AWSKMSClient kmsClient = kmsClientFactory.getClient(awsRegion);

    final String policy = kmsPolicyService.generateStandardKmsPolicy(iamPrincipalArn);

    final CreateKeyRequest request =
        new CreateKeyRequest()
            .withKeyUsage(KeyUsageType.ENCRYPT_DECRYPT)
            .withDescription(
                "Key used by Cerberus "
                    + environmentName
                    + " for IAM role authentication. "
                    + iamPrincipalArn)
            .withPolicy(policy)
            .withTags(
                createTag("created_for", "cerberus_auth"),
                createTag("auth_principal", iamPrincipalArn),
                createTag("cerberus_env", environmentName));

    CreateKeyResult result;
    try {
      result = kmsClient.createKey(request);
    } catch (Throwable t) {
      logger.error("Failed to provision KMS key using policy: {}", policy, t);
      throw t;
    }

    String kmsKeyAliasName = getAliasName(kmsKeyRecordId, iamPrincipalArn);
    String kmsKeyArn = result.getKeyMetadata().getArn();
    try {
      // alias is only used to provide extra description in AWS console
      final CreateAliasRequest aliasRequest =
          new CreateAliasRequest()
              .withAliasName(kmsKeyAliasName)
              .withTargetKeyId(result.getKeyMetadata().getArn());
      kmsClient.createAlias(aliasRequest);
    } catch (RuntimeException re) {
      logger.error("Failed to create KMS alias: {}, for keyId: {}", kmsKeyAliasName, kmsKeyArn);
    }

    return kmsKeyArn;
  }

  /** Create a KMS tag truncating the key/value as needed to ensure successful operation */
  private Tag createTag(String key, String value) {
    // extra safety measures here are probably not needed but just in case there are any weird edge
    // cases
    return new Tag()
        .withTagKey(StringUtils.substring(key, 0, 128))
        .withTagValue(StringUtils.substring(value != null ? value : "unknown", 0, 256));
  }

  /**
   * Updates the KMS CMK record for the specified IAM role and region
   *
   * @param awsIamRoleId The IAM role that this CMK will be associated with
   * @param awsRegion The region to provision the key in
   * @param user The user requesting it
   * @param lastedUpdatedTs The date when the record was last updated
   * @param lastValidatedTs The date when the record was last validated
   */
  @Transactional
  public void updateKmsKey(
      final String awsIamRoleId,
      final String awsRegion,
      final String user,
      final OffsetDateTime lastedUpdatedTs,
      final OffsetDateTime lastValidatedTs) {
    final Optional<AwsIamRoleKmsKeyRecord> kmsKey =
        awsIamRoleDao.getKmsKey(awsIamRoleId, awsRegion);

    if (kmsKey.isEmpty()) {
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

  public List<AuthKmsKeyMetadata> getAuthenticationKmsMetadata() {
    List<AuthKmsKeyMetadata> result = new LinkedList<>();

    Optional<List<AwsIamRoleKmsKeyRecord>> keysOptional = awsIamRoleDao.getAllKmsKeys();
    List<AwsIamRoleKmsKeyRecord> keys = keysOptional.orElse(new LinkedList<>());

    keys.forEach(
        key -> {
          AuthKmsKeyMetadata metadata =
              new AuthKmsKeyMetadata()
                  .setAwsKmsKeyId(key.getAwsKmsKeyId())
                  .setAwsRegion(key.getAwsRegion())
                  .setCreatedTs(key.getCreatedTs())
                  .setLastUpdatedTs(key.getLastUpdatedTs())
                  .setLastValidatedTs(key.getLastValidatedTs());

          awsIamRoleDao
              .getIamRoleById(key.getAwsIamRoleId())
              .ifPresent(
                  awsIamRoleRecord ->
                      metadata.setAwsIamRoleArn(awsIamRoleRecord.getAwsIamRoleArn()));

          result.add(metadata);
        });

    return result;
  }

  /**
   * Generate a unique and descriptive alias name for a KMS key
   *
   * @param awsIamRoleKmsKeyId UUID
   * @param iamPrincipalArn the main ARN this key is being created for.
   */
  protected String getAliasName(String awsIamRoleKmsKeyId, String iamPrincipalArn) {
    // create a descriptive text for the alias including as much info as possible
    String descriptiveText =
        "alias/cerberus/"
            + environmentName
            + ALIAS_DELIMITER
            + awsIamRoleArnParser.stripOutDescription(iamPrincipalArn);
    String validAliasText = slugger.slugifyKmsAliases(descriptiveText);

    // if the descriptive text is too long then truncate it (this seems very unlikely to happen)
    if (validAliasText.length() + ALIAS_DELIMITER.length() + awsIamRoleKmsKeyId.length()
        > MAX_KMS_ALIAS_LENGTH) {
      validAliasText =
          StringUtils.substring(
              validAliasText,
              0,
              MAX_KMS_ALIAS_LENGTH - ALIAS_DELIMITER.length() - awsIamRoleKmsKeyId.length());
      // remove final '/' if it exists
      validAliasText = StringUtils.stripEnd(validAliasText, ALIAS_DELIMITER);
    }

    // tack on the UUID onto the end to make sure the alias is unique
    return validAliasText + ALIAS_DELIMITER + awsIamRoleKmsKeyId;
  }

  /**
   * Perform validation and fix some issues.
   *
   * <p>When a KMS key policy statement is created and an AWS ARN is specified as a principal, AWS
   * behind the scene binds that ARNs ID to the statement and not the ARN.
   *
   * <p>They do this so that when a role or user is deleted and another team or person recreates the
   * identity that this new identity does not get permissions that were not meant for it.
   *
   * <p>In Cerberus's case we need to support a flow where teams use automation to automatically
   * destroy and recreate IAM roles.
   *
   * <p>We can detect that this event has happened because when an Identity that was referenced by
   * an ARN in a KMS policy statement has been deleted the ARN is replaced by the ID. We can
   * validate that principal matches an ARN pattern or recreate the policy.
   *
   * @param kmsKeyRecord - The CMK record to validate policy on
   * @param iamPrincipalArn - The principal ARN that should have decrypt permission
   */
  public void validateKeyAndPolicy(AwsIamRoleKmsKeyRecord kmsKeyRecord, String iamPrincipalArn) {

    if (!kmsPolicyNeedsValidation(kmsKeyRecord, iamPrincipalArn)) {
      // Avoiding extra calls to AWS so that we don't get rate limited.
      return;
    }

    String kmsCMKRegion = kmsKeyRecord.getAwsRegion();
    String awsKmsKeyArn = kmsKeyRecord.getAwsKmsKeyId();
    try {
      String keyPolicy = getKmsKeyPolicy(awsKmsKeyArn, kmsCMKRegion);

      if (!kmsPolicyService.isPolicyValid(keyPolicy)) {
        logger.info(
            "The KMS key: {} generated for IAM principal: {} contained an invalid policy, regenerating",
            awsKmsKeyArn,
            iamPrincipalArn);

        String updatedPolicy = kmsPolicyService.generateStandardKmsPolicy(iamPrincipalArn);

        updateKmsKeyPolicy(updatedPolicy, awsKmsKeyArn, kmsCMKRegion);
      }

      // update last validated timestamp
      OffsetDateTime now = dateTimeSupplier.get();
      updateKmsKey(kmsKeyRecord.getAwsIamRoleId(), kmsCMKRegion, SYSTEM_USER, now, now);
    } catch (NotFoundException nfe) {
      logger.warn(
          "Failed to validate KMS policy because the KMS key did not exist, but the key record did."
              + "Deleting the key record to prevent this from failing again: keyId: {} for IAM principal: {} in region: {}",
          awsKmsKeyArn,
          iamPrincipalArn,
          kmsCMKRegion,
          nfe);
      deleteKmsKeyById(kmsKeyRecord.getId());
    } catch (AmazonServiceException e) {
      logger.warn(
          "Failed to validate KMS policy for keyId: {} for IAM principal: {} in region: {} due to {}",
          awsKmsKeyArn,
          iamPrincipalArn,
          kmsCMKRegion,
          e.toString());
    } catch (RejectedExecutionException e) {
      logger.warn("Hystrix rejected policy lookup, thread pool full, {}", e.toString());
    } catch (RuntimeException e) {
      logger.warn("Failed to look up policy, Hystrix is probably short circuited", e);
    }
  }

  /**
   * Validate the the CMS IAM role has permissions to ScheduleKeyDeletion and CancelKeyDeletion for
   * the give CMK if not, then update the policy to give the appropriate permissions.
   *
   * @param awsKmsKeyId - The ARN of the CMK to validate
   * @param kmsCMKRegion - The region that the CMK exists in
   */
  protected void validatePolicyAllowsCMSToDeleteCMK(String awsKmsKeyId, String kmsCMKRegion) {

    try {
      String policyJson = getKmsKeyPolicy(awsKmsKeyId, kmsCMKRegion);

      if (!kmsPolicyService.cmsHasKeyDeletePermissions(policyJson)) {
        // Overwrite the policy statement for CMS only, instead of regenerating the entire policy
        // because regenerating
        // the full policy would require unnecessarily looking up the associated IAM principal in
        // the DB
        String updatedPolicy = kmsPolicyService.overwriteCMSPolicy(policyJson);

        // If the consumer IAM principal has been deleted then the policy will contain a principal
        // 'ID' instead
        // of and ARN, rendering the policy invalid. So delete the consumer statement here just in
        // case
        String updatedPolicyWithNoConsumer =
            kmsPolicyService.removeConsumerPrincipalFromPolicy(updatedPolicy);

        updateKmsKeyPolicy(updatedPolicyWithNoConsumer, awsKmsKeyId, kmsCMKRegion);
      }
    } catch (AmazonServiceException ase) {
      logger.error(
          "Failed to validate that CMS can delete the given KMS key, ARN: {}, region: {}",
          awsKmsKeyId,
          kmsCMKRegion,
          ase);
    } catch (IllegalArgumentException iae) {
      logger.error(
          "Failed to add ScheduleKeyDeletion to key policy for CMK: {}, because the policy does"
              + "not contain any allow statements for the CMS role",
          awsKmsKeyId,
          iae);
    }
  }

  /** Gets the KMS key policy from AWS for the given CMK */
  protected String getKmsKeyPolicy(String kmsKeyId, String kmsCMKRegion) {

    AWSKMSClient kmsClient = kmsClientFactory.getClient(kmsCMKRegion);

    GetKeyPolicyRequest request =
        new GetKeyPolicyRequest().withKeyId(kmsKeyId).withPolicyName("default");

    return kmsClient.getKeyPolicy(request).getPolicy();
  }

  /** Updates the KMS key policy in AWS for the given CMK */
  protected void updateKmsKeyPolicy(
      String updatedPolicyJson, String awsKmsKeyArn, String kmsCMKRegion) {

    AWSKMSClient kmsClient = kmsClientFactory.getClient(kmsCMKRegion);

    kmsClient.putKeyPolicy(
        new PutKeyPolicyRequest()
            .withKeyId(awsKmsKeyArn)
            .withPolicyName("default")
            .withPolicy(updatedPolicyJson));
  }

  /**
   * Get the state of the KMS key
   *
   * @param kmsKeyId - The AWS KMS Key ID
   * @param region - The KMS key region
   * @return - KMS key state
   */
  protected String getKmsKeyState(String kmsKeyId, String region) {

    AWSKMSClient kmsClient = kmsClientFactory.getClient(region);
    DescribeKeyRequest request = new DescribeKeyRequest().withKeyId(kmsKeyId);

    return kmsClient.describeKey(request).getKeyMetadata().getKeyState();
  }

  /**
   * Delete a CMK in AWS
   *
   * @param kmsKeyId - The AWS KMS Key ID
   * @param region - The KMS key region
   */
  public void scheduleKmsKeyDeletion(String kmsKeyId, String region, Integer pendingWindowInDays) {

    logger.info(
        "Scheduling kms cmk id: {} in region: {} for deletion in {} days",
        kmsKeyId,
        region,
        pendingWindowInDays);

    final AWSKMSClient kmsClient = kmsClientFactory.getClient(region);
    final ScheduleKeyDeletionRequest scheduleKeyDeletionRequest =
        new ScheduleKeyDeletionRequest()
            .withKeyId(kmsKeyId)
            .withPendingWindowInDays(pendingWindowInDays);

    try {
      kmsClient.scheduleKeyDeletion(scheduleKeyDeletionRequest);
    } catch (KMSInvalidStateException e) {
      if (e.getErrorMessage().contains("pending deletion")) {
        logger.warn("The key: {} in region: {} is already pending deletion", kmsKeyId, region);
      } else {
        throw e;
      }
    }
  }

  /**
   * Determines if given KMS policy should be validated
   *
   * @param kmsKeyRecord - KMS key record to check for validation
   * @return True if needs validation, False if not
   */
  protected boolean kmsPolicyNeedsValidation(AwsIamRoleKmsKeyRecord kmsKeyRecord, String arn) {

    OffsetDateTime now = dateTimeSupplier.get();
    long timeSinceLastValidatedInMillis =
        ChronoUnit.MILLIS.between(kmsKeyRecord.getLastValidatedTs(), now);

    boolean needValidation = timeSinceLastValidatedInMillis >= kmsKeyPolicyValidationInterval;

    logger.debug(
        "last validated: {}, time since last validated in millis: {}, "
            + "kmsKeyPolicyValidationInterval: {}, needs validation: {}",
        kmsKeyRecord.getLastValidatedTs(),
        timeSinceLastValidatedInMillis,
        kmsKeyPolicyValidationInterval,
        needValidation);

    if (needValidation) {
      logger.info("Re-validating kms policy for arn: {}", arn);
    }

    return needValidation;
  }

  /**
   * Attempts to download the policy, if something goes wrong, it returns an empty optional.
   *
   * @param kmsCmkId The KMS CMK that you want the default policy for
   * @param regionName The region that the KMS CMK resides in.
   * @return The policy if it can successfully be fetched.
   */
  protected Optional<Policy> downloadPolicy(String kmsCmkId, String regionName, int retryCount) {
    final Set<String> unretryableErrors =
        ImmutableSet.of("NotFoundException", "InvalidArnException", "AccessDeniedException");
    try {
      var policy =
          kmsPolicyService.getPolicyFromPolicyString(getKmsKeyPolicy(kmsCmkId, regionName));
      return Optional.of(policy);
    } catch (AWSKMSException e) {
      String errorCode = e.getErrorCode();
      logger.error("Failed to download policy, error code: {}", errorCode);
      if (!unretryableErrors.contains(errorCode) && retryCount < 10) {
        try {
          Thread.sleep(500 ^ (retryCount + 1));
        } catch (InterruptedException e1) {
          return Optional.empty();
        }
        return downloadPolicy(kmsCmkId, regionName, retryCount + 1);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets all the KMS CMK ids for a given region
   *
   * @param regionName The region in which you want all the KMS CMK ids
   * @return A list of of the KMS CMK ids for the requested region.
   */
  public Set<String> getKmsKeyIdsForRegion(String regionName) {
    AWSKMS kms = kmsClientFactory.getClient(regionName);

    Set<String> kmsKeyIdsForRegion = new HashSet<>();

    String marker = null;
    do {
      logger.debug("Fetching keys for region: {} and marker: {}", regionName, marker);
      ListKeysRequest listKeysRequest = new ListKeysRequest();
      if (marker != null) {
        listKeysRequest.withMarker(marker);
      }
      ListKeysResult listKeysResult = kms.listKeys(listKeysRequest);
      listKeysResult
          .getKeys()
          .forEach(keyListEntry -> kmsKeyIdsForRegion.add(keyListEntry.getKeyId()));
      marker = listKeysResult.getNextMarker();
    } while (marker != null);

    return kmsKeyIdsForRegion;
  }

  /**
   * Downloads the KMS CMK policies to determine if the key was created by this.
   *
   * @param allKmsCmkIdsForRegion The list of all the KMS Key.
   * @param regionName The region.
   * @return The list of KMS Keys that were created by this.
   */
  public Set<String> filterKeysCreatedByKmsService(
      Set<String> allKmsCmkIdsForRegion, String regionName) {
    Set<String> keysCreatedByThis = new HashSet<>();
    int numberOfKeys = allKmsCmkIdsForRegion.size();
    String[] kmsCmkIds = allKmsCmkIdsForRegion.toArray(new String[numberOfKeys]);

    logger.info(
        "Preparing to download and analyze {} key policies in region: {} to "
            + "determine what keys where created by this env",
        numberOfKeys,
        regionName);

    for (int i = 0; i < numberOfKeys; i++) {
      logger.debug(
          "Processed {}% of keys for region: {}",
          Math.ceil((double) i / numberOfKeys * 100), regionName);
      String kmsCmkId = kmsCmkIds[i];
      if (wasKeyCreatedByKmsService(kmsCmkId, regionName)) {
        keysCreatedByThis.add(kmsCmkId);
      }
    }

    return keysCreatedByThis;
  }

  /**
   * Checks the KMS CMK policy to see if it was created by this environments cms cluster.
   *
   * @param kmsCmkId The KMS CMK id.
   * @param regionName The region.
   * @return true if the kms key was created by CMS for the current env
   */
  private boolean wasKeyCreatedByKmsService(String kmsCmkId, String regionName) {
    boolean wasCreatedByThis = false;

    logger.debug("Downloading policy for key: {} in region: {}", kmsCmkId, regionName);

    Optional<Policy> policyOptional = downloadPolicy(kmsCmkId, regionName, 0);

    if (policyOptional.isPresent()) {
      Policy policy = policyOptional.get();
      if (policy.getStatements().size() == 4) {
        Optional<Statement> cmsStatement =
            policy.getStatements().stream()
                .filter(statement -> statement.getId().equals(CERBERUS_MANAGEMENT_SERVICE_SID))
                .findFirst();

        if (cmsStatement.isPresent()) {
          wasCreatedByThis =
              cmsStatement.get().getPrincipals().stream()
                  .anyMatch(principal -> principal.getId().contains(environmentName));
          logger.debug("detected cms policy, was created by this env: {}", wasCreatedByThis);
        }
      }
    } else {
      logger.warn(
          "Failed to fetch policy for key: {} in region: {}, skipping...", kmsCmkId, regionName);
    }

    return wasCreatedByThis;
  }
}
