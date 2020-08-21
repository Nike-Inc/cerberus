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
import com.thedeanda.lorem.Lorem
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

import static com.nike.cerberus.api.CerberusApiActions.AUTH_TOKEN_HEADER_NAME
import static com.nike.cerberus.api.CerberusApiActions.IAM_PRINCIPAL_AUTH_PATH
import static com.nike.cerberus.api.CerberusApiActions.IAM_ROLE_AUTH_PATH
import static com.nike.cerberus.api.CerberusApiActions.SECRETS_PATH
import static com.nike.cerberus.api.CerberusApiActions.USER_AUTH_PATH
import static com.nike.cerberus.api.CerberusApiActions.USER_CREDENTIALS_HEADER_NAME
import static com.nike.cerberus.api.CerberusApiActions.V1_SAFE_DEPOSIT_BOX_PATH
import static com.nike.cerberus.api.CerberusApiActions.V2_SAFE_DEPOSIT_BOX_PATH
import static com.nike.cerberus.api.CerberusApiActions.validateDELETEApiResponse
import static com.nike.cerberus.api.CerberusApiActions.validateGETApiResponse
import static com.nike.cerberus.api.CerberusApiActions.validatePOSTApiResponse
import static com.nike.cerberus.api.CerberusApiActions.validatePUTApiResponse
import static com.nike.cerberus.api.CerberusCompositeApiActions.*

class InvalidAuthApiTests {

    static final String INVALID_AUTH_TOKEN_STR = "invalid-auth-token"
    static final FAKE_SECRET_REQUEST_URI_PATH = "$SECRETS_PATH/$ROOT_INTEGRATION_TEST_SDB_PATH/${UUID.randomUUID().toString()}"
    static final V1_FAKE_SDB_PATH = "$V1_SAFE_DEPOSIT_BOX_PATH/0000-0000-0000-0000"
    static final V2_FAKE_SDB_PATH = "$V1_SAFE_DEPOSIT_BOX_PATH/1111-1111-1111-1111"
    static final String FAKE_ACCOUNT_ID = "1111111111"
    static final String FAKE_ROLE_NAME = "fake_role"

    private final List<String> CHINA_REGIONS = new ArrayList<String>(
            Arrays.asList(
                    "cn-north-1",
                    "cn-northwest-1")
    );

    @BeforeTest
    void beforeTest() {
        TestUtils.configureRestAssured()
    }

    @Test
    void "test that a secret cannot be created with an invalid token"() {
        def schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/permission-denied-invalid-auth-token-error.json"

        validatePOSTApiResponse(INVALID_AUTH_TOKEN_STR, FAKE_SECRET_REQUEST_URI_PATH, HttpStatus.SC_UNAUTHORIZED, schemaFilePath, [value: 'value'])
    }

    @Test
    void "test that a secret cannot be read with an invalid token"() {
        def schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/permission-denied-invalid-auth-token-error.json"

        validateGETApiResponse(AUTH_TOKEN_HEADER_NAME, INVALID_AUTH_TOKEN_STR, FAKE_SECRET_REQUEST_URI_PATH, HttpStatus.SC_UNAUTHORIZED, schemaFilePath)
    }

    @Test
    void "test that a secret cannot be listed with an invalid token"() {
        def listSecretsRequestUri = "$SECRETS_PATH/$ROOT_INTEGRATION_TEST_SDB_PATH/?list=true"
        def schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/permission-denied-invalid-auth-token-error.json"

        validateGETApiResponse(AUTH_TOKEN_HEADER_NAME, INVALID_AUTH_TOKEN_STR, listSecretsRequestUri, HttpStatus.SC_UNAUTHORIZED, schemaFilePath)
    }

    @Test
    void "test that a secret cannot be updated with an invalid token"() {
        def schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/permission-denied-invalid-auth-token-error.json"

        validatePOSTApiResponse(INVALID_AUTH_TOKEN_STR, FAKE_SECRET_REQUEST_URI_PATH, HttpStatus.SC_UNAUTHORIZED, schemaFilePath, [value: 'new value'])
    }

