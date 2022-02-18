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

import com.nike.cerberus.util.PropUtils
import com.nike.cerberus.api.util.TestUtils
import io.restassured.path.json.JsonPath
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpStatus
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test
import org.testng.collections.Maps

import static com.nike.cerberus.api.CerberusApiActions.*
import static com.nike.cerberus.api.CerberusCompositeApiActions.NEGATIVE_JSON_SCHEMA_ROOT_PATH
import static com.nike.cerberus.api.util.TestUtils.generateRandomSdbDescription
import static com.nike.cerberus.api.util.TestUtils.generateSdbJson
import static com.nike.cerberus.api.util.TestUtils.updateArnWithPartition

class NegativeIamPermissionsApiTests {

    private static final String PERMISSION_DENIED_JSON_SCHEMA = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/permission-denied-invalid-auth-token-error.json"

    private String accountId
    private String roleName
    private String region
    private String iamAuthToken
    private String testPartition
    private String username
    private String password
    private String otpDeviceId
    private String otpSecret
    private String ownerGroup
    private String userAuthToken
    private Map userAuthData

    private Map roleMap

    private def iamPrincipalReadOnlySdb
    private def iamPrincipalWriteOnlySdb


    private void loadRequiredEnvVars() {
        accountId = PropUtils.getRequiredProperty("TEST_ACCOUNT_ID",
                "The account id to use when authenticating with Cerberus using the IAM Auth endpoint")

        roleName = PropUtils.getRequiredProperty("TEST_ROLE_NAME",
                "The role name to use when authenticating with Cerberus using the IAM Auth endpoint")

        region = PropUtils.getRequiredProperty("TEST_REGION",
                "The region to use when authenticating with Cerberus using the IAM Auth endpoint")

        username = PropUtils.getRequiredProperty("TEST_USER_EMAIL",
                "The email address for a test user for testing user based endpoints")

        password = PropUtils.getRequiredProperty("TEST_USER_PASSWORD",
                "The password for a test user for testing user based endpoints")

        ownerGroup = PropUtils.getRequiredProperty("TEST_OWNER_GROUP",
                "The owner group to use when creating an SDB")

        // AWS partition under test
        testPartition = PropUtils.getPropWithDefaultValue("TEST_PARTITION", "aws")

        // todo: make this optional
        otpSecret = PropUtils.getRequiredProperty("TEST_USER_OTP_SECRET",
                "The secret for the test users OTP MFA (OTP == Google auth)")

        otpDeviceId = PropUtils.getRequiredProperty("TEST_USER_OTP_DEVICE_ID",
                "The device id for the test users OTP MFA (OTP == Google auth)")
    }

    @BeforeTest
    void beforeTest() {
        TestUtils.configureRestAssured()
        loadRequiredEnvVars()
        userAuthData = retrieveUserAuthToken(username, password, otpSecret, otpDeviceId)
        userAuthToken = userAuthData."client_token"
        String userGroupOfTestUser = ownerGroup

        String iamPrincipalArn = updateArnWithPartition("arn:$testPartition:iam::${accountId}:role/${roleName}")
        def iamAuthData = retrieveStsToken(region, accountId, roleName)
        iamAuthToken = iamAuthData."client_token"

        String sdbCategoryId = getCategoryMap(userAuthToken).Applications
        String sdbDescription = generateRandomSdbDescription()

        roleMap = getRoleMap(userAuthToken)
        def readOnlyIamPrincipalPermissions = [["iam_principal_arn": iamPrincipalArn, "role_id": roleMap.read]]
        def writeOnlyIamPrincipalPermissions = [["iam_principal_arn": iamPrincipalArn, "role_id": roleMap.read]]
        iamPrincipalReadOnlySdb = createSdbV2(userAuthToken, TestUtils.generateRandomSdbName(), sdbDescription, sdbCategoryId, userGroupOfTestUser, [], readOnlyIamPrincipalPermissions)
        iamPrincipalWriteOnlySdb = createSdbV2(userAuthToken, TestUtils.generateRandomSdbName(), sdbDescription, sdbCategoryId, userGroupOfTestUser, [], writeOnlyIamPrincipalPermissions)
    }

    @AfterTest
    void afterTest() {
        String readOnlyIamPrincipalSdbId = iamPrincipalReadOnlySdb.getString("id")
        deleteSdb(userAuthToken, readOnlyIamPrincipalSdbId, V2_SAFE_DEPOSIT_BOX_PATH)

        String writeOnlyIamPrincipalSdbId = iamPrincipalWriteOnlySdb.getString("id")
        deleteSdb(userAuthToken, writeOnlyIamPrincipalSdbId, V2_SAFE_DEPOSIT_BOX_PATH)

        logoutUser(userAuthToken)
        deleteAuthToken(iamAuthToken)
    }

