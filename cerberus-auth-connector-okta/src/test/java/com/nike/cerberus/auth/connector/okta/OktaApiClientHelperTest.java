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

package com.nike.cerberus.auth.connector.okta;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.nike.backstopper.exception.ApiException;
import com.okta.sdk.clients.UserGroupApiClient;
import com.okta.sdk.framework.PagedResults;
import com.okta.sdk.models.usergroups.UserGroup;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/** Tests the OktaApiClientHelper class */
public class OktaApiClientHelperTest {

  // class under test
  private OktaApiClientHelper oktaApiClientHelper;

  // dependencies
  @Mock private UserGroupApiClient userGroupApiClient;

  @Before
  public void setup() {

    initMocks(this);

    // create test object
    this.oktaApiClientHelper = new OktaApiClientHelper(userGroupApiClient, "");
  }

  /////////////////////////
  // Test Methods
  /////////////////////////
  @Test
  public void OktaApiClientHelper() {
    OktaConfigurationProperties oktaConfigurationProperties =
        mock(OktaConfigurationProperties.class);
    when(oktaConfigurationProperties.getBaseUrl()).thenReturn("url");
    when(oktaConfigurationProperties.getApiKey()).thenReturn("api_key");
    this.oktaApiClientHelper = new OktaApiClientHelper(oktaConfigurationProperties);
    assertNotNull(oktaApiClientHelper);
    assertNotNull(oktaConfigurationProperties.getBaseUrl());
    assertNotNull(oktaConfigurationProperties.getApiKey());
  }

  @Test
  public void getUserGroupsHappy() throws Exception {

    String id = "id";
    UserGroup group = mock(UserGroup.class);
    PagedResults res = mock(PagedResults.class);
    when(res.getResult()).thenReturn(Lists.newArrayList(group));
    when(res.isLastPage()).thenReturn(true);
    when(userGroupApiClient.getUserGroupsPagedResultsByUrl(anyString())).thenReturn(res);

    // do the call
    List<UserGroup> result = this.oktaApiClientHelper.getUserGroups(id);

    // verify results
    assertTrue(result.contains(group));
  }

  @Test
  public void getUserGroupsWithLimit() throws Exception {

    String id = "id";
    UserGroup group = mock(UserGroup.class);
    PagedResults res = mock(PagedResults.class);
    when(res.getResult()).thenReturn(Lists.newArrayList(group));
    when(res.isLastPage()).thenReturn(true);
    when(userGroupApiClient.getUserGroupsPagedResultsByUrl(anyString())).thenReturn(res);

    // do the call
    List<UserGroup> result = this.oktaApiClientHelper.getUserGroups(id, 1);

    // verify results
    assertTrue(result.contains(group));
  }

  @Test(expected = ApiException.class)
  public void getUserGroupsFails() throws Exception {

    when(userGroupApiClient.getUserGroupsPagedResultsByUrl(anyString()))
        .thenThrow(IOException.class);

    // do the call
    this.oktaApiClientHelper.getUserGroups("id");
  }
}
