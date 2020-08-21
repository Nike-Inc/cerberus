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
import org.apache.commons.lang3.StringUtils
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

import java.security.NoSuchAlgorithmException

import static com.nike.cerberus.api.CerberusCompositeApiActions.*
import static com.nike.cerberus.api.CerberusApiActions.*
import static com.nike.cerberus.api.util.TestUtils.generateRandomSdbDescription

class CerberusIamApiV2Tests {

    private String accountId
    private String roleName
    private String region
    private String ownerGroup
    private String cerberusAuthToken
    private def cerberusAuthData

    private final List<String> CHINA_REGIONS = new ArrayList<String>(
        Arrays.asList(
            "cn-north-1",
            "cn-northwest-1")
    );

    private ObjectMapper mapper

    @BeforeTest
    void beforeTest() throws NoSuchAlgorithmException {
        mapper = new ObjectMapper()
        TestUtils.configureRestAssured()
        loadRequiredEnvVars()
        cerberusAuthData = retrieveStsToken(region)
        System.out.println(cerberusAuthData.toString())
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
        "create, read, update then delete a secret node"(cerberusAuthToken)
    }

    @Test
    void "test that an authenticated IAM role can create, read, update, then delete a file"() {
        "create, read, update then delete a file"(cerberusAuthToken)
    }

    @Test
    void "test that an authenticated IAM role can read secret node versions"() {
        'read secret node versions'(cerberusAuthToken)
    }

//    @Test
//    void "test that an authenticated IAM role can create, read, update then delete a safe deposit box v1"() {
//        "v1 create, read, list, update and then delete a safe deposit box"(cerberusAuthData as Map, ownerGroup)
//    }

    @Test
    void "test that an authenticated IAM role can create, read, update then delete a safe deposit box v2"() {
        "v2 create, read, list, update and then delete a safe deposit box"(cerberusAuthData as Map, ownerGroup)
    }

    @Test
    void "test that an SDB can be created with two IAM roles in permissions"() {
        String iamAuthToken = cerberusAuthData.client_token
        String sdbCategoryId = getCategoryMap(iamAuthToken).Applications
        String sdbDescription = generateRandomSdbDescription()
        String ownerRoleId = getRoleMap(iamAuthToken).owner
        String iamPrincipalArn
        String secondArn
        if (CHINA_REGIONS.contains(region)) {
            iamPrincipalArn = "arn:aws-cn:iam::$accountId:role/$roleName"
            secondArn = "arn:aws-cn:iam::1111111111:role/fake-api-test-role"
        } else {
            iamPrincipalArn = "arn:aws:iam::$accountId:role/$roleName"
            secondArn = "arn:aws-cn:iam::1111111111:role/fake-api-test-role"
        }
        def iamPrincipalPermissions = [
                ["iam_principal_arn": iamPrincipalArn, "role_id": ownerRoleId],
                ["iam_principal_arn": secondArn, "role_id": ownerRoleId],
        ]

        // create test sdb
        def testSdb = createSdbV2(iamAuthToken, TestUtils.generateRandomSdbName(), sdbDescription, sdbCategoryId, iamPrincipalArn, [], iamPrincipalPermissions)
        // delete test sdb
        String testSdbId = testSdb.getString("id")
        deleteSdb(iamAuthToken, testSdbId, V2_SAFE_DEPOSIT_BOX_PATH)
    }

    @Test
    void "test that IAM Root ARN permissions grant access for an IAM principal from the same account to read, update, and delete secrets"() {
        String iamAuthToken = cerberusAuthData.client_token
        String sdbCategoryId = getCategoryMap(iamAuthToken).Applications
        String sdbDescription = generateRandomSdbDescription()
        String ownerRoleId = getRoleMap(iamAuthToken).owner
        String accountRootArn
        if (CHINA_REGIONS.contains(region)) {
            accountRootArn = "arn:aws-cn:iam::$accountId:root"
        } else {
            accountRootArn = "arn:aws:iam::$accountId:root"
        }
        def userPerms = []
        def iamPrincipalPermissions = [
                ["iam_principal_arn": accountRootArn, "role_id": ownerRoleId],
        ]

        // create test sdb
        def testSdb = createSdbV2(iamAuthToken, TestUtils.generateRandomSdbName(), sdbDescription, sdbCategoryId, "Lst-foo", userPerms, iamPrincipalPermissions)
        def sdbPath = testSdb.getString("path")
        sdbPath = StringUtils.removeEnd(sdbPath, "/")
        "create, read, update then delete a secret node"(iamAuthToken, sdbPath)

        // delete test sdb
        String testSdbId = testSdb.getString("id")
        deleteSdb(iamAuthToken, testSdbId, V2_SAFE_DEPOSIT_BOX_PATH)
    }

    @Test
    void "test that IAM Root ARN permissions grant access for an IAM principal from the same account to read, update, and delete files"() {
        String iamAuthToken = cerberusAuthData.client_token
        String sdbCategoryId = getCategoryMap(iamAuthToken).Applications
        String sdbDescription = generateRandomSdbDescription()
        String ownerRoleId = getRoleMap(iamAuthToken).owner
        String accountRootArn
        if (CHINA_REGIONS.contains(region)) {
            accountRootArn = "arn:aws-cn:iam::$accountId:root"
        } else {
            accountRootArn = "arn:aws:iam::$accountId:root"
        }
        def userPerms = []
        def iamPrincipalPermissions = [
                ["iam_principal_arn": accountRootArn, "role_id": ownerRoleId],
        ]

        // create test sdb
        def testSdb = createSdbV2(iamAuthToken, TestUtils.generateRandomSdbName(), sdbDescription, sdbCategoryId, "Lst-foo", userPerms, iamPrincipalPermissions)
        def sdbPath = testSdb.getString("path")
        sdbPath = StringUtils.removeEnd(sdbPath, "/")
        "create, read, update then delete a file"(iamAuthToken, sdbPath)

        // delete test sdb
        String testSdbId = testSdb.getString("id")
        deleteSdb(iamAuthToken, testSdbId, V2_SAFE_DEPOSIT_BOX_PATH)
    }
}