    @Test
    void "test that an IAM principal cannot be the owner of a SDB"(){
        def sdbId = iamPrincipalReadOnlySdb.getString("id")
        String iamPrincipalArn = updateArnWithPartition("arn:$testPartition:iam::${accountId}:role/${roleName}")

        def updateSdbJson = generateSdbJson(
                iamPrincipalReadOnlySdb.getString("description"),
                iamPrincipalArn,
                iamPrincipalReadOnlySdb.get("user_group_permissions"),
                iamPrincipalReadOnlySdb.get("iam_principal_permissions"))
        def updateSdbRequestUri = "$V2_SAFE_DEPOSIT_BOX_PATH/$sdbId"
        String schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/bad-owner-ad-group.json"

        // update SDB
        validatePUTApiResponse(userAuthToken, updateSdbRequestUri, HttpStatus.SC_BAD_REQUEST, schemaFilePath, updateSdbJson)
    }

    @Test
    void "test that a read IAM principal cannot edit permissions"() {
        def sdbId = iamPrincipalReadOnlySdb.getString("id")
        def roleMap = getRoleMap(userAuthToken)
        String fake_arn = updateArnWithPartition("arn:$testPartition:iam::0011001100:user/obviously-fake-test-user")

        def newIamPrincipalPermissions = [["iam_principal_arn": fake_arn, "role_id": roleMap.owner]]
        def updateSdbJson = generateSdbJson(
                iamPrincipalReadOnlySdb.getString("description"),
                iamPrincipalReadOnlySdb.getString("owner"),
                iamPrincipalReadOnlySdb.get("user_group_permissions"),
                newIamPrincipalPermissions)
        def updateSdbRequestUri = "$V2_SAFE_DEPOSIT_BOX_PATH/$sdbId"
        String schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/ownership-required-permissions-error.json"

        // update SDB
        validatePUTApiResponse(iamAuthToken, updateSdbRequestUri, HttpStatus.SC_FORBIDDEN, schemaFilePath, updateSdbJson)
    }

    @Test
    void "test that a read IAM principal cannot update the SDB owner"() {
        def sdbId = iamPrincipalReadOnlySdb.getString("id")
        def newOwner = "new-owner-group"

        def updateSdbJson = generateSdbJson(
                iamPrincipalReadOnlySdb.getString("description"),
                newOwner,
                iamPrincipalReadOnlySdb.get("user_group_permissions"),
                iamPrincipalReadOnlySdb.get("iam_principal_permissions"))
        def updateSdbRequestUri = "$V2_SAFE_DEPOSIT_BOX_PATH/$sdbId"
        String schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/ownership-required-permissions-error.json"

        // update SDB
        validatePUTApiResponse(iamAuthToken, updateSdbRequestUri, HttpStatus.SC_FORBIDDEN, schemaFilePath, updateSdbJson)
    }

    @Test
    void "test that a read IAM principal cannot write a secret"() {
        String sdbPath = iamPrincipalReadOnlySdb.getString("path")
        sdbPath = StringUtils.substringBeforeLast(sdbPath, "/")

        def writeSecretRequestUri = "$SECRETS_PATH/$sdbPath/${UUID.randomUUID().toString()}"

        // create secret
        validatePOSTApiResponse(iamAuthToken, writeSecretRequestUri, HttpStatus.SC_FORBIDDEN, PERMISSION_DENIED_JSON_SCHEMA, [value: 'value'])
    }

    @Test
    void "test that a read IAM principal cannot delete the SDB V2"() {
        def sdbId = iamPrincipalReadOnlySdb.getString("id")
        def deleteSdbRequestUri = "$V2_SAFE_DEPOSIT_BOX_PATH/$sdbId"

        validateDELETEApiResponse(iamAuthToken, deleteSdbRequestUri, HttpStatus.SC_FORBIDDEN, PERMISSION_DENIED_JSON_SCHEMA)
    }

    @Test
    void "test that a read IAM principal cannot delete the SDB V1"() {
        def sdbId = iamPrincipalReadOnlySdb.getString("id")
        def deleteSdbRequestUri = "$V1_SAFE_DEPOSIT_BOX_PATH/$sdbId"

        validateDELETEApiResponse(iamAuthToken, deleteSdbRequestUri, HttpStatus.SC_FORBIDDEN, PERMISSION_DENIED_JSON_SCHEMA)
    }


