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
import com.okta.sdk.clients.UserGroupApiClient;
import com.okta.sdk.framework.ApiClientConfiguration;
import com.okta.sdk.framework.PagedResults;
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

    private final UserGroupApiClient userGroupApiClient;

    private final String baseUrl;

    protected OktaApiClientHelper(UserGroupApiClient userGroupApiClient,
                                  String baseUrl) {

        this.userGroupApiClient = userGroupApiClient;
        this.baseUrl = baseUrl;
    }

    @Inject
    public OktaApiClientHelper(@Named("auth.connector.okta.api_key") String oktaApiKey,
                               @Named("auth.connector.okta.base_url") String baseUrl) {

        Preconditions.checkArgument(oktaApiKey != null, "okta api key cannot be null");
        Preconditions.checkArgument(baseUrl != null, "okta base url cannot be null");

        this.baseUrl = baseUrl;

        final ApiClientConfiguration clientConfiguration = new ApiClientConfiguration(baseUrl, oktaApiKey);
        userGroupApiClient = new UserGroupApiClient(clientConfiguration);
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

}
