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

import com.amazonaws.DefaultRequest
import com.amazonaws.auth.AWS4Signer
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.auth.profile.internal.securitytoken.RoleInfo
import com.amazonaws.auth.profile.internal.securitytoken.STSProfileCredentialsServiceProvider
import com.amazonaws.http.HttpMethodName
import com.amazonaws.regions.Regions
import com.amazonaws.services.kms.AWSKMSClient
import com.amazonaws.services.kms.model.DecryptRequest
import com.amazonaws.services.kms.model.DecryptResult
import com.amazonaws.services.securitytoken.AWSSecurityTokenService
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder
import com.nike.cerberus.util.PropUtils
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import groovy.json.JsonSlurper
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpStatus

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

import static com.nike.cerberus.api.util.TestUtils.updateArnWithPartition
import static io.restassured.RestAssured.*
import static io.restassured.module.jsv.JsonSchemaValidator.*
import static org.hamcrest.Matchers.*
import static org.junit.Assert.*

import static com.nike.cerberus.api.CerberusCompositeApiActions.*

class CerberusApiActions {

    public final static String V1_SAFE_DEPOSIT_BOX_PATH = "v1/safe-deposit-box"
    public final static String V2_SAFE_DEPOSIT_BOX_PATH = "v2/safe-deposit-box"
    public final static String CLEAN_UP_PATH = "/v1/cleanup"
    public final static String SECRETS_PATH = "/v1/secret"
    public final static String IAM_ROLE_AUTH_PATH = "/v1/auth/iam-role"
    public final static String IAM_PRINCIPAL_AUTH_PATH = "/v2/auth/iam-principal"
    public final static String USER_AUTH_PATH = "v2/auth/user"
    public final static String USER_TOKEN_REFRESH_PATH = "v2/auth/user/refresh"
    public final static String SDB_METADATA_PATH = "v1/metadata"
    public final static String AUTH_TOKEN_HEADER_NAME = "X-Vault-Token"
    public final static String USER_CREDENTIALS_HEADER_NAME = "Authorization"
    public final static String SAFE_DEPOSIT_BOX_VERSION_PATHS_PATH = "v1/sdb-secret-version-paths"
    public final static int SLEEP_IN_MILLISECONDS = PropUtils.getPropWithDefaultValue("SLEEP_IN_MILLISECONDS",
            "0").toInteger()

    public final static String testPartition = PropUtils.getPropWithDefaultValue("TEST_PARTITION", "aws")
    static final List<String> CHINA_REGIONS = new ArrayList<String>(
            Arrays.asList(
                    "cn-north-1",
                    "cn-northwest-1")
    )

    /**
     * Use a cache of KMS clients because creating too many kmsCLients causes a performance bottleneck
     */
    private static Cache<Tuple2<String,String>,AWSKMSClient> kmsClientCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build()

