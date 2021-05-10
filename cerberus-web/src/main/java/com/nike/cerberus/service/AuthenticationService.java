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

import static com.nike.cerberus.domain.DomainConstants.AWS_GLOBAL_PARTITION_NAME;
import static com.nike.cerberus.domain.DomainConstants.AWS_IAM_ROLE_ARN_TEMPLATE;
import static com.nike.cerberus.security.CerberusPrincipal.*;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;
import com.amazonaws.services.kms.model.KMSInvalidStateException;
import com.amazonaws.services.kms.model.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.PrincipalType;
import com.nike.cerberus.auth.connector.AuthConnector;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.domain.AuthTokenResponse;
import com.nike.cerberus.domain.AwsIamKmsAuthRequest;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.domain.EncryptedAuthDataWrapper;
import com.nike.cerberus.domain.IamRoleCredentials;
import com.nike.cerberus.domain.MfaCheckRequest;
import com.nike.cerberus.domain.UserCredentials;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.error.KeyInvalidForAuthException;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.CustomApiError;
import com.nike.cerberus.util.DateTimeSupplier;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Authentication service for Users and IAM roles to be able to authenticate and get an assigned
 * auth token.
 */
@Component
public class AuthenticationService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public static final String SYSTEM_USER = "system";
  public static final String LOOKUP_SELF_POLICY = "lookup-self";
  public static final int KMS_SIZE_LIMIT = 4096;

  private final AwsIamRoleDao awsIamRoleDao;
  private final AuthConnector authServiceConnector;
  private final KmsService kmsService;
  private final KmsClientFactory kmsClientFactory;
  private final ObjectMapper objectMapper;
  private final String adminGroup;
  private final DateTimeSupplier dateTimeSupplier;
  private final AwsIamRoleArnParser awsIamRoleArnParser;
  private final AuthTokenService authTokenService;
  private final String userTokenTTL;
  private final String iamTokenTTL;
  private final AwsIamRoleService awsIamRoleService;
  private final int maxTokenRefreshCount;
  private final boolean cacheEnabled;
  private final Cache<AwsIamKmsAuthRequest, EncryptedAuthDataWrapper> kmsAuthCache;

  // package exposed for testing, todo maybe fix?
  String adminRoleArns;

  private Set<String> adminRoleArnSet;

  @Autowired
  public AuthenticationService(
      AwsIamRoleDao awsIamRoleDao,
      AuthConnector authConnector,
      KmsService kmsService,
      KmsClientFactory kmsClientFactory,
      ObjectMapper objectMapper,
      @Value("${cerberus.admin.roles:#{null}}") String adminRoleArns,
      @Value("${cerberus.admin.group}") String adminGroup,
      @Value("${cerberus.auth.user.token.maxRefreshCount:#{0}}") int maxTokenRefreshCount,
      DateTimeSupplier dateTimeSupplier,
      AwsIamRoleArnParser awsIamRoleArnParser,
      AuthTokenService authTokenService,
      @Value("${cerberus.auth.user.token.ttl}") String userTokenTTL,
      @Value("${cerberus.auth.iam.token.ttl}") String iamTokenTTL,
      AwsIamRoleService awsIamRoleService,
      @Value("${cerberus.auth.iam.kms.cache.enabled:#{false}}") boolean cacheEnabled,
      Cache<AwsIamKmsAuthRequest, EncryptedAuthDataWrapper> kmsAuthCache) {

    this.awsIamRoleDao = awsIamRoleDao;
    this.authServiceConnector = authConnector;
    this.kmsService = kmsService;
    this.kmsClientFactory = kmsClientFactory;
    this.objectMapper = objectMapper;
    this.adminRoleArns = adminRoleArns;
    this.adminGroup = adminGroup;
    this.dateTimeSupplier = dateTimeSupplier;
    this.awsIamRoleArnParser = awsIamRoleArnParser;
    this.maxTokenRefreshCount = maxTokenRefreshCount;
    this.authTokenService = authTokenService;
    this.userTokenTTL = userTokenTTL;
    this.iamTokenTTL = iamTokenTTL;
    this.awsIamRoleService = awsIamRoleService;
    this.cacheEnabled = cacheEnabled;
    this.kmsAuthCache = kmsAuthCache;
  }

  /**
   * Enables a user to authenticate with their credentials and get back a token with any policies
   * they are entitled to. If a MFA check is required, the details are contained within the auth
   * response.
   *
   * @param credentials User credentials for the authenticating user
   * @return The auth response
   */
  public AuthResponse authenticate(final UserCredentials credentials) {
    final AuthResponse authResponse =
        authServiceConnector.authenticate(
            credentials.getUsername(),
            new String(credentials.getPassword(), Charset.defaultCharset()));

    if (authResponse.getStatus() == AuthStatus.SUCCESS) {
      authResponse
          .getData()
          .setClientToken(
              generateToken(
                  credentials.getUsername(),
                  authServiceConnector.getGroups(authResponse.getData()),
                  0));
    }

    return authResponse;
  }

  /**
   * Enables a user to trigger a factor challenge.
   *
   * @param challengeRequest Request containing the MFA token details with no passcode
   * @return The auth response
   */
  public AuthResponse triggerChallenge(final MfaCheckRequest challengeRequest) {
    final AuthResponse authResponse =
        authServiceConnector.triggerChallenge(
            challengeRequest.getStateToken(), challengeRequest.getDeviceId());

    return authResponse;
  }

  /**
   * Enables a user to trigger a factor challenge.
   *
   * @param challengeRequest Request containing the MFA token details with no passcode
   * @return The auth response
   */
  public AuthResponse triggerPush(final MfaCheckRequest challengeRequest) {
    final AuthResponse authResponse =
        authServiceConnector.triggerPush(
            challengeRequest.getStateToken(), challengeRequest.getDeviceId());
    if (authResponse.getStatus() == AuthStatus.SUCCESS) {
      authResponse
          .getData()
          .setClientToken(
              generateToken(
                  authResponse.getData().getUsername(),
                  authServiceConnector.getGroups(authResponse.getData()),
                  0));
    }
    return authResponse;
  }

  /**
   * Enables a user to execute an MFA check to complete authentication and get an auth token.
   *
   * @param mfaCheckRequest Request containing the MFA token details
   * @return The auth response
   */
  public AuthResponse mfaCheck(final MfaCheckRequest mfaCheckRequest) {
    final AuthResponse authResponse =
        authServiceConnector.mfaCheck(
            mfaCheckRequest.getStateToken(),
            mfaCheckRequest.getDeviceId(),
            mfaCheckRequest.getOtpToken());

    if (authResponse.getStatus() == AuthStatus.SUCCESS) {
      authResponse
          .getData()
          .setClientToken(
              generateToken(
                  authResponse.getData().getUsername(),
                  authServiceConnector.getGroups(authResponse.getData()),
                  0));
    }

    return authResponse;
  }

  /**
   * Enables an IAM role to authenticate and get back an encrypted payload that the role is only
   * able to decrypt with KMS.
   *
   * @param credentials IAM role credentials
   * @return Encrypted auth response
   */
  public EncryptedAuthDataWrapper authenticate(IamRoleCredentials credentials) {

    final String iamPrincipalArn =
        String.format(
            AWS_IAM_ROLE_ARN_TEMPLATE,
            AWS_GLOBAL_PARTITION_NAME, // hardcoding this to AWS Global for backwards compatibility
            credentials.getAccountId(),
            credentials.getRoleName());
    final String region = credentials.getRegion();

    final AwsIamKmsAuthRequest awsIamKmsAuthRequest = new AwsIamKmsAuthRequest();
    awsIamKmsAuthRequest.setIamPrincipalArn(iamPrincipalArn);
    awsIamKmsAuthRequest.setRegion(region);

    final Map<String, String> authPrincipalMetadata =
        generateCommonIamPrincipalAuthMetadata(iamPrincipalArn, region);
    authPrincipalMetadata.put(
        CerberusPrincipal.METADATA_KEY_AWS_ACCOUNT_ID,
        awsIamRoleArnParser.getAccountId(iamPrincipalArn));
    authPrincipalMetadata.put(
        CerberusPrincipal.METADATA_KEY_AWS_IAM_ROLE_NAME,
        awsIamRoleArnParser.getRoleName(iamPrincipalArn));

    return cachingKmsAuthenticate(awsIamKmsAuthRequest, authPrincipalMetadata);
  }

  public EncryptedAuthDataWrapper authenticate(AwsIamKmsAuthRequest awsIamKmsAuthRequest) {

    final String iamPrincipalArn = awsIamKmsAuthRequest.getIamPrincipalArn();
    awsIamRoleArnParser.iamPrincipalPartitionCheck(iamPrincipalArn);
    final Map<String, String> authPrincipalMetadata =
        generateCommonIamPrincipalAuthMetadata(iamPrincipalArn, awsIamKmsAuthRequest.getRegion());
    authPrincipalMetadata.put(
        CerberusPrincipal.METADATA_KEY_AWS_IAM_PRINCIPAL_ARN, iamPrincipalArn);

    return cachingKmsAuthenticate(awsIamKmsAuthRequest, authPrincipalMetadata);
  }

  /**
   * Enables an IAM role to authenticate and get back an UNENCRYPTED payload
   *
   * @param iamPrincipalArn IAM role ARN
   * @return Unencrypted auth response
   */
  public AuthTokenResponse stsAuthenticate(final String iamPrincipalArn) {
    awsIamRoleArnParser.iamPrincipalPartitionCheck(iamPrincipalArn);
    final Map<String, String> authPrincipalMetadata =
        generateCommonIamPrincipalAuthMetadata(iamPrincipalArn);
    authPrincipalMetadata.put(
        CerberusPrincipal.METADATA_KEY_AWS_IAM_PRINCIPAL_ARN, iamPrincipalArn);
    final AwsIamRoleRecord iamRoleRecord =
        getIamPrincipalRecord(
            iamPrincipalArn); // throws error if iam principal not associated with SDB
    return createToken(
        iamRoleRecord.getAwsIamRoleArn(), PrincipalType.IAM, authPrincipalMetadata, iamTokenTTL);
  }

  private EncryptedAuthDataWrapper cachingKmsAuthenticate(
      AwsIamKmsAuthRequest credentials, Map<String, String> authPrincipalMetadata) {
    if (cacheEnabled) {
      return kmsAuthCache.get(credentials, key -> authenticate(credentials, authPrincipalMetadata));
    } else {
      return authenticate(credentials, authPrincipalMetadata);
    }
  }

  private EncryptedAuthDataWrapper authenticate(
      AwsIamKmsAuthRequest credentials, Map<String, String> authPrincipalMetadata) {
    final AwsIamRoleKmsKeyRecord kmsKeyRecord;
    final AwsIamRoleRecord iamRoleRecord;
    try {
      iamRoleRecord = getIamPrincipalRecord(credentials.getIamPrincipalArn());
      kmsKeyRecord = getKmsKeyRecordForIamPrincipal(iamRoleRecord, credentials.getRegion());
    } catch (AmazonServiceException e) {
      if ("InvalidArnException".equals(e.getErrorCode())) {
        String msg =
            String.format(
                "Failed to lazily provision KMS key for %s in region: %s",
                credentials.getIamPrincipalArn(), credentials.getRegion());
        throw ApiException.newBuilder()
            .withApiErrors(
                CustomApiError.createCustomApiError(DefaultApiError.AUTH_IAM_ROLE_REJECTED, msg))
            .withExceptionCause(e)
            .withExceptionMessage(msg)
            .build();
      }
      throw e;
    }

    AuthTokenResponse authResponse =
        createToken(
            iamRoleRecord.getAwsIamRoleArn(),
            PrincipalType.IAM,
            authPrincipalMetadata,
            iamTokenTTL);

    byte[] authResponseJson;
    try {
      authResponseJson = objectMapper.writeValueAsBytes(authResponse);
    } catch (JsonProcessingException e) {
      String msg = "Failed to write IAM role authentication response as JSON for encrypting.";
      throw ApiException.newBuilder()
          .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
          .withExceptionCause(e)
          .withExceptionMessage(msg)
          .build();
    }

    authResponseJson =
        validateAuthPayloadSizeAndTruncateIfLargerThanMaxKmsSupportedSize(
            authResponseJson, authResponse, credentials.getIamPrincipalArn());

    final byte[] encryptedAuthResponse =
        safeEncryptWithRetry(
            kmsKeyRecord.getAwsIamRoleId(),
            credentials.getIamPrincipalArn(),
            kmsKeyRecord.getId(),
            kmsKeyRecord.getAwsKmsKeyId(),
            credentials.getRegion(),
            authResponseJson);

    EncryptedAuthDataWrapper encryptedAuthDataWrapper = new EncryptedAuthDataWrapper();
    encryptedAuthDataWrapper.setAuthData(Base64.encodeBase64String(encryptedAuthResponse));
    return encryptedAuthDataWrapper;
  }

  private AuthTokenResponse createToken(
      String principal,
      PrincipalType principalType,
      Map<String, String> metadata,
      String vaultStyleTTL) {

    PeriodFormatter formatter =
        new PeriodFormatterBuilder()
            .appendHours()
            .appendSuffix("h")
            .appendMinutes()
            .appendSuffix("m")
            .toFormatter();

    Period ttl = formatter.parsePeriod(vaultStyleTTL);
    long ttlInMinutes = ttl.toStandardMinutes().getMinutes();

    // todo eliminate this data coming from a map which may or may not contain the data and force
    // the data to be
    // required as method parameters
    boolean isAdmin = Boolean.valueOf(metadata.get(METADATA_KEY_IS_ADMIN));
    String groups = metadata.get(METADATA_KEY_GROUPS);
    int refreshCount =
        Integer.parseInt(metadata.getOrDefault(METADATA_KEY_TOKEN_REFRESH_COUNT, "0"));

    CerberusAuthToken tokenResult =
        authTokenService.generateToken(
            principal, principalType, isAdmin, groups, ttlInMinutes, refreshCount);

    return new AuthTokenResponse()
        .setClientToken(tokenResult.getToken())
        .setPolicies(Collections.emptySet())
        .setMetadata(metadata)
        .setLeaseDuration(
            Duration.between(tokenResult.getCreated(), tokenResult.getExpires()).getSeconds())
        .setRenewable(PrincipalType.USER.equals(principalType));
  }

  /**
   * If the metadata and policies make the token too big to encrypt with KMS we can as a stop gap
   * trim the metadata and policies from the token.
   *
   * @param authResponseJson The current serialized auth payload
   * @param authToken The auth payload, with the original policies and metadata
   * @param iamPrincipal The calling iam principal
   * @return a serialized auth payload that KMS can encrypt
   */
  protected byte[] validateAuthPayloadSizeAndTruncateIfLargerThanMaxKmsSupportedSize(
      byte[] authResponseJson, AuthTokenResponse authToken, String iamPrincipal) {

    if (authResponseJson.length <= KMS_SIZE_LIMIT) {
      return authResponseJson;
    }

    String originalMetadata = "unknown";
    String originalPolicies = "unknown";
    try {
      originalMetadata = objectMapper.writeValueAsString(authToken.getMetadata());
      originalPolicies = objectMapper.writeValueAsString(authToken.getPolicies());
    } catch (JsonProcessingException e) {
      logger.warn(
          "Failed to serialize original metadata or policies for token generated for IAM Principal: {}",
          iamPrincipal,
          e);
    }

    authToken.setMetadata(ImmutableMap.of("_truncated", "true"));
    authToken.setPolicies(ImmutableSet.of("_truncated"));

    logger.debug(
        "The auth token has length: {} which is > {} KMS cannot encrypt it, truncating auth payload by removing policies and metadata "
            + "original metadata: {} "
            + "original policies: {}",
        authResponseJson.length,
        KMS_SIZE_LIMIT,
        originalMetadata,
        originalPolicies);

    try {
      return objectMapper.writeValueAsBytes(authToken);
    } catch (JsonProcessingException e) {
      String msg = "Failed to write IAM role authentication response as JSON for encrypting.";
      throw ApiException.newBuilder()
          .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
          .withExceptionCause(e)
          .withExceptionMessage(msg)
          .build();
    }
  }

  /**
   * Since tokens are immutable, there are certain situations where refreshing the token used by a
   * user is necessary. Anytime permissions change, this is required to reflect that to the user.
   *
   * @param authPrincipal The principal for the caller
   * @return The auth response with the token and metadata
   */
  public AuthResponse refreshUserToken(final CerberusPrincipal authPrincipal) {

    if (!PrincipalType.USER.equals(authPrincipal.getPrincipalType())) {
      String msg = "The principal: %s attempted to use the user token refresh method";
      throw ApiException.newBuilder()
          .withApiErrors(
              CustomApiError.createCustomApiError(DefaultApiError.USER_ONLY_RESOURCE, msg))
          .withExceptionMessage(msg)
          .build();
    }

    Integer currentTokenRefreshCount = authPrincipal.getTokenRefreshCount();
    if (currentTokenRefreshCount >= maxTokenRefreshCount) {
      throw ApiException.newBuilder()
          .withApiErrors(DefaultApiError.MAXIMUM_TOKEN_REFRESH_COUNT_REACHED)
          .withExceptionMessage(
              String.format(
                  "The principal %s attempted to refresh its token but has "
                      + "reached the maximum number of refreshes allowed",
                  authPrincipal.getName()))
          .build();
    }

    revoke(authPrincipal, authPrincipal.getTokenExpires());
    final AuthData authData =
        AuthData.builder()
            .username(authPrincipal.getName())
            .clientToken(
                generateToken(
                    authPrincipal.getName(),
                    authPrincipal.getUserGroups(),
                    currentTokenRefreshCount + 1))
            .build();

    final AuthResponse authResponse =
        AuthResponse.builder().status(AuthStatus.SUCCESS).data(authData).build();

    return authResponse;
  }

  /**
   * @param cerberusPrincipal Auth principal to be revoked
   * @param tokenExpires Token expire timestamp
   */
  public void revoke(final CerberusPrincipal cerberusPrincipal, OffsetDateTime tokenExpires) {
    authTokenService.revokeToken(cerberusPrincipal, tokenExpires);
  }

  /**
   * Creates a token request and executes it returning the auth response.
   *
   * @param username The user requesting a token
   * @param userGroups The user's groups
   * @return The auth response with the token and metadata
   */
  private AuthTokenResponse generateToken(
      final String username, final Set<String> userGroups, int refreshCount) {
    final Map<String, String> meta = Maps.newHashMap();
    meta.put(CerberusPrincipal.METADATA_KEY_USERNAME, username);

    boolean isAdmin = false;
    if (userGroups.contains(this.adminGroup)) {
      isAdmin = true;
    }
    meta.put(METADATA_KEY_IS_ADMIN, String.valueOf(isAdmin));
    meta.put(CerberusPrincipal.METADATA_KEY_GROUPS, StringUtils.join(userGroups, ','));
    meta.put(CerberusPrincipal.METADATA_KEY_TOKEN_REFRESH_COUNT, String.valueOf(refreshCount));
    meta.put(
        CerberusPrincipal.METADATA_KEY_MAX_TOKEN_REFRESH_COUNT,
        String.valueOf(maxTokenRefreshCount));

    return createToken(username, PrincipalType.USER, meta, userTokenTTL);
  }

  protected AwsIamRoleRecord getIamPrincipalRecord(String iamPrincipalArn) {
    final Optional<AwsIamRoleRecord> iamRole = findIamRoleAssociatedWithSdb(iamPrincipalArn);

    if (!iamRole.isPresent()) {
      String msg = String.format("The role: %s was not configured for any SDB", iamPrincipalArn);
      throw ApiException.newBuilder()
          .withApiErrors(
              CustomApiError.createCustomApiError(DefaultApiError.AUTH_IAM_PRINCIPAL_INVALID, msg))
          .withExceptionMessage(msg)
          .build();
    }

    return iamRole.get();
  }

  protected AwsIamRoleKmsKeyRecord getKmsKeyRecordForIamPrincipal(
      final AwsIamRoleRecord iamRoleRecord, final String awsRegion) {
    final Optional<AwsIamRoleKmsKeyRecord> kmsKey =
        awsIamRoleDao.getKmsKey(iamRoleRecord.getId(), awsRegion);

    final AwsIamRoleKmsKeyRecord kmsKeyRecord;
    final OffsetDateTime now = dateTimeSupplier.get();

    if (!kmsKey.isPresent()) {
      kmsKeyRecord =
          kmsService.provisionKmsKey(
              iamRoleRecord.getId(), iamRoleRecord.getAwsIamRoleArn(), awsRegion, SYSTEM_USER, now);
    } else {
      kmsKeyRecord = kmsKey.get();

      // regenerate the KMS key policy, if it is invalid
      kmsService.validateKeyAndPolicy(kmsKeyRecord, iamRoleRecord.getAwsIamRoleArn());
    }

    return kmsKeyRecord;
  }

  /**
   * Encrypt the given payload with KMS. If the given KMS key is invalid, create a new key and
   * encrypt using the new key.
   *
   * @param iamPrincipalArn The IAM principal ARN associated with the key
   * @param kmsKeyRecordId The ID of the KMS key record
   * @param keyId The AWS ARN of the KMS key
   * @param awsRegion The region in which the KMS key exists
   * @param data The data which to encrypt with KMS
   * @return The encrypted payload
   */
  private byte[] safeEncryptWithRetry(
      final String iamRoleRecordId,
      final String iamPrincipalArn,
      final String kmsKeyRecordId,
      final String keyId,
      final String awsRegion,
      final byte[] data) {
    try {
      return encrypt(awsRegion, keyId, data);
    } catch (KeyInvalidForAuthException invalidKeyException) {
      logger.error(
          "The KMS key with id: {} for principal: {} is disabled or scheduled for deletion. "
              + "The record for this KMS key will be deleted and a new KMS key will be created.",
          keyId,
          iamPrincipalArn);

      kmsService.deleteKmsKeyById(kmsKeyRecordId);
      AwsIamRoleKmsKeyRecord newKeyRecord =
          kmsService.provisionKmsKey(
              iamRoleRecordId, iamPrincipalArn, awsRegion, SYSTEM_USER, dateTimeSupplier.get());

      return encrypt(awsRegion, newKeyRecord.getAwsKmsKeyId(), data);
    }
  }

  /**
   * Encrypts the data provided using KMS based on the provided region and key id.
   *
   * @param regionName Region where key is located
   * @param keyId Key id
   * @param data Data to be encrypted
   * @return encrypted data
   */
  private byte[] encrypt(final String regionName, final String keyId, final byte[] data) {
    Region region;
    try {
      region = Region.getRegion(Regions.fromName(regionName));
    } catch (IllegalArgumentException iae) {
      throw ApiException.newBuilder()
          .withApiErrors(DefaultApiError.AUTH_IAM_ROLE_AWS_REGION_INVALID)
          .withExceptionCause(iae)
          .build();
    }

    final AWSKMSClient kmsClient = kmsClientFactory.getClient(region);

    try {
      final EncryptResult encryptResult =
          kmsClient.encrypt(
              new EncryptRequest().withKeyId(keyId).withPlaintext(ByteBuffer.wrap(data)));

      return encryptResult.getCiphertextBlob().array();
    } catch (NotFoundException | KMSInvalidStateException keyNotUsableException) {
      throw new KeyInvalidForAuthException(
          String.format("Failed to encrypt token using KMS key with id: %s", keyId),
          keyNotUsableException);
    } catch (AmazonClientException ace) {
      String msg =
          String.format("Unexpected error communicating with AWS KMS for region %s.", regionName);
      throw ApiException.newBuilder()
          .withApiErrors(
              CustomApiError.createCustomApiError(DefaultApiError.INTERNAL_SERVER_ERROR, msg))
          .withExceptionCause(ace)
          .withExceptionMessage(msg)
          .build();
    }
  }

  private Set<String> getAdminRoleArnSet() {
    if (adminRoleArnSet == null) {
      adminRoleArnSet = new HashSet<>();
      if (StringUtils.isNotBlank(adminRoleArns)) {
        String[] roles = adminRoleArns.split(",");
        if (roles.length > 0) {
          Arrays.stream(roles)
              .forEach(
                  role -> {
                    adminRoleArnSet.add(role.trim());
                  });
        }
      }
    }
    return adminRoleArnSet;
  }

  /**
   * Generate map of Token metadata that is common to all principals
   *
   * @param iamPrincipalArn - The authenticating IAM principal ARN
   * @return - Map of token metadata
   */
  protected Map<String, String> generateCommonIamPrincipalAuthMetadata(
      final String iamPrincipalArn) {
    Map<String, String> metadata = Maps.newHashMap();
    metadata.put(CerberusPrincipal.METADATA_KEY_USERNAME, iamPrincipalArn);
    metadata.put(CerberusPrincipal.METADATA_KEY_IS_IAM_PRINCIPAL, Boolean.TRUE.toString());

    Set<String> groups = new HashSet<>();
    groups.add("registered-iam-principals");

    // We will allow specific ARNs access to the user portions of the API
    Set<String> adminRoleArnSet = getAdminRoleArnSet();

    if (adminRoleArnSet.contains(iamPrincipalArn)
        || awsIamRoleArnParser.isAssumedRoleArn(iamPrincipalArn)
            && adminRoleArnSet.contains(
                awsIamRoleArnParser.convertPrincipalArnToRoleArn(iamPrincipalArn))) {
      metadata.put(METADATA_KEY_IS_ADMIN, Boolean.toString(true));
      groups.add("admin-iam-principals");
    } else {
      metadata.put(METADATA_KEY_IS_ADMIN, Boolean.toString(false));
    }
    metadata.put(CerberusPrincipal.METADATA_KEY_GROUPS, StringUtils.join(groups, ','));

    return metadata;
  }

  /**
   * Generate map of Token metadata that is common to all principals
   *
   * @param iamPrincipalArn - The authenticating IAM principal ARN
   * @param region - The AWS region
   * @return - Map of token metadata
   */
  protected Map<String, String> generateCommonIamPrincipalAuthMetadata(
      final String iamPrincipalArn, final String region) {
    Map<String, String> metadata = generateCommonIamPrincipalAuthMetadata(iamPrincipalArn);
    metadata.put(CerberusPrincipal.METADATA_KEY_AWS_REGION, region);
    return metadata;
  }

  /**
   * Search for the given IAM principal (e.g. arn:aws:iam::1111111111:instance-profile/example), if
   * not found, then also search for the base role that the principal assumes (i.e.
   * arn:aws:iam::1111111111:role/example)
   *
   * @param iamPrincipalArn - The authenticating IAM principal ARN
   * @return - The associated IAM role record
   */
  protected Optional<AwsIamRoleRecord> findIamRoleAssociatedWithSdb(String iamPrincipalArn) {
    Optional<AwsIamRoleRecord> iamRole = awsIamRoleDao.getIamRole(iamPrincipalArn);

    // if the arn is not already in 'role' format, and cannot be found,
    // then try checking for the generic "arn:aws:iam::0000000000:role/foo" format
    if (iamRole.isEmpty() && !awsIamRoleArnParser.isRoleArn(iamPrincipalArn)) {
      logger.debug(
          "Detected non-role ARN, attempting to find SDBs associated with the principal's base role...");
      // Minimal code change to stop authentication with assumed-role ARN from inserting too many
      // rows into AWS_IAM_ROLE table
      iamPrincipalArn = awsIamRoleArnParser.convertPrincipalArnToRoleArn(iamPrincipalArn);

      iamRole = awsIamRoleDao.getIamRole(iamPrincipalArn);
    }

    if (iamRole.isEmpty()) {
      String accountRootArn = awsIamRoleArnParser.convertPrincipalArnToRootArn(iamPrincipalArn);
      boolean rootArnExists = awsIamRoleDao.getIamRole(accountRootArn).isPresent();
      if (rootArnExists) {
        AwsIamRoleRecord newAwsIamRoleRecord = awsIamRoleService.createIamRole(iamPrincipalArn);
        iamRole = Optional.of(newAwsIamRoleRecord);
      }
    }

    return iamRole;
  }
}
