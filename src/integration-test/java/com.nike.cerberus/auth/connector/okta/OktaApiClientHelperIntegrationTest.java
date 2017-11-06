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

package com.nike.cerberus.auth.connector.okta;

import com.fieldju.commons.EnvUtils;
import com.okta.sdk.clients.AuthApiClient;
import com.okta.sdk.clients.FactorsApiClient;
import com.okta.sdk.clients.UserGroupApiClient;
import com.okta.sdk.framework.ApiClientConfiguration;
import com.okta.sdk.models.usergroups.UserGroup;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class OktaApiClientHelperIntegrationTest {

    AuthApiClient authClient;
    UserGroupApiClient userApiClient;
    FactorsApiClient factorsApiClient;

    OktaApiClientHelper oktaApiClientHelper;

    @Before
    public void before() {
        String oktaApiKey = EnvUtils.getRequiredEnv("OKTA_API_KEY", "The okta api key");
        String oktaApiUrl = EnvUtils.getRequiredEnv("OKTA_API_URL", "The okta api url");

        final ApiClientConfiguration clientConfiguration = new ApiClientConfiguration(oktaApiUrl, oktaApiKey);

        authClient = spy(new AuthApiClient(clientConfiguration));
        userApiClient = spy(new UserGroupApiClient(clientConfiguration));
        factorsApiClient = spy(new FactorsApiClient(clientConfiguration));

        oktaApiClientHelper = new OktaApiClientHelper(authClient, userApiClient, factorsApiClient, oktaApiUrl);
    }


    @Test
    public void test_that_getUserGroups_can_handle_pagination() throws IOException {
        String userIdForUserWithMoreThan1Group = EnvUtils.getRequiredEnv("OKTA_USER",
                "Provide a user id for a user with more than 1 groups");

        OktaApiClientHelper oktaApiClientHelperSpy = spy(oktaApiClientHelper);

        List<UserGroup> userGroups = oktaApiClientHelperSpy.getUserGroups(userIdForUserWithMoreThan1Group, 1);
        assertTrue("The user group list should have more than 1 group in it", userGroups.size() > 1);
        verify(userApiClient, atLeast(2)).getUserGroupsPagedResultsByUrl(anyString());
    }

}
