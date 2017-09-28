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

import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

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
    REQUEST_BODY_MISSING(99000, "Request body required", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * Request body is malformed.
     */
    REQUEST_BODY_MALFORMED(99001, "Request body malformed.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * IAM Role account id is malformed.
     */
    IAM_ROLE_ACCT_ID_INVALID(99102, "AWS account id is malformed.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * IAM Role account id is blank
     */
    AUTH_IAM_ROLE_NAME_INVALID(99103, "AWS IAM role name is invalid.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * IAM Role account id is blank
     */
    AUTH_IAM_ROLE_AWS_REGION_BLANK(99104, "AWS region is malformed.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * X-Vault-Token header is blank.
     */
    AUTH_VAULT_TOKEN_INVALID(99105, "X-Vault-Token or X-Cerberus-Token header is malformed.", HttpServletResponse.SC_UNAUTHORIZED),

    /**
     * Supplied credentials are invalid.
     */
    AUTH_BAD_CREDENTIALS(99106, "Invalid credentials", HttpServletResponse.SC_UNAUTHORIZED),

    /**
     * Category display name is blank.
     */
    CATEGORY_DISPLAY_NAME_BLANK(99200, "Display name may not be blank.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * Category path is blank.
     */
    CATEGORY_DISPLAY_NAME_TOO_LONG(99201, "Display name must be 255 characters or less.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * User group name is blank.
     */
    USER_GROUP_NAME_BLANK(99202, "User group name may not be blank.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * User group role id is invalid.
     */
    USER_GROUP_ROLE_ID_INVALID(99203, "User group role id is invalid.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * IAM Role AWS account id is blank.
     */
    IAM_ROLE_ACCT_ID_BLANK(99204, "The AWS account id for the IAM role may not be blank.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * IAM Role name is blank.
     */
    IAM_ROLE_NAME_INVALID(99205, "The AWS IAM role name is invalid. Alpha-numeric characters and the dash (-) are valid.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * IAM Role role id is invalid.
     */
    IAM_ROLE_ROLE_ID_INVALID(99206, "The role id for the IAM role is invalid.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * SDB Category id is invalid.
     */
    SDB_CATEGORY_ID_INVALID(99207, "The category id is invalid.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * SDB name field is blank.
     */
    SDB_NAME_BLANK(99208, "The name may not be blank.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * SDB owner field is blank.
     */
    SDB_OWNER_BLANK(99209, "The owner may not be blank.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * SDB name isn't unique.
     */
    SDB_UNIQUE_NAME(99210, "Duplicate SDB names are not allowed.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * SDB action requires caller to be the owner.
     */
    SDB_CALLER_OWNERSHIP_REQUIRED(99211, "Ownership required.", HttpServletResponse.SC_FORBIDDEN),

    /**
     * SDB name too long
     */
    SDB_NAME_TOO_LONG(99212, "Name may not exceed 100 characters.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * SDB description too long
     */
    SDB_DESCRIPTION_TOO_LONG(99213, "Description may not exceed 1000 characters.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * SDB owner too long
     */
    SDB_OWNER_TOO_LONG(99214, "Owner may not exceed 255 characters.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * The AWS region specified is invalid.
     */
    AUTH_IAM_ROLE_AWS_REGION_INVALID(99215, "Invalid AWS region.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * The IAM Role + Region don't have a KMS key provisioned to encrypt the auth response.
     */
    AUTH_IAM_PRINCIPAL_INVALID(99216, "The specified IAM principal is not valid.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * IAM Role permission on SDB specifies in invalid AWS region.
     */
    SDB_IAM_ROLE_PERMISSION_AWS_REGION_INVALID(99217, "Invalid AWS region specified for the IAM role.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * IAM Role permission on SDB specifies in invalid AWS region.
     */
    SDB_IAM_ROLE_PERMISSION_IAM_ROLE_INVALID(99226, "Invalid AWS IAM role specified for the SDB.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * User group permissions contain duplicate entries.
     */
    SDB_USER_GROUP_REPEATED(99218, "The user group permissions contains duplicate entries.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * IAM role permissions contain duplicate entries.
     */
    SDB_IAM_ROLE_REPEATED(99219, "The IAM role permissions contains duplicate entries.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * Owner should not be included in the user group permission set.
     */
    SDB_OWNER_IN_USER_GROUP_PERMS(99220, "The owner can not be included in the user group permissions.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * SDB has too many owners
     */
    SDB_TOO_MANY_OWNERS(99221, "The SDB has more than one owners!", HttpServletResponse.SC_INTERNAL_SERVER_ERROR),

    /**
     * Authentication error for when a user attempts to login and MFA is required but not setup on their account.
     */
    MFA_SETUP_REQUIRED(99222, "MFA is required but the user has not set up any factors.", HttpServletResponse.SC_BAD_REQUEST),


    /**
     * The IAM Role + Region don't have a KMS key provisioned to encrypt the auth response.
     */
    AUTH_IAM_ROLE_REJECTED(99223, "KMS rejected the IAM Role ARN with an InvalidArnException.", HttpServletResponse.SC_INTERNAL_SERVER_ERROR),

    /**
     * The IAM Role + Region don't have a KMS key provisioned to encrypt the auth response.
     */
    INVALID_QUERY_PARAMS(99224, "Invalid query params", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * Failed to validate that the KMS key policy was valid
     */
    FAILED_TO_VALIDATE_KMS_KEY_POLICY(99225, "Failed to validate KMS key policy", HttpServletResponse.SC_INTERNAL_SERVER_ERROR),

    /**
     * IAM Role permission on SDB specifies in invalid AWS region.
     */
    SDB_IAM_PRINCIPAL_PERMISSION_ARN_INVALID(99226, "Invalid AWS IAM role specified for the SDB.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * IAM role permissions contain duplicate entries.
     */
    SDB_IAM_PRINCIPAL_REPEATED(99227, "The IAM principal permissions contains duplicate entries.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * IAM Role account id is blank
     */
    AUTH_IAM_PRINCIPAL_AWS_REGION_BLANK(99228, "AWS region is malformed.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * Invalid region provided in authentication
     */
    AUTHENTICATION_ERROR_INVALID_REGION(99229, "Invalid AWS region provided during authentication.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * The token has exceeded the amount of times it can be refreshed
     */
    MAXIMUM_TOKEN_REFRESH_COUNT_REACHED(99230, "Maximum token refresh count reached, re-authentication required.", HttpServletResponse.SC_FORBIDDEN),

    /**
     * The token has exceeded the amount of times it can be refreshed
     */
    USER_ONLY_RESOURCE(99231, "The requested resource is for User Principals only.", HttpServletResponse.SC_FORBIDDEN),

    /**
     * KMS key is scheduled for deletion or disabled
     */
    KMS_KEY_IS_SCHEDULED_FOR_DELETION_OR_DISABLED(99232, "KMS key is scheduled for deletion or disabled", HttpServletResponse.SC_INTERNAL_SERVER_ERROR),

    /**
     * Error reading the file contents of the requested dashboard asset
     */
    FAILED_TO_READ_DASHBOARD_ASSET_CONTENT(99233, "The requested dashboard asset file could not be read", HttpServletResponse.SC_INTERNAL_SERVER_ERROR),

    /**
     * Error reading the file contents of the requested dashboard asset
     */
    NO_SECURE_DATA_AT_GIVEN_PATH(99234, "There is no secure data at the given path", HttpServletResponse.SC_NOT_FOUND),


    /**
     * Generic not found error.
     */
    ENTITY_NOT_FOUND(99996, "Not found", HttpServletResponse.SC_NOT_FOUND),

    /**
     * Unable to fulfill request resulting in service being unavailable.
     */
    SERVICE_UNAVAILABLE(99997, "Service is unavailable at this time.", HttpServletResponse.SC_SERVICE_UNAVAILABLE),

    /**
     * Uncaught errors that leak.
     */
    INTERNAL_SERVER_ERROR(99998, "Internal server error has occurred.", HttpServletResponse.SC_INTERNAL_SERVER_ERROR),

    /**
     * Generic bad requests.  This is useful because the blueprint error handling sucks.
     */
    GENERIC_BAD_REQUEST(99999, "Request will not be completed.", HttpServletResponse.SC_BAD_REQUEST),

    /**
     * If we encounter an error where something expected is not setup correctly, meaning the service is not functional.
     */
    MISCONFIGURED_APP(99995, "The application is not properly configured.", HttpServletResponse.SC_SERVICE_UNAVAILABLE),

    /**
     * If client attempts to access resource it doesn't have access to.
     */
    ACCESS_DENIED(99994, "Access to the requested resource was denied.", HttpServletResponse.SC_FORBIDDEN);

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
