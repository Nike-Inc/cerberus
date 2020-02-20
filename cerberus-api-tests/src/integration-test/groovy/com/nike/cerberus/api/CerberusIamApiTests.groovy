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

package com.nike.cerberus.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.nike.cerberus.util.PropUtils
import com.nike.cerberus.api.util.TestUtils
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

import java.security.NoSuchAlgorithmException

import static com.nike.cerberus.api.CerberusCompositeApiActions.*
import static com.nike.cerberus.api.CerberusApiActions.*

class CerberusIamApiTests {

    private String accountId
    private String roleName
    private String region
    private String ownerGroup
    private String cerberusAuthToken
    private def cerberusAuthData

    private ObjectMapper mapper

    @BeforeTest
    void beforeTest() throws NoSuchAlgorithmException {
        mapper = new ObjectMapper()
        TestUtils.configureRestAssured()
        loadRequiredEnvVars()
        cerberusAuthData = retrieveIamAuthToken(accountId, roleName, region)
        cerberusAuthToken = cerberusAuthData."client_token"
    }

    @AfterTest
    void afterTest() {
        deleteAuthToken(cerberusAuthToken)
    }

    private void loadRequiredEnvVars() {
        accountId = PropUtils.getRequiredProperty("TEST_ACCOUNT_ID",
                "The account id to use when authenticating with Cerberus using the IAM Auth endpoint")

        roleName = PropUtils.getRequiredProperty("TEST_ROLE_NAME",
                "The role name to use when authenticating with Cerberus using the IAM Auth endpoint")

        region = PropUtils.getRequiredProperty("TEST_REGION",
                "The region to use when authenticating with Cerberus using the IAM Auth endpoint")
        ownerGroup = PropUtils.getRequiredProperty("TEST_OWNER_GROUP",
                "The owner group to use when creating an SDB")
    }

    @Test
    void "test that an authenticated IAM role can create, read, update then delete a secret node"() {
        'create, read, update then delete a secret node'(cerberusAuthToken)
    }

    @Test
    void "test that an authenticated IAM role can create, read, update, then delete a file"() {
        "create, read, update then delete a file"(cerberusAuthToken)
    }

    @Test
    void "test that an authenticated IAM role can read secret node versions"() {
        'read secret node versions'(cerberusAuthToken)
    }

    @Test
    void "test that an authenticated IAM role can create, read, update then delete a safe deposit box v1"() {
        "v1 create, read, list, update and then delete a safe deposit box"(cerberusAuthData as Map, ownerGroup)
    }

    @Test
    void "test that an authenticated IAM role can create, read, update then delete a safe deposit box v2"() {
        "v2 create, read, list, update and then delete a safe deposit box"(cerberusAuthData as Map, ownerGroup)
    }

    @Test
    void "test that an authenticated IAM role can read a preexisting secret"() {
        readSecretNode(PRE_EXISTING_TEST_SECRET_PATH, cerberusAuthToken)
    }
}
