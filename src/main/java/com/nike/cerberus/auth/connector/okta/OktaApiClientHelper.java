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
 *
 */

package com.nike.cerberus.auth.connector.okta;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.okta.sdk.clients.AuthApiClient;
import com.okta.sdk.clients.FactorsApiClient;
import com.okta.sdk.clients.UserGroupApiClient;
import com.okta.sdk.framework.ApiClientConfiguration;
import com.okta.sdk.framework.PagedResults;
import com.okta.sdk.models.auth.AuthResult;
import com.okta.sdk.models.factors.Factor;
import com.okta.sdk.models.usergroups.UserGroup;

import javax.inject.Named;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper methods to authenticate with Okta.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
public class OktaApiClientHelper {

    private final AuthApiClient authClient;

    private final UserGroupApiClient userGroupApiClient;

    private final FactorsApiClient factorsApiClient;

    private final String baseUrl;

    protected OktaApiClientHelper(AuthApiClient authClient,
                                  UserGroupApiClient userGroupApiClient,
                                  FactorsApiClient factorsApiClient,
                                  String baseUrl) {

        this.authClient = authClient;
        this.userGroupApiClient = userGroupApiClient;
        this.factorsApiClient = factorsApiClient;
        this.baseUrl = baseUrl;
    }

    @Inject
    public OktaApiClientHelper(@Named("auth.connector.okta.api_key") String oktaApiKey,
                               @Named("auth.connector.okta.base_url") String baseUrl) {

        Preconditions.checkArgument(oktaApiKey != null, "okta api key cannot be null");
        Preconditions.checkArgument(baseUrl != null, "okta base url cannot be null");

        this.baseUrl = baseUrl;

        final ApiClientConfiguration clientConfiguration = new ApiClientConfiguration(baseUrl, oktaApiKey);
        authClient = new AuthApiClient(clientConfiguration);
        userGroupApiClient = new UserGroupApiClient(clientConfiguration);
        factorsApiClient = new FactorsApiClient(clientConfiguration);
    }

    /**
     * Request to get user group data by the user's ID.
     *
     * @param userId User ID
     * @return User groups
     */
    protected List<UserGroup> getUserGroups(final String userId) {
        return getUserGroups(userId, null);
    }

    /**
     * Request to get user group data by the user's ID.
     *
     * @param userId User ID
     * @param limit The page size limit, if null the default will be used
     * @return User groups
     */
    protected List<UserGroup> getUserGroups(String userId, Integer limit) {
        List<UserGroup> userGroups = new LinkedList<>();
        try {
            boolean hasNext;
            StringBuilder urlBuilder = new StringBuilder(baseUrl)
                    .append("/api/v1/users/").append(userId).append("/groups");

            if (limit != null) {
                urlBuilder.append("?limit=").append(limit);
            }
            String url = urlBuilder.toString();

            do {
                PagedResults<UserGroup> userGroupPagedResults = userGroupApiClient
                        .getUserGroupsPagedResultsByUrl(url);

                userGroups.addAll(userGroupPagedResults.getResult());

                if (! userGroupPagedResults.isLastPage()) {
                    hasNext = true;
                    url = userGroupPagedResults.getNextUrl();
                } else {
                    hasNext = false;
                }
            } while (hasNext);
        } catch (IOException ioe) {
            final String msg = String.format("failed to get user groups for user (%s) for reason: %s", userId,
                    ioe.getMessage());

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST)
                    .withExceptionMessage(msg)
                    .build();
        }
        return userGroups;
    }

    /**
     * Request for verifying a MFA factor.
     *
     * @param factorId MFA factor id
     * @param stateToken State token
     * @param passCode One Time Passcode from MFA factor
     * @return Session login token
     */
    protected AuthResult verifyFactor(final String factorId,
                                      final String stateToken,
                                      final String passCode) {

        final AuthResult authResult;
        try {
            authResult = authClient.authenticateWithFactor(stateToken, factorId, passCode);
        } catch (IOException ioe) {
            final String msg = String.format("stateToken: %s failed to verify 2nd factor for reason: %s",
                    stateToken, ioe.getMessage());

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS)
                    .withExceptionMessage(msg)
                    .build();
        }

        return authResult;
    }

    /**
     * Authenticate a user with Okta
     *
     * @param username   Okta username
     * @param password   Okta password
     * @param relayState Deep link to redirect user to after authentication
     * @return Session login token
     */
    protected AuthResult authenticateUser(final String username, final String password,
                                          final String relayState) {

        try {
            return authClient.authenticate(username, password, relayState);
        } catch (IOException ioe) {
            final String msg = String.format("failed to authenticate user (%s) for reason: %s", username,
                    ioe.getMessage());

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS)
                    .withExceptionMessage(msg)
                    .build();        }
    }

    /**
     * Get list of enrolled MFA factors for a user
     * @param userId  Okta user ID
     * @return List of factors
     */
    protected List<Factor> getFactorsByUserId(final String userId) {

        Preconditions.checkArgument(userId != null, "user id cannot be null.");

        try {
            return factorsApiClient.getUserLifecycleFactors(userId);
        } catch (IOException e) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
                    .withExceptionCause(e)
                    .withExceptionMessage("Error parsing the embedded auth data from Okta.")
                    .build();
        }
    }

}
