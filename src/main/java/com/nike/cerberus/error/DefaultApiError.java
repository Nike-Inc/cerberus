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

package com.nike.cerberus.error;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;

import java.util.Map;
import java.util.UUID;

import static javax.servlet.http.HttpServletResponse.*;

/**
 * Contains the application-specific errors that can occur. Maps these application-specific errors
 * to appropriate Http Status codes, and contains appropriate messages for them. These can be
 * thrown with {@link com.nike.backstopper.exception.ApiException} to garner error-specific
 * information for this project
 */
public enum DefaultApiError implements ApiError {

    /**
     * Required request body is missing.
     */
    REQUEST_BODY_MISSING(99000, "Request body required", SC_BAD_REQUEST),

    /**
     * Request body is malformed.
     */
    REQUEST_BODY_MALFORMED(99001, "Request body malformed.", SC_BAD_REQUEST),

    /**
     * IAM Role account id is malformed.
     */
    IAM_ROLE_ACCT_ID_INVALID(99102, "AWS account id is malformed.", SC_BAD_REQUEST),

    /**
     * IAM Role account id is blank
     */
    AUTH_IAM_ROLE_NAME_INVALID(99103, "AWS IAM role name is invalid.", SC_BAD_REQUEST),

    /**
     * IAM Role account id is blank
     */
    AUTH_IAM_ROLE_AWS_REGION_BLANK(99104, "AWS region is malformed.", SC_BAD_REQUEST),

    /**
     * X-Vault-Token or X-Cerberus-Token header was blank or invalid.
     */
    AUTH_TOKEN_INVALID(99105, "X-Vault-Token or X-Cerberus-Token header is malformed or invalid.", SC_UNAUTHORIZED),

    /**
     * Supplied credentials are invalid.
     */
    AUTH_BAD_CREDENTIALS(99106, "Invalid credentials", SC_UNAUTHORIZED),

    /**
     * Category display name is blank.
     */
    CATEGORY_DISPLAY_NAME_BLANK(99200, "Display name may not be blank.", SC_BAD_REQUEST),

    /**
     * Category path is blank.
     */
    CATEGORY_DISPLAY_NAME_TOO_LONG(99201, "Display name must be 255 characters or less.", SC_BAD_REQUEST),

    /**
     * User group name is blank.
     */
    USER_GROUP_NAME_BLANK(99202, "User group name may not be blank.", SC_BAD_REQUEST),

    /**
     * User group role id is invalid.
     */
    USER_GROUP_ROLE_ID_INVALID(99203, "User group role id is invalid.", SC_BAD_REQUEST),

    /**
     * IAM Role AWS account id is blank.
     */
    IAM_ROLE_ACCT_ID_BLANK(99204, "The AWS account id for the IAM role may not be blank.", SC_BAD_REQUEST),

    /**
     * IAM Role name is blank.
     */
    IAM_ROLE_NAME_INVALID(99205, "The AWS IAM role name is invalid. Alpha-numeric characters and the dash (-) are valid.", SC_BAD_REQUEST),

    /**
     * IAM Role role id is invalid.
     */
    IAM_ROLE_ROLE_ID_INVALID(99206, "The role id for the IAM role is invalid.", SC_BAD_REQUEST),

    /**
     * SDB Category id is invalid.
     */
    SDB_CATEGORY_ID_INVALID(99207, "The category id is invalid.", SC_BAD_REQUEST),

    /**
     * SDB name field is blank.
     */
    SDB_NAME_BLANK(99208, "The name may not be blank.", SC_BAD_REQUEST),

    /**
     * SDB owner field is blank.
     */
    SDB_OWNER_BLANK(99209, "The owner may not be blank.", SC_BAD_REQUEST),

    /**
     * SDB name isn't unique.
     */
    SDB_UNIQUE_NAME(99210, "Duplicate SDB names are not allowed.", SC_BAD_REQUEST),

    /**
     * SDB action requires caller to be the owner.
     */
    SDB_CALLER_OWNERSHIP_REQUIRED(99211, "Ownership required.", SC_FORBIDDEN),

    /**
     * SDB name too long
     */
    SDB_NAME_TOO_LONG(99212, "Name may not exceed 100 characters.", SC_BAD_REQUEST),

    /**
     * SDB description too long
     */
    SDB_DESCRIPTION_TOO_LONG(99213, "Description may not exceed 1000 characters.", SC_BAD_REQUEST),

    /**
     * SDB owner too long
     */
    SDB_OWNER_TOO_LONG(99214, "Owner may not exceed 255 characters.", SC_BAD_REQUEST),

