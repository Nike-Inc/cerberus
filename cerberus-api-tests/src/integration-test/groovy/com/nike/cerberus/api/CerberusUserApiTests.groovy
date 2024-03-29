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

import com.nike.cerberus.api.util.TestUtils
import com.nike.cerberus.util.PropUtils
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

import static com.nike.cerberus.api.CerberusCompositeApiActions.*
import static com.nike.cerberus.api.CerberusApiActions.*

class CerberusUserApiTests {

    private String username
    private String password
    private String otpDeviceId
    private String otpSecret
    private String ownerGroup
    private String adGroupNamePrefix
    private String cerberusAuthToken
    private Map cerberusAuthData
    
    @BeforeTest
    void beforeTest() {
        TestUtils.configureRestAssured()
        loadRequiredEnvVars()
        cerberusAuthData = auth()
        cerberusAuthToken = cerberusAuthData.'client_token'
    }

    def auth(retryCount = 0) {
        try {
            Map authResult = "login user with multi factor authentication (or skip mfa if not required) and return auth data"(username, password, otpSecret, otpDeviceId)
            System.out.println("user login successful on try " + retryCount)
            authResult
        } catch (Throwable t) {
            System.err.println("user login failed on try " + retryCount)
            if (retryCount < 3) {
                sleep(10000)
                return auth(retryCount + 1)
            } else {throw t}
        }
    }

    @AfterTest
    void afterTest() {
        logoutUser(cerberusAuthToken)
    }

    @Test (groups = ['deprecated'])
    void "test that an authenticated user can create, read, update then delete a safe deposit box v1"() {
        "v1 create, read, list, update and then delete a safe deposit box"(cerberusAuthData as Map, ownerGroup, adGroupNamePrefix)
    }

    @Test
    void "test that an authenticated user can create, read, update then delete a safe deposit box v2"() {
        "v2 create, read, list, update and then delete a safe deposit box"(cerberusAuthData as Map, ownerGroup, adGroupNamePrefix)
    }

    @Test
    void "test that an authenticated user can create, update then delete a secret node in a safe deposit box"() {
        "create, read, update then delete a secret node"(cerberusAuthToken)
    }

    @Test
    void "test that an authenticated user can create, read, update, then delete a file"() {
        "create, read, update then delete a file"(cerberusAuthToken)
    }

    @Test
    void "test that an authenticated user can read a preexisting secret"() {
        readSecretNode(PRE_EXISTING_TEST_SECRET_PATH, cerberusAuthToken)
    }

    private void loadRequiredEnvVars() {
        username = PropUtils.getRequiredProperty("TEST_USER_EMAIL",
                "The email address for a test user for testing user based endpoints")

        password = PropUtils.getRequiredProperty("TEST_USER_PASSWORD",
                "The password for a test user for testing user based endpoints")

        // todo: make this optional
        otpSecret = PropUtils.getRequiredProperty("TEST_USER_OTP_SECRET",
                "The secret for the test users OTP MFA (OTP == Google auth)")

        otpDeviceId = PropUtils.getRequiredProperty("TEST_USER_OTP_DEVICE_ID",
                "The device id for the test users OTP MFA (OTP == Google auth)")

        ownerGroup = PropUtils.getRequiredProperty("TEST_OWNER_GROUP",
                "The owner group to use when creating an SDB")

        adGroupNamePrefix = PropUtils.getRequiredProperty("AD_GROUP_NAME_PREFIX",
                "AD group name prefix")
    }
}