    @Test
    void "test that a write IAM principal cannot edit permissions"() {
        def sdbId = iamPrincipalWriteOnlySdb.getString("id")
        def roleMap = getRoleMap(userAuthToken)
        String fake_arn = updateArnWithPartition("arn:$testPartition:iam::0011001100:user/obviously-fake-test-user")

        def newIamPrincipalPermissions = [["iam_principal_arn": fake_arn, "role_id": roleMap.owner]]
        def updateSdbJson = generateSdbJson(
                iamPrincipalWriteOnlySdb.getString("description"),
                iamPrincipalWriteOnlySdb.getString("owner"),
                iamPrincipalWriteOnlySdb.get("user_group_permissions"),
                newIamPrincipalPermissions)
        def updateSdbRequestUri = "$V2_SAFE_DEPOSIT_BOX_PATH/$sdbId"
        String schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/ownership-required-permissions-error.json"

        // update SDB
        validatePUTApiResponse(iamAuthToken, updateSdbRequestUri, HttpStatus.SC_FORBIDDEN, schemaFilePath, updateSdbJson)
    }

    @Test
    void "test that a write IAM principal cannot update the SDB owner"() {
        def sdbId = iamPrincipalWriteOnlySdb.getString("id")
        def newOwner = "new-owner-group"

        def updateSdbJson = generateSdbJson(
                iamPrincipalWriteOnlySdb.getString("description"),
                newOwner,
                iamPrincipalWriteOnlySdb.get("user_group_permissions"),
                iamPrincipalWriteOnlySdb.get("iam_principal_permissions"))
        def updateSdbRequestUri = "$V2_SAFE_DEPOSIT_BOX_PATH/$sdbId"
        String schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/ownership-required-permissions-error.json"

        // update SDB
        validatePUTApiResponse(iamAuthToken, updateSdbRequestUri, HttpStatus.SC_FORBIDDEN, schemaFilePath, updateSdbJson)
    }

    @Test
    void "test that a write IAM principal cannot delete the SDB V1"() {
        def sdbId = iamPrincipalWriteOnlySdb.getString("id")
        def deleteSdbRequestUri = "$V1_SAFE_DEPOSIT_BOX_PATH/$sdbId"

        validateDELETEApiResponse(iamAuthToken, deleteSdbRequestUri, HttpStatus.SC_FORBIDDEN, PERMISSION_DENIED_JSON_SCHEMA)
        System.out.println("After write tries to delete SDB")
    }

    @Test
    void "test that a write IAM principal cannot delete the SDB V2"() {
        def sdbId = iamPrincipalWriteOnlySdb.getString("id")
        def deleteSdbRequestUri = "$V2_SAFE_DEPOSIT_BOX_PATH/$sdbId"

        validateDELETEApiResponse(iamAuthToken, deleteSdbRequestUri, HttpStatus.SC_FORBIDDEN, PERMISSION_DENIED_JSON_SCHEMA)
        System.out.println("After write tries to delete SDB")
    }

    @Test
    void "test that a write IAM principal cannot call refresh endpoint"() {
        validateGETApiResponse(
                AUTH_TOKEN_HEADER_NAME,
                iamAuthToken,
                "v2/auth/user/refresh",
                HttpStatus.SC_FORBIDDEN,
                "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/requested-resource-for-user-principals-only.json")
    }

    @Test
    void "test that a non admin IAM principal cannot call PUT v1 metadata endpoint"() {
        String schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/access-to-requested-resource-is-denied.json"
        // call PUT metadata
        validatePUTApiResponse(iamAuthToken, "v1/metadata", HttpStatus.SC_FORBIDDEN, schemaFilePath, Maps.newHashMap())
    }

    @Test
    void "test that a non admin IAM principal cannot call GET v1 metadata endpoint"() {
        String schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/access-to-requested-resource-is-denied.json"
        // call PUT metadata
        validateGETApiResponse(AUTH_TOKEN_HEADER_NAME, iamAuthToken, "v1/metadata", HttpStatus.SC_FORBIDDEN, schemaFilePath)
    }