    /**
     * The AWS region specified is invalid.
     */
    AUTH_IAM_ROLE_AWS_REGION_INVALID(99215, "Invalid AWS region.", SC_BAD_REQUEST),

    /**
     * The IAM Role + Region don't have a KMS key provisioned to encrypt the auth response.
     */
    AUTH_IAM_PRINCIPAL_INVALID(99216, "The specified IAM principal is not valid.", SC_BAD_REQUEST),

    /**
     * IAM Role permission on SDB specifies in invalid AWS region.
     */
    SDB_IAM_ROLE_PERMISSION_AWS_REGION_INVALID(99217, "Invalid AWS region specified for the IAM role.", SC_BAD_REQUEST),

    /**
     * IAM Role permission on SDB specifies in invalid AWS region.
     */
    SDB_IAM_ROLE_PERMISSION_IAM_ROLE_INVALID(99226, "Invalid AWS IAM role specified for the SDB.", SC_BAD_REQUEST),

    /**
     * User group permissions contain duplicate entries.
     */
    SDB_USER_GROUP_REPEATED(99218, "The user group permissions contains duplicate entries.", SC_BAD_REQUEST),

    /**
     * IAM role permissions contain duplicate entries.
     */
    SDB_IAM_ROLE_REPEATED(99219, "The IAM role permissions contains duplicate entries.", SC_BAD_REQUEST),

    /**
     * Owner should not be included in the user group permission set.
     */
    SDB_OWNER_IN_USER_GROUP_PERMS(99220, "The owner can not be included in the user group permissions.", SC_BAD_REQUEST),

    /**
     * SDB has too many owners
     */
    SDB_TOO_MANY_OWNERS(99221, "The SDB has more than one owners!", SC_INTERNAL_SERVER_ERROR),

    /**
     * Authentication error for when a user attempts to login and MFA is required but not setup on their account.
     */
    MFA_SETUP_REQUIRED(99222, "MFA is required. Please set up a supported device, either Okta Verify or Google Authenticator.", SC_BAD_REQUEST),

    /**
     * The IAM Role + Region don't have a KMS key provisioned to encrypt the auth response.
     */
    AUTH_IAM_ROLE_REJECTED(99223, "KMS rejected the IAM Role ARN with an InvalidArnException.", SC_INTERNAL_SERVER_ERROR),

    /**
     * The IAM Role + Region don't have a KMS key provisioned to encrypt the auth response.
     */
    INVALID_QUERY_PARAMS(99224, "Invalid query params", SC_BAD_REQUEST),

    /**
     * Failed to validate that the KMS key policy was valid
     */
    FAILED_TO_VALIDATE_KMS_KEY_POLICY(99225, "Failed to validate KMS key policy", SC_INTERNAL_SERVER_ERROR),

    /**
     * IAM Role permission on SDB specifies in invalid AWS region.
     */
    SDB_IAM_PRINCIPAL_PERMISSION_ARN_INVALID(99226, "Invalid AWS IAM role specified for the SDB.", SC_BAD_REQUEST),

    /**
     * IAM role permissions contain duplicate entries.
     */
    SDB_IAM_PRINCIPAL_REPEATED(99227, "The IAM principal permissions contains duplicate entries.", SC_BAD_REQUEST),

    /**
     * IAM Role account id is blank
     */
    AUTH_IAM_PRINCIPAL_AWS_REGION_BLANK(99228, "AWS region is malformed.", SC_BAD_REQUEST),

    /**
     * Invalid region provided in authentication
     */
    AUTHENTICATION_ERROR_INVALID_REGION(99229, "Invalid AWS region provided during authentication.", SC_BAD_REQUEST),

    /**
     * The token has exceeded the amount of times it can be refreshed
     */
    MAXIMUM_TOKEN_REFRESH_COUNT_REACHED(99230, "Maximum token refresh count reached, re-authentication required.", SC_FORBIDDEN),

    /**
     * The token has exceeded the amount of times it can be refreshed
     */
    USER_ONLY_RESOURCE(99231, "The requested resource is for User Principals only.", SC_FORBIDDEN),

    /**
     * KMS key is scheduled for deletion or disabled
     */
    KMS_KEY_IS_SCHEDULED_FOR_DELETION_OR_DISABLED(99232, "KMS key is scheduled for deletion or disabled", SC_INTERNAL_SERVER_ERROR),

    /**
     * Error reading the file contents of the requested dashboard asset
     */
    FAILED_TO_READ_DASHBOARD_ASSET_CONTENT(99233, "The requested dashboard asset file could not be read", SC_NOT_FOUND),

