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
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpStatus
import org.hamcrest.Matchers
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

import static com.nike.cerberus.api.CerberusApiActions.*
import static com.nike.cerberus.api.CerberusCompositeApiActions.NEGATIVE_JSON_SCHEMA_ROOT_PATH
import static com.nike.cerberus.api.util.TestUtils.generateRandomSdbDescription
import static com.nike.cerberus.api.util.TestUtils.updateArnWithPartition
import static io.restassured.RestAssured.given

class ValidationErrorApiTests {

    private String accountId
    private String roleName
    private String region
    private String ownerGroup
    private String iamAuthToken

    private def testSdb

    private void loadRequiredEnvVars() {
        accountId = PropUtils.getRequiredProperty("TEST_ACCOUNT_ID",
                "The account id to use when authenticating with Cerberus using the IAM Auth endpoint")

        roleName = PropUtils.getRequiredProperty("TEST_ROLE_NAME",
                "The role name to use when authenticating with Cerberus using the IAM Auth endpoint")

        ownerGroup = PropUtils.getRequiredProperty("TEST_OWNER_GROUP",
                "The owner group to use when creating an SDB")

        region = PropUtils.getRequiredProperty("TEST_REGION",
                "The region to use when authenticating with Cerberus using the IAM Auth endpoint")
    }

    @BeforeTest
    void beforeTest() {
        TestUtils.configureRestAssured()
        loadRequiredEnvVars()
        String iamPrincipalArn = updateArnWithPartition("arn:aws:iam::${accountId}:role/${roleName}")
        def iamAuthData = retrieveStsToken(region, accountId, roleName)
        iamAuthToken = iamAuthData."client_token"

        String sdbCategoryId = getCategoryMap(iamAuthToken).Applications
        String sdbDescription = generateRandomSdbDescription()
        String ownerRoleId = getRoleMap(iamAuthToken).owner
        def iamPrincipalPermissions = [["iam_principal_arn": iamPrincipalArn, "role_id": ownerRoleId]]

        testSdb = createSdbV2(iamAuthToken, TestUtils.generateRandomSdbName(), sdbDescription, sdbCategoryId, ownerGroup, [], iamPrincipalPermissions)

        // regenerate token to get policy for new SDB
        iamAuthData = retrieveStsToken(region, accountId, roleName)
        iamAuthToken = iamAuthData."client_token"
    }

    @AfterTest
    void afterTest() {
        String testSdbId = testSdb.getString("id")
        deleteSdb(iamAuthToken, testSdbId, V2_SAFE_DEPOSIT_BOX_PATH)

        deleteAuthToken(iamAuthToken)
    }

    @Test
    void "test that list secrets returns an empty list when there are no nodes under path"() {
        String sdbPath = testSdb.getString("path")
        sdbPath = StringUtils.substringBeforeLast(sdbPath, "/")

        def listSecretsUri = "$SECRETS_PATH/$sdbPath/?list=true"

        // read secret
        given()
          .header(AUTH_TOKEN_HEADER_NAME, iamAuthToken)
        .when()
          .get(listSecretsUri)
        .then()
          .statusCode(HttpStatus.SC_OK)
          .body("data.keys", Matchers.empty())
    }

    @Test
    void "test that a 404 is returned when reading a secret that does not exist"() {
        String sdbPath = testSdb.getString("path")
        sdbPath = StringUtils.substringBeforeLast(sdbPath, "/")
        String newSecretName = UUID.randomUUID().toString()

        def readSecretUri = "$SECRETS_PATH/$sdbPath/$newSecretName"
        String schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/secret-not-found-error.json"

        // read secret
        given()
          .header(AUTH_TOKEN_HEADER_NAME, iamAuthToken)
        .when()
          .get(readSecretUri)
        .then()
          .statusCode(HttpStatus.SC_NOT_FOUND)
    }

    @Test
    void "test that a 404 is returned when reading a secret from an SDB that does not exist"() {
        def readSecretUri = "$SECRETS_PATH/sdb-does-not-exist/random-secret-name"

        // read secret
        given()
          .header(AUTH_TOKEN_HEADER_NAME, iamAuthToken)
        .when()
          .get(readSecretUri)
        .then()
          .statusCode(HttpStatus.SC_NOT_FOUND)
    }

    @Test
    void "test that a 400 is returned when creating an SDB with the same name"() {
        String alreadyExistingSdbName = testSdb.getString("name")
        String ownerRoleId = getRoleMap(iamAuthToken).owner
        String sdbCategoryId = getCategoryMap(iamAuthToken).Applications
        String sdbDescription = generateRandomSdbDescription()
        String iamPrincipalArn = updateArnWithPartition("arn:aws:iam::${accountId}:role/${roleName}")
        def iamPrincipalPermissions = [["iam_principal_arn": iamPrincipalArn, "role_id": ownerRoleId]]
        def sdbObject = [
                category_id             : sdbCategoryId,
                name                    : alreadyExistingSdbName,
                description             : sdbDescription,
                owner                   : ownerGroup,
                'user_group_permissions': [],
                'iam_role_permissions'  : iamPrincipalPermissions
        ]

        String schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/create-sdb-with-the-same-name.json"
        // update SDB
        validatePOSTApiResponse(iamAuthToken, V2_SAFE_DEPOSIT_BOX_PATH, HttpStatus.SC_BAD_REQUEST, schemaFilePath, sdbObject)
    }
}