    @Test
    void "test that IAM Root ARN permissions do not grant access to an IAM principal from a different account"() {
        String sdbCategoryId = getCategoryMap(iamAuthToken).Applications
        String sdbDescription = generateRandomSdbDescription()
        String ownerRoleId = getRoleMap(iamAuthToken).owner
        String accountRootArn = updateArnWithPartition("arn:$testPartition:iam::00000000:root")
        String automationUserGroup = ownerGroup
        def userPerms = []
        def iamPrincipalPermissions = [
                ["iam_principal_arn": accountRootArn, "role_id": ownerRoleId],
        ]

        // create test sdb
        def testSdb = createSdbV2(iamAuthToken, TestUtils.generateRandomSdbName(), sdbDescription, sdbCategoryId, automationUserGroup, userPerms, iamPrincipalPermissions)
        def testSecretName = "${RandomStringUtils.randomAlphabetic(5,10)} ${RandomStringUtils.randomAlphabetic(5,10)}"

        // create test secret to read
        def secret = ["foo": "bar"]
        def secretPath = "${testSdb.getString("path")}${testSecretName}"
        createOrUpdateSecretNode(secret, secretPath, userAuthToken)

        // test that principal cannot read
        validateGETApiResponse(
                AUTH_TOKEN_HEADER_NAME,
                iamAuthToken,
                "v1/secret/${secretPath}",
                HttpStatus.SC_FORBIDDEN,
                "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/permission-denied-invalid-auth-token-error.json")

        // delete test sdb
        String testSdbId = testSdb.getString("id")
        deleteSdb(userAuthToken, testSdbId, V2_SAFE_DEPOSIT_BOX_PATH)
    }

    @Test
    void "test that IAM Root ARN permissions do not grant access to a IAM principal with permissions to a different SDB"() {
        String sdbCategoryId = getCategoryMap(iamAuthToken).Applications
        String sdbDescription = generateRandomSdbDescription()
        String ownerRoleId = getRoleMap(iamAuthToken).owner
        String accountRootWithNoAccess = updateArnWithPartition("arn:$testPartition:iam::00000000:root")
        String accountRootWithAccess = updateArnWithPartition("arn:$testPartition:iam::$accountId:root")

        String automationUserGroup = ownerGroup
        def userPerms = []
        def iamPermsWithNoAccess = [
                ["iam_principal_arn": accountRootWithNoAccess, "role_id": ownerRoleId],
        ]
        def iamPermsWithAccess = [
                ["iam_principal_arn": accountRootWithAccess, "role_id": ownerRoleId],
        ]

        // create test sdb
        def sdbWithNoAccess = createSdbV2(iamAuthToken, TestUtils.generateRandomSdbName(), sdbDescription, sdbCategoryId, automationUserGroup, userPerms, iamPermsWithNoAccess)
        def sdbWithAccess = createSdbV2(iamAuthToken, TestUtils.generateRandomSdbName(), sdbDescription, sdbCategoryId, automationUserGroup, userPerms, iamPermsWithAccess)

        // create test secret to read
        def secret = ["foo": "bar"]
        def sdbPathWithNoAccess = sdbWithNoAccess.getString("path")
        def secretPathWithNoAccess = "${sdbPathWithNoAccess}${RandomStringUtils.randomAlphabetic(5,10)}"
        createOrUpdateSecretNode(secret, secretPathWithNoAccess, userAuthToken)

        // test that principal cannot read from sdb without access
        validateGETApiResponse(
                AUTH_TOKEN_HEADER_NAME,
                iamAuthToken,
                "v1/secret/${secretPathWithNoAccess}",
                HttpStatus.SC_FORBIDDEN,
                "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/permission-denied-invalid-auth-token-error.json")

        // delete test sdbs
        deleteSdb(userAuthToken, sdbWithNoAccess.getString("id"), V2_SAFE_DEPOSIT_BOX_PATH)
        deleteSdb(iamAuthToken, sdbWithAccess.getString("id"), V2_SAFE_DEPOSIT_BOX_PATH)
    }

    private static Map getRoleMap(String cerberusAuthToken) {
        // Create a map of role ids to names
        JsonPath getRolesResponse = getRoles(cerberusAuthToken)
        def roleMap = [:]
        getRolesResponse.getList("").each { role ->
            roleMap.put role.name, role.id
        }

        return roleMap
    }

    private static Map getCategoryMap(String cerberusAuthToken) {
        // Create a map of category ids to names'
        JsonPath getCategoriesResponse = getCategories(cerberusAuthToken)
        def catMap = [:]
        getCategoriesResponse.getList("").each { category ->
            catMap.put category.display_name, category.id
        }

        return catMap
    }
}