    /**
     * Various methods have switch statements for principal types if a new type gets added and the method doesn't get updated
     * The method can throw this error.
     */
    UNKNOWN_PRINCIPAL_TYPE(99234, "The method wasn't configured for the given principal type", SC_INTERNAL_SERVER_ERROR),

    /**
     * Do not allow secure data objects/maps to be overwritten by files, or vice versa.
     */
    INVALID_SECURE_DATA_TYPE(99235, "Failed to update secret. The new value is of a different type than the current value (e.g. file vs. key/value map).", SC_BAD_REQUEST),

    /**
     * One or more AWS signature headers are missing.
     */
    MISSING_AWS_SIGNATURE_HEADERS(99236, "One or more required headers (x-amz-date, x-amz-security-token, and Authorization) are missing.", SC_BAD_REQUEST),

    /**
     * Signature does not match. Either the request is invalid or the request is signed with invalid region and/or wrong host.
     */
    SIGNATURE_DOES_NOT_MATCH(99237, "Signature does not match. Make sure the request is signed with us-east-1 as region and sts.amazonaws.com as host.", SC_BAD_REQUEST),

    /**
     * AWS token expired.
     */
    EXPIRED_AWS_TOKEN(99238, "The security token included in the request is expired.", SC_UNAUTHORIZED),

    /**
     * Login failed either from incorrect email or password or from a completable future timeout.
     */
    LOGIN_FAILED(99239, "Failed to login. Please confirm email and password and try again.", SC_UNAUTHORIZED),

    /**
     * Failed to wait for Okta Auth Completable Future to complete.
     */
    AUTH_RESPONSE_WAIT_FAILED(99240, "Failed to wait for Okta Auth Response to complete.", SC_UNAUTHORIZED),

    /**
     * Unable to read MFA factor type because it is not one of the standard types.
     */
    FAILED_TO_READ_FACTOR(99241, "Failed to read Okta MFA factor type.", SC_UNAUTHORIZED),

    /**
     * Generic authentication error for when a user attempts to login and is not successful.
     */
    AUTH_FAILED(99242, "MFA is required. Please confirm that you are enrolled in a supported MFA device.", SC_UNAUTHORIZED),

    /**
     * Factor failed to validate.
     */
    FACTOR_VALIDATE_FAILED(99243, "Failed to validate factor. Please try again or try a different factor.", SC_UNAUTHORIZED),

    /**
     * Call to trigger sms or call challenge for OneLogin is not implemented.
     */
    TRIGGER_CHALLENGE_NOT_IMPLEMENTED(99244, "Call to trigger sms or call challenge for OneLogin is not implemented.", SC_METHOD_NOT_ALLOWED),

    /**
     * Generic not found error.
     */
    ENTITY_NOT_FOUND(99996, "Not found", SC_NOT_FOUND),

    /**
     * Unable to fulfill request resulting in service being unavailable.
     */
    SERVICE_UNAVAILABLE(99997, "Service is unavailable at this time.", SC_SERVICE_UNAVAILABLE),

    /**
     * Uncaught errors that leak.
     */
    INTERNAL_SERVER_ERROR(99998, "Internal server error has occurred.", SC_INTERNAL_SERVER_ERROR),

    /**
     * Generic bad requests.  This is useful because the blueprint error handling sucks.
     */
    GENERIC_BAD_REQUEST(99999, "Request will not be completed.", SC_BAD_REQUEST),

    /**
     * If we encounter an error where something expected is not setup correctly, meaning the service is not functional.
     */
    MISCONFIGURED_APP(99995, "The application is not properly configured.", SC_SERVICE_UNAVAILABLE),

    /**
     * If client attempts to access resource it doesn't have access to.
     */
    ACCESS_DENIED(99994, "Access to the requested resource was denied.", SC_FORBIDDEN);

    private final ApiError delegate;

    DefaultApiError(final ApiError delegate) {
        this.delegate = delegate;
    }

    DefaultApiError(final int errorCode,
                    final String message,
                    final int httpStatusCode) {
        this(new ApiErrorBase("delegated-to-enum-wrapper-" + UUID.randomUUID().toString(),
                              errorCode, message, httpStatusCode));
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getErrorCode() {
        return delegate.getErrorCode();
    }

    @Override
    public String getMessage() {
        return delegate.getMessage();
    }

    @Override
    public Map<String, Object> getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public int getHttpStatusCode() {
        return delegate.getHttpStatusCode();
    }

}
