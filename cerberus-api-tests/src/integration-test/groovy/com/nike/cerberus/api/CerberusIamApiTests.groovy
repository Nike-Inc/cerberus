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
        "v1 create, read, list, update and then delete a safe deposit box"(cerberusAuthData)
    }

    @Test
    void "test that an authenticated IAM role can create, read, update then delete a safe deposit box v2"() {
        "v2 create, read, list, update and then delete a safe deposit box"(cerberusAuthData)
    }

    @Test
    void "test that an authenticated IAM role can read a preexisting secret"() {
        readSecretNode(PRE_EXISTING_TEST_SECRET_PATH, cerberusAuthToken)
    }
}