    /**
     * Executes a delete on the v1 auth endpoint to trigger a logout / destroy token action
     *
     * @param cerberusAuthToken The token to destroy
     */
    static void deleteAuthToken(String cerberusAuthToken) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .delete("/v1/auth")
        .then()
                .statusCode(204)
    }

    static def retrieveUserAuthToken(username, password, otpSecret, otpDeviceId, retryCount = 0) {
        try {
            Map authResult = "login user with multi factor authentication (or skip mfa if not required) and return auth data"(username, password, otpSecret, otpDeviceId)
            System.out.println("user login successful on try " + retryCount)
            authResult
        } catch (Throwable t) {
            System.err.println("user login failed on try " + retryCount)
            if (retryCount < 3) {
                sleep(10000)
                return retrieveUserAuthToken(username, password, otpSecret, otpDeviceId, retryCount + 1)
            } else {throw t}
        }
    }

    /**
     * Signs request using AWS V4 signing.
     * @param request AWS STS request to sign
     * @param credentials AWS credentials
     */
    static void signRequest(com.amazonaws.Request request, AWSCredentials credentials, String region){

        AWS4Signer signer = new AWS4Signer()
        signer.setRegionName(region)
        signer.setServiceName("sts")
        signer.sign(request, credentials)
    }

    /**
     * Generates and returns signed headers.
     * @return Signed headers
     */
    static Map<String, String> getSignedHeaders(String region, String accountId, String roleName){

        String url = "https://sts." + region + ".amazonaws.com"
        if(CHINA_REGIONS.contains(region)) {
            url += ".cn"
        }

        URI endpoint = null

        def iamPrincipalArn = updateArnWithPartition("arn:$testPartition:iam::$accountId:role/$roleName")
        AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard().withRegion(Regions.fromName(region)).build()
        def credentials = new STSAssumeRoleSessionCredentialsProvider.Builder(iamPrincipalArn, UUID.randomUUID().toString())
                        .withStsClient(stsClient)
                        .build()
                        .getCredentials()

        try {
            endpoint = new URI(url)
        } catch (URISyntaxException e) {
            System.out.println(String.format("URL is not formatted correctly"), e)

        }

        Map<String, List<String>> parameters = new HashMap<>()
        parameters.put("Action", Arrays.asList("GetCallerIdentity"))
        parameters.put("Version", Arrays.asList("2011-06-15"))

        DefaultRequest<String> requestToSign = new DefaultRequest<>("sts")
        requestToSign.setParameters(parameters)
        requestToSign.setHttpMethod(HttpMethodName.POST)
        requestToSign.setEndpoint(endpoint)

        System.out.println(String.format("Signing request with [%s] as host", url))

        signRequest(requestToSign, credentials, region)

        return requestToSign.getHeaders()
    }

    static def retrieveStsToken(String region, String accountId, String roleName) {
        // get the encrypted payload and validate response

        Map<String, String> signedHeaders = getSignedHeaders(region, accountId, roleName)

        Response response =
                given()
                        .headers(signedHeaders)
                        .contentType("application/json")
                        .when()
                        .post("/v2/auth/sts-identity")
                        .then()
                        .statusCode(200)
                        .contentType("application/json")
                        .assertThat().body(matchesJsonSchemaInClasspath("json-schema/v2/auth/sts-auth.json"))
                        .extract().
                        response()

        String jsonString = new String(response.getBody().asString())
        return new JsonSlurper().parseText(jsonString)
    }

    /**
     * Authenticates with Cerberus's IAM auth endpoint get token
     *
     * @param accountId The account id to do iam auth with
     * @param roleName The role name to do iam auth with
     * @param region The region to do iam auth with
     * @return The authentication token
     */
    static def retrieveIamAuthToken(String accountId, String roleName, String region, boolean assumeRole = true) {
        // get the encrypted payload and validate response
        Response response =
                given()
                        .contentType("application/json")
                        .body([
                        'account_id': accountId,
                        'role_name': roleName,
                        'region': region
                ])
                .when()
                        .post("/v1/auth/iam-role")
                .then()
                        .statusCode(200)
                        .contentType("application/json")
                        .assertThat().body(matchesJsonSchemaInClasspath("json-schema/v1/auth/iam-role-encrypted.json"))
                .extract().
                        response()

        // decrypt the payload
        String base64EncodedKmsEncryptedAuthPayload = response.body().jsonPath().getString("auth_data")

        return getDecryptedPayload(String.format("arn:%s:iam::%s:role/%s", testPartition, accountId, roleName), region, base64EncodedKmsEncryptedAuthPayload, assumeRole)
    }

    static def retrieveIamAuthToken(String iamPrincipalArn, String region, boolean assumeRole = true) {
        // get the encrypted payload and validate response
        Response response =
                given()
                        .contentType("application/json")
                        .body([
                        'iam_principal_arn': iamPrincipalArn,
                        'region': region
                ])
                .when()
                        .post("/v2/auth/iam-principal")
                .then()
                        .statusCode(200)
                        .contentType("application/json")
                        .assertThat().body(matchesJsonSchemaInClasspath("json-schema/v2/auth/iam-role-encrypted.json"))
                .extract().
                        response()

        // decrypt the payload
        String base64EncodedKmsEncryptedAuthPayload = response.body().jsonPath().getString("auth_data")
        return getDecryptedPayload(iamPrincipalArn, region, base64EncodedKmsEncryptedAuthPayload, assumeRole)
    }

    static def getDecryptedPayload(String iamPrincipalArn, String region, String base64EncodedKmsEncryptedAuthPayload, boolean assumeRole = true) {
        AWSKMSClient kmsClient = kmsClientCache.getIfPresent(new Tuple2(iamPrincipalArn, region))

        if(kmsClient == null) {
            System.out.println("getDecryptedPayload() kmsClient cache miss, creating kmsClient for " + iamPrincipalArn + " " + region)
            if (assumeRole) {
                kmsClient = new AWSKMSClient(new STSProfileCredentialsServiceProvider(
                        new RoleInfo().withRoleArn(iamPrincipalArn)
                                .withRoleSessionName(UUID.randomUUID().toString()))).withRegion(Regions.fromName(region))
            } else {
                kmsClient = new AWSKMSClient().withRegion(Regions.fromName(region))
            }

            kmsClientCache.put(new Tuple2<String, String>(iamPrincipalArn, region), kmsClient)
        }

        DecryptResult result = kmsClient.decrypt(
                new DecryptRequest()
                        .withCiphertextBlob(
                        ByteBuffer.wrap(Base64.getDecoder().decode(base64EncodedKmsEncryptedAuthPayload)))
        )

        // validate decrypted schema and return auth token
        String jsonString = new String(result.getPlaintext().array())
        assertThat(jsonString, matchesJsonSchemaInClasspath("json-schema/v2/auth/iam-role-decrypted.json"))
        return new JsonSlurper().parseText(jsonString)
    }

    static String decryptAuthTokenAsRoleAndRetrieveToken(String iamPrincipalArn, String region, String base64EncodedKmsEncryptedAuthPayload) {
        return getDecryptedPayload(iamPrincipalArn, region, base64EncodedKmsEncryptedAuthPayload, true)."client_token"
    }

    static void createOrUpdateSecretNode(Map data, String path, String cerberusAuthToken) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
                .body(data)
        .when()
                .post("/v1/secret/${path}")
        .then()
                .statusCode(204)
    }

    static void createOrUpdateFile(byte[] file, String path, String cerberusAuthToken) {
        String filename = StringUtils.substringAfterLast(path, "/")
        given()
                .header("X-Cerberus-Token", cerberusAuthToken)
                .multiPart('file-content', filename, file)
        .when()
                .post("/v1/secure-file/${path}")
        .then()
                .statusCode(204)
    }

    static JsonPath readSecretNode(String path, String cerberusAuthToken) {
        sleep(SLEEP_IN_MILLISECONDS)
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .get("/v1/secret/${path}")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .assertThat().body(matchesJsonSchemaInClasspath("json-schema/v1/secret/get-secret.json"))
        .extract()
                .body().jsonPath()
    }

    static byte[] readSecureFile(String path, String cerberusAuthToken, byte[] expectedFileBytes, String versionId=null) {
        sleep(SLEEP_IN_MILLISECONDS)
        String uri = versionId ? "/v1/secure-file/${path}?versionId=${versionId}" : "/v1/secure-file/${path}"
        given()
                .header("X-Cerberus-Token", cerberusAuthToken)
        .when()
                .get(uri)
        .then()
                .statusCode(200)
                .assertThat().body(equalTo(new String(expectedFileBytes)))
        .extract()
                .body().asByteArray()
    }

    static JsonPath listSecureFileSummaries(String path, String cerberusAuthToken) {
        given()
                .header("X-Cerberus-Token", cerberusAuthToken)
        .when()
                .get("/v1/secure-files/${path}")
        .then()
                .statusCode(200)
                .assertThat().body(matchesJsonSchemaInClasspath("json-schema/v1/secure-file/list-summaries.json"))
        .extract()
                .body().jsonPath()
    }

    static JsonPath getSecretNodeVersionsMetadata(String path, String cerberusAuthToken) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .get("/v1/secret-versions/${path}")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .assertThat().body(matchesJsonSchemaInClasspath("json-schema/v1/secret/get-secret-versions-metadata.json"))
        .extract()
                .body().jsonPath()
    }

    static JsonPath getSdbVersionPaths(String sdbId, String cerberusAuthToken, String baseSdbVersionPathsPath = SAFE_DEPOSIT_BOX_VERSION_PATHS_PATH) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .get("${baseSdbVersionPathsPath}/${sdbId}")
        .then()
                .statusCode(200)
                .contentType("application/json")
        .extract()
                .body().jsonPath()
    }

    static JsonPath readSecretNodeVersion(String path, String versionId, String cerberusAuthToken) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .get("/v1/secret/${path}?versionId=${versionId}")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .assertThat().body(matchesJsonSchemaInClasspath("json-schema/v1/secret/get-secret-version.json"))
        .extract()
                .body().jsonPath()
    }

    static void deleteSecretNode(String path, String cerberusAuthToken) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .delete("/v1/secret/${path}")
        .then()
                .statusCode(204)
    }

    static void deleteSecureFile(String path, String cerberusAuthToken) {
        given()
                .header("X-Cerberus-Token", cerberusAuthToken)
        .when()
                .delete("/v1/secure-file/${path}")
        .then()
                .statusCode(204)
    }

    static void assertThatSecretNodeDoesNotExist(String path, String cerberusAuthToken) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .get("/v1/secret/${path}")
        .then()
                .statusCode(404)
    }

    static void assertThatSecureFileDoesNotExist(String path, String cerberusAuthToken) {
        given()
                .header("X-Cerberus-Token", cerberusAuthToken)
                .when()
                .get("/v1/secure-file/${path}")
                .then()
                .statusCode(404)
    }

    static JsonPath loginUser(String username, String password) {
        def body =
        given()
                .header("Authorization", "Basic ${"$username:$password".bytes.encodeBase64()}")
        .when()
                .get("/v2/auth/user")
        .then()
                .statusCode(200)
                .contentType("application/json")
        .extract()
                .body()

        String status = body.jsonPath().getString("status")
        if (status == "success") {
            assertThat(body.asString(), matchesJsonSchemaInClasspath("json-schema/v2/auth/user-success.json"))
        } else if (status == "mfa_req") {
            assertThat(body.asString(), matchesJsonSchemaInClasspath("json-schema/v2/auth/user-mfa_req.json"))
        } else {
            throw new IllegalStateException("unreconized status from login user: ${status}")
        }
        return body.jsonPath()
    }

    static void logoutUser(String cerberusAuthToken) {
        deleteAuthToken(cerberusAuthToken)
    }

    static JsonPath finishMfaUserAuth(String stateToken, String deviceId, String otpToken) {
        given()
                .contentType("application/json")
                .body([
                    'state_token': stateToken,
                    'device_id': deviceId,
                    'otp_token': otpToken
                ])
        .when()
                .post("/v2/auth/mfa_check")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .assertThat().body(matchesJsonSchemaInClasspath("json-schema/v2/auth/mfa_check.json"))
        .extract().
                body().jsonPath()
    }

    static String createSdbV1(String cerberusAuthToken,
                              String name,
                              String description,
                              String categoryId,
                              String owner,
                              List<Map<String, String>> userGroupPermissions,
                              List<Map<String, String>> iamRolePermissions) {

        given()
                .header("X-Vault-Token", cerberusAuthToken)
                .contentType("application/json")
                .body([
                    name: name,
                    description: description,
                    'category_id': categoryId,
                    owner: owner,
                    'user_group_permissions': userGroupPermissions,
                    'iam_role_permissions': iamRolePermissions
                ])
        .when()
                .post(V1_SAFE_DEPOSIT_BOX_PATH)
        .then()
                .statusCode(201)
                .header('X-Refresh-Token', 'false')
                .header('Location', not(isEmptyOrNullString()))
                .assertThat().body(matchesJsonSchemaInClasspath("json-schema/$V1_SAFE_DEPOSIT_BOX_PATH/create_success.json"))
        .extract().
                body().jsonPath().getString("id")
    }

    static JsonPath createSdbV2(String cerberusAuthToken,
                              String name,
                              String description,
                              String categoryId,
                              String owner,
                              List<Map<String, String>> userGroupPermissions,
                              List<Map<String, String>> iamPrincipalPermissions) {

        given()
            .header("X-Vault-Token", cerberusAuthToken)
            .contentType("application/json")
            .body([
                name: name,
                description: description,
                'category_id': categoryId,
                owner: owner,
                'user_group_permissions': userGroupPermissions,
                'iam_principal_permissions': iamPrincipalPermissions
            ])
            .when()
                .post(V2_SAFE_DEPOSIT_BOX_PATH)
            .then()
                .statusCode(201)
                .header('X-Refresh-Token', 'false')
                .header('Location', not(isEmptyOrNullString()))
                .assertThat().body(matchesJsonSchemaInClasspath("json-schema/$V2_SAFE_DEPOSIT_BOX_PATH/create_success.json"))
            .extract().
                body().jsonPath()
    }

    static JsonPath readSdb(String cerberusAuthToken, String sdbId, String baseSdbApiPath = V1_SAFE_DEPOSIT_BOX_PATH) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .get("${baseSdbApiPath}/${sdbId}")
        .then()
                .statusCode(200)
                .assertThat().body(matchesJsonSchemaInClasspath("json-schema/$baseSdbApiPath/read_success.json"))
        .extract().
                body().jsonPath()
    }

    static void updateSdbV1(String cerberusAuthToken,
                            String sdbId,
                            String description,
                            String owner,
                            List<Map<String, String>> userGroupPermissions,
                            List<Map<String, String>> iamRolePermissions) {

        given()
            .header("X-Vault-Token", cerberusAuthToken)
            .contentType("application/json")
            .body([
                description: description,
                owner: owner,
                'user_group_permissions': userGroupPermissions,
                'iam_role_permissions': iamRolePermissions
            ])
        .when()
            .put("$V1_SAFE_DEPOSIT_BOX_PATH/${sdbId}")
        .then()
            .statusCode(204)
            .header('X-Refresh-Token', 'false')

    }

    static JsonPath updateSdbV2(String cerberusAuthToken,
                            String sdbId,
                            String description,
                            String owner,
                            List<Map<String, String>> userGroupPermissions,
                            List<Map<String, String>> iamPrincipalPermissions) {

        given()
            .header("X-Vault-Token", cerberusAuthToken)
            .contentType("application/json")
            .body([
                description: description,
                owner: owner,
                'user_group_permissions': userGroupPermissions,
                'iam_principal_permissions': iamPrincipalPermissions
            ])
        .when()
            .put("$V2_SAFE_DEPOSIT_BOX_PATH/${sdbId}")
        .then()
            .statusCode(200)
            .header('X-Refresh-Token', 'false')
            .assertThat().body(matchesJsonSchemaInClasspath("json-schema/$V2_SAFE_DEPOSIT_BOX_PATH/read_success.json"))
        .extract().
            body().jsonPath()
    }

    static void deleteSdb(String cerberusAuthToken, String sdbId, String baseSdbApiPath = V1_SAFE_DEPOSIT_BOX_PATH) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
            .when()
                .delete("${baseSdbApiPath}/${sdbId}")
            .then()
                .statusCode(204)
                .header('X-Refresh-Token', 'false')
    }

    static JsonPath getRoles(String cerberusAuthToken) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .get("/v1/role")
        .then()
                .statusCode(200)
                .assertThat().body(matchesJsonSchemaInClasspath("json-schema/v1/role/get_roles_success.json"))
        .extract().
                body().jsonPath()
    }

    static JsonPath getCategories(String cerberusAuthToken) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .get("/v1/category")
        .then()
                .statusCode(200)
                .assertThat().body(matchesJsonSchemaInClasspath("json-schema/v1/category/get_categories_success.json"))
        .extract().
                body().jsonPath()
    }

    static JsonPath listSdbs(String cerberusAuthToken, String baseSdbApiPath = V1_SAFE_DEPOSIT_BOX_PATH) {
        sleep(SLEEP_IN_MILLISECONDS)
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .get(baseSdbApiPath)
        .then()
                .statusCode(200)
                .assertThat().body(matchesJsonSchemaInClasspath("json-schema/v1/safe-deposit-box/list_success.json"))
        .extract().
                body().jsonPath()
    }

    static String getSdbIdByPath(String pathToSearch, String cerberusAuthToken) {
        def sdbList = listSdbs(cerberusAuthToken).get()
        for(sdb in sdbList){
            def path = sdb.get('path')
            // When the SDB path contains trailing slash
            if(path.endsWith('/')){
                path = path.take(path.length() - 1)
            }
            if(pathToSearch == path) {
                return sdb.get('id')
            }
        }
        return null
    }

    static void cleanUpOrphanedAndInactiveRecords(String cerberusAuthToken, Integer expirationPeriodInDays = null) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
                .body([
                    kms_expiration_period_in_days: expirationPeriodInDays
                ])
        .when()
                .put(CLEAN_UP_PATH)
        .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
    }

    static void getSdbMetadata(String cerberusAuthToken) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .get(SDB_METADATA_PATH)
        .then()
                .statusCode(HttpStatus.SC_OK)
    }

    static void loadDashboardIndexHtml(String partialUriPath) {
        given().when()
            .get(partialUriPath)
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body(stringContainsInOrder(["<html lang=\"en\">", "Cerberus", "</html>"]))
    }


    static refreshUserAuthToken(String cerberusAuthToken) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .get(USER_TOKEN_REFRESH_PATH)
        .then()
                .statusCode(200)
        .extract().
                body().jsonPath()
    }

    static void validateGETApiResponse(String headerName, String headerValue, String requestPath, int statusCode, String pathToJsonSchemaFile) {
        given()
                .header(headerName, headerValue)
        .when()
                .get(requestPath)
        .then()
                .statusCode(statusCode)
                .assertThat().body(matchesJsonSchemaInClasspath(pathToJsonSchemaFile))
    }

    static void validatePUTApiResponse(String cerberusAuthToken, String requestPath, int statusCode, String pathToJsonSchemaFile, Map body) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
                .header("Content-Type", "application/json")
                .body(body)
        .when()
                .put(requestPath)
        .then()
                .statusCode(statusCode)
                .assertThat().body(matchesJsonSchemaInClasspath(pathToJsonSchemaFile))
    }

    static void validatePOSTApiResponse(String cerberusAuthToken, String requestPath, int statusCode, String pathToJsonSchemaFile, Map body) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
                .header("Content-Type", "application/json")
                .body(body)
        .when()
                .post(requestPath)
        .then()
                .statusCode(statusCode)
                .assertThat().body(matchesJsonSchemaInClasspath(pathToJsonSchemaFile))
    }

    static void validateDELETEApiResponse(String cerberusAuthToken, String requestPath, int statusCode, String pathToJsonSchemaFile) {
        given()
                .header("X-Vault-Token", cerberusAuthToken)
        .when()
                .delete(requestPath)
        .then()
                .statusCode(statusCode)
                .assertThat().body(matchesJsonSchemaInClasspath(pathToJsonSchemaFile))
    }

    static Map getRoleMap(String cerberusAuthToken) {
        // Create a map of role ids to names
        JsonPath getRolesResponse = getRoles(cerberusAuthToken)
        def roleMap = [:]
        getRolesResponse.getList("").each { role ->
            roleMap.put role.name, role.id
        }

        return roleMap
    }

    
    static Map getCategoryMap(String cerberusAuthToken) {
        // Create a map of category ids to names'
        JsonPath getCategoriesResponse = getCategories(cerberusAuthToken)
        def catMap = [:]
        getCategoriesResponse.getList("").each { category ->
            catMap.put category.display_name, category.id
        }

        return catMap
    }
}
