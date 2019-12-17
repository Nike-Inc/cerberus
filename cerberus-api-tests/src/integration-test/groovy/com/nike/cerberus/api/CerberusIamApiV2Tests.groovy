package com.nike.cerberus.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fieldju.commons.PropUtils
import com.nike.cerberus.api.util.TestUtils
import org.apache.commons.lang3.StringUtils
import org.codehaus.groovy.util.StringUtil
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
    private String cerberusAuthToken
    private def cerberusAuthData

    private ObjectMapper mapper

    @BeforeTest
    void beforeTest() throws NoSuchAlgorithmException {
        mapper = new ObjectMapper()
        TestUtils.configureRestAssured()
        loadRequiredEnvVars()
        cerberusAuthData = retrieveIamAuthToken("arn:aws:iam::$accountId:role/$roleName", region)
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

    @Test
    void "test that an authenticated IAM role can create, read, update then delete a safe deposit box v1"() {
        "v1 create, read, list, update and then delete a safe deposit box"(cerberusAuthData)
    }

    @Test
    void "test that an authenticated IAM role can create, read, update then delete a safe deposit box v2"() {
        "v2 create, read, list, update and then delete a safe deposit box"(cerberusAuthData)
    }

    @Test
    void "test that an SDB can be created with two IAM roles in permissions"() {
        String iamAuthToken = cerberusAuthData.client_token
        String sdbCategoryId = getCategoryMap(iamAuthToken).Applications
        String sdbDescription = generateRandomSdbDescription()
        String ownerRoleId = getRoleMap(iamAuthToken).owner
        String iamPrincipalArn = "arn:aws:iam::$accountId:role/$roleName"
        def iamPrincipalPermissions = [
                ["iam_principal_arn": iamPrincipalArn, "role_id": ownerRoleId],
                ["iam_principal_arn": "arn:aws:iam::1111111111:role/fake-api-test-role", "role_id": ownerRoleId],
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
        String accountRootArn = "arn:aws:iam::$accountId:root"
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
        String accountRootArn = "arn:aws:iam::$accountId:root"
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
