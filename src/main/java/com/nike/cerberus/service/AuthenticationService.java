/*
 * Copyright (c) 2017 Nike, Inc.
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

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.PrincipalType;
import com.nike.cerberus.auth.connector.AuthConnector;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.dao.SafeDepositBoxDao;
import com.nike.cerberus.domain.AuthTokenResponse;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.domain.IamRoleAuthResponse;
import com.nike.cerberus.domain.IamRoleCredentials;
import com.nike.cerberus.domain.IamPrincipalCredentials;
import com.nike.cerberus.domain.MfaCheckRequest;
import com.nike.cerberus.domain.UserCredentials;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.record.SafeDepositBoxRoleRecord;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.Slugger;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.nike.cerberus.security.CerberusPrincipal.METADATA_KEY_GROUPS;
import static com.nike.cerberus.security.CerberusPrincipal.METADATA_KEY_IS_ADMIN;
import static com.nike.cerberus.security.CerberusPrincipal.METADATA_KEY_TOKEN_REFRESH_COUNT;
import static com.nike.cerberus.util.AwsIamRoleArnParser.AWS_IAM_ROLE_ARN_TEMPLATE;

/**
 * Authentication service for Users and IAM roles to be able to authenticate and get an assigned Vault token.
 */
@Singleton
public class AuthenticationService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String SYSTEM_USER = "system";
    public static final String ADMIN_GROUP_PROPERTY = "cms.admin.group";
    public static final String MAX_TOKEN_REFRESH_COUNT = "auth.token.maxRefreshCount";
    public static final String ADMIN_IAM_ROLES_PROPERTY = "cms.admin.roles";
    public static final String USER_TOKEN_TTL_OVERRIDE = "cms.user.token.ttl.override";
    public static final String IAM_TOKEN_TTL_OVERRIDE = "cms.iam.token.ttl.override";
    public static final String LOOKUP_SELF_POLICY = "lookup-self";
    public static final String DEFAULT_TOKEN_TTL = "1h";
    public static final int KMS_SIZE_LIMIT = 4096;

    private final SafeDepositBoxDao safeDepositBoxDao;
    private final AwsIamRoleDao awsIamRoleDao;
    private final AuthConnector authServiceConnector;
    private final KmsService kmsService;
    private final KmsClientFactory kmsClientFactory;
    private final ObjectMapper objectMapper;
    private final String adminGroup;
    private final DateTimeSupplier dateTimeSupplier;
    private final AwsIamRoleArnParser awsIamRoleArnParser;
    private final Slugger slugger;
    private final AuthTokenService authTokenService;

    @Inject(optional=true)
    @Named(ADMIN_IAM_ROLES_PROPERTY)
    String adminRoleArns;

    private Set<String> adminRoleArnSet;

    @Inject(optional=true)
    @Named(USER_TOKEN_TTL_OVERRIDE)
    String userTokenTTL = DEFAULT_TOKEN_TTL;

    @Inject(optional=true)
    @Named(IAM_TOKEN_TTL_OVERRIDE)
    String iamTokenTTL = DEFAULT_TOKEN_TTL;

    private final int maxTokenRefreshCount;

    @Inject
    public AuthenticationService(final SafeDepositBoxDao safeDepositBoxDao,
                                 final AwsIamRoleDao awsIamRoleDao,
                                 final AuthConnector authConnector,
                                 final KmsService kmsService,
                                 final KmsClientFactory kmsClientFactory,
                                 final ObjectMapper objectMapper,
                                 @Named(ADMIN_GROUP_PROPERTY) final String adminGroup,
                                 @Named(MAX_TOKEN_REFRESH_COUNT) final int maxTokenRefreshCount,
                                 final DateTimeSupplier dateTimeSupplier,
                                 final AwsIamRoleArnParser awsIamRoleArnParser,
                                 final Slugger slugger,
                                 final AuthTokenService authTokenService) {

        this.safeDepositBoxDao = safeDepositBoxDao;
        this.awsIamRoleDao = awsIamRoleDao;
        this.authServiceConnector = authConnector;
        this.kmsService = kmsService;
        this.kmsClientFactory = kmsClientFactory;
        this.objectMapper = objectMapper;
        this.adminGroup = adminGroup;
        this.dateTimeSupplier = dateTimeSupplier;
        this.awsIamRoleArnParser = awsIamRoleArnParser;
        this.maxTokenRefreshCount = maxTokenRefreshCount;
        this.slugger = slugger;
        this.authTokenService = authTokenService;
    }

    /**
     * Enables a user to authenticate with their credentials and get back a Vault token with any policies they
     * are entitled to.  If a MFA check is required, the details are contained within the auth response.
     *
     * @param credentials User credentials for the authenticating user
     * @return The auth response
     */
    public AuthResponse authenticate(final UserCredentials credentials) {
        final AuthResponse authResponse = authServiceConnector.authenticate(credentials.getUsername(),
                new String(credentials.getPassword(), Charset.defaultCharset()));

        if (authResponse.getStatus() == AuthStatus.SUCCESS) {
            authResponse.getData().setClientToken(generateToken(credentials.getUsername(),
                    authServiceConnector.getGroups(authResponse.getData()), 0));
        }

        return authResponse;
    }

    /**
     * Enables a user to execute an MFA check to complete authentication and get a Vault token.
     *
     * @param mfaCheckRequest Request containing the MFA token details
     * @return The auth response
     */
    public AuthResponse mfaCheck(final MfaCheckRequest mfaCheckRequest) {
        final AuthResponse authResponse = authServiceConnector.mfaCheck(mfaCheckRequest.getStateToken(),
                mfaCheckRequest.getDeviceId(),
                mfaCheckRequest.getOtpToken());

        if (authResponse.getStatus() == AuthStatus.SUCCESS) {
            authResponse.getData().setClientToken(generateToken(authResponse.getData().getUsername(),
                    authServiceConnector.getGroups(authResponse.getData()), 0));
        }

        return authResponse;
    }

    /**
     * Enables an IAM role to authenticate and get back an encrypted payload that the role is only able to decrypt with
     * KMS.
     * @param credentials IAM role credentials
     * @return Encrypted auth response
     */
    public IamRoleAuthResponse authenticate(IamRoleCredentials credentials) {

        final String iamPrincipalArn = String.format(AWS_IAM_ROLE_ARN_TEMPLATE, credentials.getAccountId(),
                credentials.getRoleName());
        final String region = credentials.getRegion();

        final IamPrincipalCredentials iamPrincipalCredentials = new IamPrincipalCredentials();
        iamPrincipalCredentials.setIamPrincipalArn(iamPrincipalArn);
        iamPrincipalCredentials.setRegion(region);

        final Map<String, String> vaultAuthPrincipalMetadata = generateCommonIamPrincipalAuthMetadata(iamPrincipalArn, region);
        vaultAuthPrincipalMetadata.put(CerberusPrincipal.METADATA_KEY_AWS_ACCOUNT_ID, awsIamRoleArnParser.getAccountId(iamPrincipalArn));
        vaultAuthPrincipalMetadata.put(CerberusPrincipal.METADATA_KEY_AWS_IAM_ROLE_NAME, awsIamRoleArnParser.getRoleName(iamPrincipalArn));

        return authenticate(iamPrincipalCredentials, vaultAuthPrincipalMetadata);
    }

    public IamRoleAuthResponse authenticate(IamPrincipalCredentials credentials) {

        final String iamPrincipalArn = credentials.getIamPrincipalArn();
        final Map<String, String> vaultAuthPrincipalMetadata = generateCommonIamPrincipalAuthMetadata(iamPrincipalArn, credentials.getRegion());
        vaultAuthPrincipalMetadata.put(CerberusPrincipal.METADATA_KEY_AWS_IAM_PRINCIPAL_ARN, iamPrincipalArn);

        return authenticate(credentials, vaultAuthPrincipalMetadata);
    }

    private IamRoleAuthResponse authenticate(IamPrincipalCredentials credentials, Map<String, String> vaultAuthPrincipalMetadata) {
        final String keyId;
        try {
            keyId = getKeyId(credentials);
        } catch (AmazonServiceException e) {
            if ("InvalidArnException".equals(e.getErrorCode())) {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.AUTH_IAM_ROLE_REJECTED)
                        .withExceptionCause(e)
                        .withExceptionMessage(String.format(
                                "Failed to lazily provision KMS key for %s in region: %s",
                                credentials.getIamPrincipalArn(), credentials.getRegion()))
                        .build();
            }
            throw e;
        }

        final Set<String> policies = buildCompleteSetOfPolicies(credentials.getIamPrincipalArn());

        AuthTokenResponse authResponse = createToken(
                credentials.getIamPrincipalArn(),
                PrincipalType.IAM,
                policies,
                vaultAuthPrincipalMetadata,
                iamTokenTTL
        );

        byte[] authResponseJson;
        try {
            authResponseJson = objectMapper.writeValueAsBytes(authResponse);
        } catch (JsonProcessingException e) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
                    .withExceptionCause(e)
                    .withExceptionMessage("Failed to write IAM role authentication response as JSON for encrypting.")
                    .build();
        }

        authResponseJson = validateAuthPayloadSizeAndTruncateIfLargerThanMaxKmsSupportedSize(authResponseJson,
                authResponse, credentials.getIamPrincipalArn());

        final byte[] encryptedAuthResponse = encrypt(credentials.getRegion(), keyId, authResponseJson);

        IamRoleAuthResponse iamRoleAuthResponse = new IamRoleAuthResponse();
        iamRoleAuthResponse.setAuthData(Base64.encodeBase64String(encryptedAuthResponse));
        return iamRoleAuthResponse;
    }

    private AuthTokenResponse createToken(String principal,
                                          PrincipalType principalType,
                                          Set<String> policies,
                                          Map<String, String> metadata,
                                          String vaultStyleTTL) {

        PeriodFormatter formatter = new PeriodFormatterBuilder()
                .appendHours().appendSuffix("h")
                .appendMinutes().appendSuffix("m")
                .toFormatter();

        Period ttl = formatter.parsePeriod(vaultStyleTTL);
        long ttlInMinutes = ttl.toStandardMinutes().getMinutes();

        // todo eliminate this data coming from a map which may or may not contain the data and force the data to be
        // required as method parameters
        boolean isAdmin = Boolean.valueOf(metadata.get(METADATA_KEY_IS_ADMIN));
        String groups = metadata.get(METADATA_KEY_GROUPS);
        int refreshCount = Integer.valueOf(metadata.getOrDefault(METADATA_KEY_TOKEN_REFRESH_COUNT, "0"));

        CerberusAuthToken tokenResult = authTokenService
                .generateToken(principal, principalType, isAdmin, groups, ttlInMinutes, refreshCount);

        return new AuthTokenResponse()
                .setClientToken(tokenResult.getToken())
                .setPolicies(policies)
                .setMetadata(metadata)
                .setLeaseDuration(Duration.between(tokenResult.getCreated(), tokenResult.getExpires()).getSeconds())
                .setRenewable(PrincipalType.USER.equals(principalType));
    }

    /**
     * If the metadata and policies make the token too big to encrypt with KMS we can as a stop gap trim the metadata
     * and policies from the token.
     *
     * This information is stored in Vault and can be fetched by the client with a look-up self call
     *
     * @param authResponseJson The current serialized auth payload
     * @param authToken The auth payload, with the original policies and metadata
     * @param iamPrincipal The calling iam principal
     * @return a serialized auth payload that KMS can encrypt
     */
    protected byte[] validateAuthPayloadSizeAndTruncateIfLargerThanMaxKmsSupportedSize(byte[] authResponseJson,
                                                                                     AuthTokenResponse authToken,
                                                                                     String iamPrincipal) {

        if (authResponseJson.length <= KMS_SIZE_LIMIT) {
            return authResponseJson;
        }

        String originalMetadata = "unknown";
        String originalPolicies = "unknown";
        try {
            originalMetadata = objectMapper.writeValueAsString(authToken.getMetadata());
            originalPolicies = objectMapper.writeValueAsString(authToken.getPolicies());
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize original metadata or policies for token generated for IAM Principal: {}", iamPrincipal, e);
        }

        authToken.setMetadata(ImmutableMap.of("_truncated", "true"));
        authToken.setPolicies(ImmutableSet.of("_truncated"));

        logger.debug(
                "The auth token has length: {} which is > {} KMS cannot encrypt it, truncating auth payload by removing policies and metadata " +
                        "original metadata: {} " +
                        "original policies: {}",
                authResponseJson.length,
                KMS_SIZE_LIMIT,
                originalMetadata,
                originalPolicies
        );

        try {
            return objectMapper.writeValueAsBytes(authToken);
        } catch (JsonProcessingException e) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
                    .withExceptionCause(e)
                    .withExceptionMessage("Failed to write IAM role authentication response as JSON for encrypting.")
                    .build();
        }

    }

    /**
     * Since tokens are immutable, there are certain situations where refreshing the token used by a user is
     * necessary.  Anytime permissions change, this is required to reflect that to the user.
     *
     * @param authPrincipal The principal for the caller
     * @return The auth response directly from Vault with the token and metadata
     */
    public AuthResponse refreshUserToken(final CerberusPrincipal authPrincipal) {

        if (! PrincipalType.USER.equals(authPrincipal.getPrincipalType())) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.USER_ONLY_RESOURCE)
                    .withExceptionMessage("The principal: %s attempted to use the user token refresh method")
                    .build();
        }

        Integer currentTokenRefreshCount = authPrincipal.getTokenRefreshCount();
        if (currentTokenRefreshCount >= maxTokenRefreshCount) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.MAXIMUM_TOKEN_REFRESH_COUNT_REACHED)
                    .withExceptionMessage(String.format("The principal %s attempted to refresh its token but has " +
                            "reached the maximum number of refreshes allowed", authPrincipal.getName()))
                    .build();
        }

        revoke(authPrincipal.getToken());

        final AuthResponse authResponse = new AuthResponse();
        authResponse.setStatus(AuthStatus.SUCCESS);
        final AuthData authData = new AuthData();
        authResponse.setData(authData);
        authData.setUsername(authPrincipal.getName());
        authData.setClientToken(generateToken(authPrincipal.getName(),
                authPrincipal.getUserGroups(),
                currentTokenRefreshCount + 1));

        return authResponse;
    }

    /**
     * @param authToken Auth Token to be revoked
     */
    public void revoke(final String authToken) {
        authTokenService.revokeToken(authToken);
    }

    /**
     * Creates a token request and executes it returning the auth response.
     *
     * @param username The user requesting a token
     * @param userGroups The user's groups
     * @return The auth response directly from Vault with the token and metadata
     */
    private AuthTokenResponse generateToken(final String username, final Set<String> userGroups, int refreshCount) {
        final Map<String, String> meta = Maps.newHashMap();
        meta.put(CerberusPrincipal.METADATA_KEY_USERNAME, username);

        boolean isAdmin = false;
        if (userGroups.contains(this.adminGroup)) {
            isAdmin = true;
        }
        meta.put(METADATA_KEY_IS_ADMIN, String.valueOf(isAdmin));
        meta.put(CerberusPrincipal.METADATA_KEY_GROUPS, StringUtils.join(userGroups, ','));
        meta.put(CerberusPrincipal.METADATA_KEY_TOKEN_REFRESH_COUNT, String.valueOf(refreshCount));
        meta.put(CerberusPrincipal.METADATA_KEY_MAX_TOKEN_REFRESH_COUNT, String.valueOf(maxTokenRefreshCount));

        final Set<String> policies = buildPolicySet(userGroups);

        return createToken(username, PrincipalType.USER, policies, meta, userTokenTTL);
    }

    /**
     * Builds the policy set to be associated with the to-be generated Vault token.  The lookup-self policy is
     * included by default.  All other associated policies are based on the groups the user is a member of.
     *
     * @param groups Groups the user is a member of
     * @return Set of policies to be associated
     */
    private Set<String> buildPolicySet(final Set<String> groups) {
        final Set<String> policies = Sets.newHashSet(LOOKUP_SELF_POLICY);
        final List<SafeDepositBoxRoleRecord> sdbRoles = safeDepositBoxDao.getUserAssociatedSafeDepositBoxRoles(groups);

        sdbRoles.forEach(i -> {
            policies.add(buildPolicyName(i.getSafeDepositBoxName(), i.getRoleName()));
        });

        return policies;
    }

    /**
     * Builds the policy set with permissions given to the specific IAM principal
     * (e.g. arn:aws:iam::1111111111:instance-profile/example), as well as the base role that is assumed by that
     * principal (i.e. arn:aws:iam::1111111111:role/example)
     * @param iamPrincipalArn - The given IAM principal ARN during authentication
     * @return - List of all policies the given ARN has access to
     */
    protected Set<String> buildCompleteSetOfPolicies(final String iamPrincipalArn) {

        final Set<String> allPolicies = buildPolicySet(iamPrincipalArn);

        if (! awsIamRoleArnParser.isRoleArn(iamPrincipalArn)) {
            logger.debug("Detected non-role ARN, attempting to collect policies for the principal's base role...");
            final String iamPrincipalInRoleFormat = awsIamRoleArnParser.convertPrincipalArnToRoleArn(iamPrincipalArn);

            final Set<String> additionalPolicies = buildPolicySet(iamPrincipalInRoleFormat);
            allPolicies.addAll(additionalPolicies);
        }

        return allPolicies;
    }

    /**
     * Builds the policy set to be associated with the to-be generated Vault token.  The lookup-self policy is
     * included by default.  All other associated policies are based on what permissions are granted to the IAM role.
     *
     * @param iamRoleArn IAM role ARN
     * @return Set of policies to be associated
     */
    private Set<String> buildPolicySet(final String iamRoleArn) {
        final Set<String> policies = Sets.newHashSet(LOOKUP_SELF_POLICY);
        final List<SafeDepositBoxRoleRecord> sdbRoles =
                safeDepositBoxDao.getIamRoleAssociatedSafeDepositBoxRoles(iamRoleArn);

        sdbRoles.forEach(i -> {
            policies.add(buildPolicyName(i.getSafeDepositBoxName(), i.getRoleName()));
        });

        return policies;
    }

    /**
     * Looks up the KMS key id associated with the iam role + region.  If the IAM role exists, but its the first time
     * we've seen the region, we provision a key for usage and return it.
     *
     * @param credentials IAM role credentials
     * @return KMS Key id
     */
    protected String getKeyId(IamPrincipalCredentials credentials) {
        final String iamPrincipalArn = credentials.getIamPrincipalArn();
        final Optional<AwsIamRoleRecord> iamRole = findIamRoleAssociatedWithSdb(iamPrincipalArn);

        if (!iamRole.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.AUTH_IAM_PRINCIPAL_INVALID)
                    .withExceptionMessage(String.format("The role: %s was not configured for any SDB", iamPrincipalArn))
                    .build();
        }

        final Optional<AwsIamRoleKmsKeyRecord> kmsKey = awsIamRoleDao.getKmsKey(iamRole.get().getId(), credentials.getRegion());

        final String kmsKeyId;
        final AwsIamRoleKmsKeyRecord kmsKeyRecord;
        final OffsetDateTime now = dateTimeSupplier.get();

        if (!kmsKey.isPresent()) {
            kmsKeyId = kmsService.provisionKmsKey(iamRole.get().getId(), iamPrincipalArn, credentials.getRegion(), SYSTEM_USER, now);
        } else {
            kmsKeyRecord = kmsKey.get();
            kmsKeyId = kmsKeyRecord.getAwsKmsKeyId();
            kmsService.validateKeyAndPolicy(kmsKeyRecord, iamPrincipalArn);
        }

        return kmsKeyId;
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
                    kmsClient.encrypt(new EncryptRequest().withKeyId(keyId).withPlaintext(ByteBuffer.wrap(data)));

            return encryptResult.getCiphertextBlob().array();
        } catch (AmazonClientException ace) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
                    .withExceptionCause(ace)
                    .withExceptionMessage(
                            String.format("Unexpected error communicating with AWS KMS for region %s.", regionName))
                    .build();
        }
    }

    private Set<String> getAdminRoleArnSet() {
        if (adminRoleArnSet == null) {
            adminRoleArnSet = new HashSet<>();
            if (StringUtils.isNotBlank(adminRoleArns)) {
                String[] roles = adminRoleArns.split(",");
                if (roles.length > 0) {
                    Arrays.stream(roles).forEach(role -> {
                        adminRoleArnSet.add(role.trim());
                    });
                }
            }
        }
        return adminRoleArnSet;
    }

    /**
     * Generate map of Vault token metadata that is common to all principals
     * @param iamPrincipalArn - The authenticating IAM principal ARN
     * @param region - The AWS region
     * @return - Map of token metadata
     */
    protected Map<String, String> generateCommonIamPrincipalAuthMetadata(final String iamPrincipalArn, final String region) {
        Map<String, String> metadata = Maps.newHashMap();
        metadata.put(CerberusPrincipal.METADATA_KEY_AWS_REGION, region);
        metadata.put(CerberusPrincipal.METADATA_KEY_USERNAME, iamPrincipalArn);
        metadata.put(CerberusPrincipal.METADATA_KEY_IS_IAM_PRINCIPAL, Boolean.TRUE.toString());

        Set<String> groups = new HashSet<>();
        groups.add("registered-iam-principals");

        // We will allow specific ARNs access to the user portions of the API
        if (getAdminRoleArnSet().contains(iamPrincipalArn)) {
            metadata.put(METADATA_KEY_IS_ADMIN, Boolean.toString(true));
            groups.add("admin-iam-principals");
        } else {
            metadata.put(METADATA_KEY_IS_ADMIN, Boolean.toString(false));
        }
        metadata.put(CerberusPrincipal.METADATA_KEY_GROUPS, StringUtils.join(groups, ','));

        return metadata;
    }

    /**
     * Search for the given IAM principal (e.g. arn:aws:iam::1111111111:instance-profile/example), if not found, then
     * also search for the base role that the principal assumes (i.e. arn:aws:iam::1111111111:role/example)
     * @param iamPrincipalArn - The authenticating IAM principal ARN
     * @return - The associated IAM role record
     */
    protected Optional<AwsIamRoleRecord> findIamRoleAssociatedWithSdb(final String iamPrincipalArn) {
        Optional<AwsIamRoleRecord> iamRole = awsIamRoleDao.getIamRole(iamPrincipalArn);

        // if the arn is not already in 'role' format, and cannot be found,
        // then try checking for the generic "arn:aws:iam::0000000000:role/foo" format
        if (!iamRole.isPresent() && !awsIamRoleArnParser.isRoleArn(iamPrincipalArn) ) {
            logger.debug("Detected non-role ARN, attempting to find SDBs associated with the principal's base role...");
            final String iamPrincipalInRoleFormat = awsIamRoleArnParser.convertPrincipalArnToRoleArn(iamPrincipalArn);

            iamRole = awsIamRoleDao.getIamRole(iamPrincipalInRoleFormat);
        }

        return iamRole;
    }

    /**
     * Outputs the expected policy name format used in Vault.
     *
     * @param sdbName Safe deposit box name.
     * @param roleName Role for safe deposit box.
     * @return Formatted policy name.
     */
    public String buildPolicyName(final String sdbName, final String roleName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(sdbName), "sdbName cannot be blank!");
        Preconditions.checkArgument(StringUtils.isNotBlank(roleName), "roleName cannot be blank!");

        final StringBuilder sb = new StringBuilder();
        sb.append(slugger.toSlug(sdbName));
        sb.append('-');
        sb.append(StringUtils.lowerCase(roleName));
        return sb.toString();
    }
}