    @Test
    void "test that a secret cannot be delete with an invalid token"() {
        def schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/permission-denied-invalid-auth-token-error.json"

        validateDELETEApiResponse(INVALID_AUTH_TOKEN_STR, FAKE_SECRET_REQUEST_URI_PATH, HttpStatus.SC_UNAUTHORIZED, schemaFilePath)
    }

    @Test
    void "test that a v1 safe deposit box cannot be created with an invalid token"() {
        String schemeFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/auth-token-is-malformed-cms-error.json"

        def iamRolePermissions = [["account_id": FAKE_ACCOUNT_ID, "iam_role_name": FAKE_ROLE_NAME, "role_id": "owner"]]
        def sdb = generateSafeDepositBox(iamRolePermissions)

        validatePOSTApiResponse(INVALID_AUTH_TOKEN_STR, V1_SAFE_DEPOSIT_BOX_PATH, HttpStatus.SC_UNAUTHORIZED, schemeFilePath, sdb)
    }

    @Test
    void "test that a v1 safe deposit box cannot be read with an invalid token"() {
        String schemeFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/auth-token-is-malformed-cms-error.json"

        validateGETApiResponse(AUTH_TOKEN_HEADER_NAME, INVALID_AUTH_TOKEN_STR, V1_FAKE_SDB_PATH, HttpStatus.SC_UNAUTHORIZED, schemeFilePath)
    }

    @Test
    void "test that a v1 safe deposit boxes cannot be listed with an invalid token"() {
        String schemeFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/auth-token-is-malformed-cms-error.json"

        validateGETApiResponse(AUTH_TOKEN_HEADER_NAME, INVALID_AUTH_TOKEN_STR, V1_FAKE_SDB_PATH, HttpStatus.SC_UNAUTHORIZED, schemeFilePath)
    }

    @Test
    void "test that a v1 safe deposit box cannot be updated with an invalid token"() {
        String schemeFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/auth-token-is-malformed-cms-error.json"

        def iamRolePermissions = [["account_id": "98989898989", "iam_role_name": "a-fake-role-name", "role_id": "owner"]]
        def sdb = generateSafeDepositBox(iamRolePermissions)

        validatePUTApiResponse(INVALID_AUTH_TOKEN_STR, V1_FAKE_SDB_PATH, HttpStatus.SC_UNAUTHORIZED, schemeFilePath, sdb)
    }

    @Test
    void "test that a v1 safe deposit box cannot be deleted with an invalid token"() {
        String schemeFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/auth-token-is-malformed-cms-error.json"

        validateDELETEApiResponse(INVALID_AUTH_TOKEN_STR, V1_FAKE_SDB_PATH, HttpStatus.SC_UNAUTHORIZED, schemeFilePath)
    }


    @Test
    void "test that a v2 safe deposit box cannot be created with an invalid token"() {
        String schemeFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/auth-token-is-malformed-cms-error.json"

        def iamRolePermissions = [["account_id": FAKE_ACCOUNT_ID, "iam_role_name": FAKE_ROLE_NAME, "role_id": "owner"]]
        def sdb = generateSafeDepositBox(iamRolePermissions)

        validatePOSTApiResponse(INVALID_AUTH_TOKEN_STR, V2_SAFE_DEPOSIT_BOX_PATH, HttpStatus.SC_UNAUTHORIZED, schemeFilePath, sdb)
    }

    @Test
    void "test that a v2 safe deposit box cannot be read with an invalid token"() {
        String schemeFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/auth-token-is-malformed-cms-error.json"

        validateGETApiResponse(AUTH_TOKEN_HEADER_NAME, INVALID_AUTH_TOKEN_STR, V2_FAKE_SDB_PATH, HttpStatus.SC_UNAUTHORIZED, schemeFilePath)
    }

    @Test
    void "test that a v2 safe deposit boxes cannot be listed with an invalid token"() {
        String schemeFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/auth-token-is-malformed-cms-error.json"

        validateGETApiResponse(AUTH_TOKEN_HEADER_NAME, INVALID_AUTH_TOKEN_STR, V2_SAFE_DEPOSIT_BOX_PATH, HttpStatus.SC_UNAUTHORIZED, schemeFilePath)
    }

    @Test
    void "test that a v2 safe deposit box cannot be updated with an invalid token"() {
        String schemeFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/auth-token-is-malformed-cms-error.json"

        def iamRolePermissions = [["account_id": "98989898989", "iam_role_name": "a-fake-role-name", "role_id": "owner"]]
        def sdb = generateSafeDepositBox(iamRolePermissions)

        validatePUTApiResponse(INVALID_AUTH_TOKEN_STR, V2_FAKE_SDB_PATH, HttpStatus.SC_UNAUTHORIZED, schemeFilePath, sdb)
    }

    @Test
    void "test that a v2 safe deposit box cannot be deleted with an invalid token"() {
        String schemeFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/auth-token-is-malformed-cms-error.json"

        validateDELETEApiResponse(INVALID_AUTH_TOKEN_STR, V2_FAKE_SDB_PATH, HttpStatus.SC_UNAUTHORIZED, schemeFilePath)
    }

//    @Test
//    void "an IAM role cannot auth if it does not have permission to any safe deposit box"() {
//        def schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/iam-role-auth-no-permission-to-any-sdb.json"
//        def requestBody = [account_id: "0000000000", role_name: "non-existent-role-name", region: "us-west-2"]
//
//        validatePOSTApiResponse("token not needed", IAM_ROLE_AUTH_PATH, HttpStatus.SC_BAD_REQUEST, schemaFilePath, requestBody)
//    }

    @Test
    void "an IAM principal cannot auth if it does not have permission to any safe deposit box"() {
        def schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/iam-principal-auth-no-permission-to-any-sdb.json"
        String iamPrincipalArn
        if (CHINA_REGIONS.contains(region)) {
            iamPrincipalArn = "arn:aws-cn:iam::$FAKE_ACCOUNT_ID:role/$FAKE_ROLE_NAME"
        } else {
            iamPrincipalArn = "arn:aws:iam::$FAKE_ACCOUNT_ID:role/$FAKE_ROLE_NAME"
        }
        def requestBody = [
                iam_principal_arn: iamPrincipalArn,
                role_name        : "non-existent-role-name",
                region           : "us-west-2"
        ]

        validatePOSTApiResponse("token not needed", IAM_PRINCIPAL_AUTH_PATH, HttpStatus.SC_BAD_REQUEST, schemaFilePath, requestBody)
    }

    @Test
    void "a user cannot authenticate with invalid credentials"() {
        String email = "invalid.user@example.com"
        String password = "tooEZtoGuess"
        def schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/user-auth-invalid-credentials.json"
        def credentialsHeaderValue = "Basic ${"$email:$password".bytes.encodeBase64()}"

        validateGETApiResponse(USER_CREDENTIALS_HEADER_NAME, credentialsHeaderValue, USER_AUTH_PATH, HttpStatus.SC_UNAUTHORIZED, schemaFilePath)
    }

    private static Map generateSafeDepositBox(def iamPermissions) {
        String name = "${RandomStringUtils.randomAlphabetic(5, 10)} ${RandomStringUtils.randomAlphabetic(5, 10)}"
        String description = "${Lorem.getWords(50)}"
        String categoryId = "category id"
        String owner = "user group"

        def userGroupPermissions = [["name": 'foo', "role_id": "read"]]

        return [
                name                  : name,
                description           : description,
                category_id           : categoryId,
                owner                 : owner,
                user_group_permissions: userGroupPermissions,
                iam_role_permissions  : iamPermissions
        ]
    }
}
