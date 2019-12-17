package com.nike.cerberus.api

import com.amazonaws.util.IOUtils
import com.fieldju.commons.PropUtils
import com.thedeanda.lorem.Lorem
import io.restassured.path.json.JsonPath
import org.apache.commons.lang3.RandomStringUtils
import org.jboss.aerogear.security.otp.Totp

import static org.junit.Assert.assertEquals
import static com.nike.cerberus.api.CerberusApiActions.*
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class CerberusCompositeApiActions {
    private CerberusCompositeApiActions() {}

    static final String ROOT_INTEGRATION_TEST_SDB_PATH = "app/cerberus-integration-tests-sdb"
    static final String PRE_EXISTING_TEST_SECRET_PATH = "${ROOT_INTEGRATION_TEST_SDB_PATH}/default-test-secret"
    static final String NEGATIVE_JSON_SCHEMA_ROOT_PATH = "json-schema/negative"

    static void "create, read, update then delete a secret node"(String cerberusAuthToken) {
        "create, read, update then delete a secret node"(cerberusAuthToken, ROOT_INTEGRATION_TEST_SDB_PATH)
    }

    static void "create, read, update then delete a secret node"(String cerberusAuthToken, String sdbPath) {
        def path = "${sdbPath}/${UUID.randomUUID().toString()}"
        String value1 = 'value1'
        String value2 = 'value2'

        // Create the initial secret node
        createOrUpdateSecretNode([value: value1], path, cerberusAuthToken)
        // Read and verify that it was created
        def resp = readSecretNode(path, cerberusAuthToken)
        assertEquals(value1, resp?.'data'?.'value')
        // Update the secret node
        createOrUpdateSecretNode([value: value2], path, cerberusAuthToken)
        // Read that the node was updated
        def resp2 = readSecretNode(path, cerberusAuthToken)
        assertEquals(value2, resp2?.'data'?.'value')
        // Delete the node
        deleteSecretNode(path, cerberusAuthToken)
        // Verify that the node was deleted
        assertThatSecretNodeDoesNotExist(path, cerberusAuthToken)
    }

    static void "create, read, update then delete a file"(String cerberusAuthToken) {
        "create, read, update then delete a file"(cerberusAuthToken, ROOT_INTEGRATION_TEST_SDB_PATH)
    }

    static void "create, read, update then delete a file"(String cerberusAuthToken, String sdbPath) {
        InputStream originalFile = Thread.currentThread().getContextClassLoader().getResourceAsStream("example-file.pem")
        byte[] originalFileBytes = IOUtils.toByteArray(originalFile);

        InputStream updatedFile = Thread.currentThread().getContextClassLoader().getResourceAsStream("updated-file.pem")
        byte[] updatedFileBytes = IOUtils.toByteArray(updatedFile);
        def path = "${sdbPath}/${UUID.randomUUID().toString().substring(0, 8) + ".pem"}"

        // Create the initial secret node
        createOrUpdateFile(originalFileBytes, path, cerberusAuthToken)
        // Read and verify that it was created
        readSecureFile(path, cerberusAuthToken, originalFileBytes)
        // Update the secret node
        createOrUpdateFile(updatedFileBytes, path, cerberusAuthToken)
        // Read that the node was updated
        readSecureFile(path, cerberusAuthToken, updatedFileBytes)
        // List files in SDB
        listSecureFileSummaries("${ROOT_INTEGRATION_TEST_SDB_PATH}/", cerberusAuthToken)
        // List file versions
        def fileVersionsResponse = getSecretNodeVersionsMetadata(path, cerberusAuthToken)
        assertEquals(fileVersionsResponse?.'total_version_count', 2)
        // Read original file using version ID
        def originalFileVersionId = fileVersionsResponse?.secure_data_version_summaries[1]?.'id'
        readSecureFile(path, cerberusAuthToken, originalFileBytes, originalFileVersionId)
        // Delete the node
        deleteSecureFile(path, cerberusAuthToken)
        // Verify that the node was deleted
        assertThatSecureFileDoesNotExist(path, cerberusAuthToken)

        originalFile.close()
        updatedFile.close()
    }

    static void "read secret node versions"(String cerberusAuthToken) {
        def path = "${ROOT_INTEGRATION_TEST_SDB_PATH}/${UUID.randomUUID().toString()}"
        String value1 = 'value1'
        String value2 = 'value2'

        // Create the initial secret node
        createOrUpdateSecretNode([value: value1], path, cerberusAuthToken)
        // Update the secret node
        createOrUpdateSecretNode([value: value2], path, cerberusAuthToken)

        // The versions are sorted by timestamp. Automated test executes very quickly, causing the 'update' and
        // 'delete' timestamp to be the same which causes an issue with sorting. Sleep is used to ensure correct order.
        sleep(1000)
        // Delete the node
        deleteSecretNode(path, cerberusAuthToken)
        // Verify that the node was deleted
        assertThatSecretNodeDoesNotExist(path, cerberusAuthToken)

        def metadataResp = getSecretNodeVersionsMetadata(path, cerberusAuthToken)
        assertEquals(metadataResp?.'total_version_count', 2)
        // Read the oldest version
        def versionId1 = metadataResp?.'secure_data_version_summaries'[1]?.'id'
        def secretResp1 = readSecretNodeVersion(path, versionId1, cerberusAuthToken)
        assertEquals(value1, secretResp1?.'data'?.'value')
        // Read the second oldest version
        def versionId2 = metadataResp?.'secure_data_version_summaries'[0]?.'id'
        def secretResp2 = readSecretNodeVersion(path, versionId2, cerberusAuthToken)
        assertEquals(value2, secretResp2?.'data'?.'value')
        def sdbId = getSdbIdByPath(ROOT_INTEGRATION_TEST_SDB_PATH, cerberusAuthToken)
        assertNotNull(sdbId)
        // Verify that this secret node is in the list of changed secret nodes
        def sdbVersions = getSdbVersionPaths(sdbId, cerberusAuthToken)
        def list = sdbVersions.get()
        assertTrue(list.contains(path.toString()))
    }

    static void "v1 create, read, list, update and then delete a safe deposit box"(Map cerberusAuthPayloadData) {
        String accountId = PropUtils.getPropWithDefaultValue("TEST_ACCOUNT_ID", "1111111111")
        String roleName = PropUtils.getPropWithDefaultValue("TEST_ROLE_NAME", "fake_role")
        String cerberusAuthToken = cerberusAuthPayloadData.'client_token'
        String groups = cerberusAuthPayloadData.metadata.groups
        def group = groups.split(/,/)[0]

        // Create a map of category ids to names'
        JsonPath getCategoriesResponse = getCategories(cerberusAuthToken)
        def catMap = [:]
        getCategoriesResponse.getList("").each { category ->
            catMap.put category.display_name, category.id
        }
        // Create a map of role ids to names
        JsonPath getRolesResponse = getRoles(cerberusAuthToken)
        def roleMap = [:]
        getRolesResponse.getList("").each { role ->
            roleMap.put role.name, role.id
        }

        String name = "${RandomStringUtils.randomAlphabetic(5,10)} ${RandomStringUtils.randomAlphabetic(5,10)}"
        String description = "${Lorem.getWords(50)}"
        String categoryId = catMap.Applications
        String owner = group
        def userGroupPermissions = [
            [
                "name": 'foo',
                "role_id": roleMap.read
            ]
        ]
        def iamRolePermissions = [
            [
                "account_id": accountId,
                "iam_role_name": roleName,
                "role_id": roleMap.owner
            ]
        ]

        def sdbId = createSdbV1(cerberusAuthToken, name, description, categoryId, owner, userGroupPermissions, iamRolePermissions)
        JsonPath sdb = readSdb(cerberusAuthToken, sdbId, V1_SAFE_DEPOSIT_BOX_PATH)

        // verify that the sdb we created contains the data we expect
        assertSafeDepositBoxV1HasFields(sdb, name, description, categoryId, owner, userGroupPermissions, iamRolePermissions)

        // verify that the listing call contains our new SDB
        def sdbList = listSdbs(cerberusAuthToken, V1_SAFE_DEPOSIT_BOX_PATH)
        def foundNewSdb = false
        def listSdb

        sdbList.getList("").each { sdbMeta ->
            if (sdbMeta.id == sdbId) {
                foundNewSdb = true
                listSdb = sdbMeta
            }
        }
        assertTrue("Failed to find the newly created SDB in the list results", foundNewSdb)
        assertEquals(listSdb.name, sdb.get('name'))
        assertEquals(listSdb.id, sdb.get('id'))
        assertEquals(listSdb.path, sdb.get('path'))
        assertEquals(listSdb.'category_id', sdb.get('category_id'))

        // update the sdb
        description = "${Lorem.getWords(60)}"
        userGroupPermissions.add([
            "name": 'bar',
            "role_id": roleMap.write
        ])
        iamRolePermissions.add([
            "account_id": "1111111111",
            "iam_role_name": "fake_role2",
            "role_id": roleMap.read
        ])
        updateSdbV1(cerberusAuthToken, sdbId, description, owner, userGroupPermissions, iamRolePermissions)
        JsonPath sdbUpdated = readSdb(cerberusAuthToken, sdbId, V1_SAFE_DEPOSIT_BOX_PATH)

        // verify that the sdbUpdated we created contains the data we expect
        assertSafeDepositBoxV1HasFields(sdbUpdated, name, description, categoryId, owner, userGroupPermissions, iamRolePermissions)

        // delete the SDB
        deleteSdb(cerberusAuthToken, sdbId, V1_SAFE_DEPOSIT_BOX_PATH)

        // verify that the sdb is not longer in the list
        def updatedSdbList = listSdbs(cerberusAuthToken, V1_SAFE_DEPOSIT_BOX_PATH)
        def isSdbPresentInUpdatedList = false

        updatedSdbList.getList("").each { sdbMeta ->
            if (sdbMeta.id == sdbId) {
                isSdbPresentInUpdatedList = true
            }
        }
        assertFalse("The created sdb should not be in the sdb listing call after deleting it", isSdbPresentInUpdatedList)
    }

    static void "v2 create, read, list, update and then delete a safe deposit box"(Map cerberusAuthPayloadData) {
        String accountId = PropUtils.getPropWithDefaultValue("TEST_ACCOUNT_ID", "1111111111")
        String roleName = PropUtils.getPropWithDefaultValue("TEST_ROLE_NAME", "fake_role")
        String cerberusAuthToken = cerberusAuthPayloadData.'client_token'
        String groups = cerberusAuthPayloadData.metadata.groups
        def group = groups.split(/,/)[0]

        // Create a map of category ids to names'
        JsonPath getCategoriesResponse = getCategories(cerberusAuthToken)
        def catMap = [:]
        getCategoriesResponse.getList("").each { category ->
            catMap.put category.display_name, category.id
        }
        // Create a map of role ids to names
        JsonPath getRolesResponse = getRoles(cerberusAuthToken)
        def roleMap = [:]
        getRolesResponse.getList("").each { role ->
            roleMap.put role.name, role.id
        }

        String name = "${RandomStringUtils.randomAlphabetic(5,10)} ${RandomStringUtils.randomAlphabetic(5,10)}"
        String description = "${Lorem.getWords(50)}"
        String categoryId = catMap.Applications
        String owner = group
        def userGroupPermissions = [
            [
                "name": 'foo',
                "role_id": roleMap.read
            ]
        ]

        String arn = "arn:aws:iam::${accountId}:role/${roleName}"
        def iamPrincipalPermissions = [
            [
                "iam_principal_arn": arn,
                "role_id": roleMap.owner
            ]
        ]

        // verify that the sdb we created contains the data we expect
        def createdSdb = createSdbV2(cerberusAuthToken, name, description, categoryId, owner, userGroupPermissions, iamPrincipalPermissions)
        assertSafeDepositBoxV2HasFields(createdSdb, name, description, categoryId, owner, userGroupPermissions, iamPrincipalPermissions)

        // test read sdb returns returns expected data
        def sdbId = createdSdb.getString("id")
        JsonPath sdb = readSdb(cerberusAuthToken, sdbId, V2_SAFE_DEPOSIT_BOX_PATH)
        assertSafeDepositBoxV2HasFields(sdb, name, description, categoryId, owner, userGroupPermissions, iamPrincipalPermissions)

        // verify that the listing call contains our new SDB
        def sdbList = listSdbs(cerberusAuthToken, V2_SAFE_DEPOSIT_BOX_PATH)
        def foundNewSdb = false
        def listSdb

        sdbList.getList("").each { sdbMeta ->
            if (sdbMeta.id == sdbId) {
                foundNewSdb = true
                listSdb = sdbMeta
            }
        }
        assertTrue("Failed to find the newly created SDB in the list results", foundNewSdb)
        assertEquals(listSdb.name, sdb.get('name'))
        assertEquals(listSdb.id, sdb.get('id'))
        assertEquals(listSdb.path, sdb.get('path'))
        assertEquals(listSdb.'category_id', sdb.get('category_id'))

        // update the sdb
        description = "${Lorem.getWords(60)}"
        userGroupPermissions.add([
            "name": 'bar',
            "role_id": roleMap.write
        ])
        iamPrincipalPermissions.add([
            "iam_principal_arn": "arn:aws:iam::1111111111:role/fake_role2",
            "role_id": roleMap.read
        ])
        JsonPath sdbUpdatedUpdate = updateSdbV2(cerberusAuthToken, sdbId, description, owner, userGroupPermissions, iamPrincipalPermissions)

        // verify that the sdbUpdated we created contains the data we expect
        assertSafeDepositBoxV2HasFields(sdbUpdatedUpdate, name, description, categoryId, owner, userGroupPermissions, iamPrincipalPermissions)

        JsonPath sdbUpdatedRead = readSdb(cerberusAuthToken, sdbId, V2_SAFE_DEPOSIT_BOX_PATH)
        assertSafeDepositBoxV2HasFields(sdbUpdatedRead, name, description, categoryId, owner, userGroupPermissions, iamPrincipalPermissions)

        // delete the SDB
        deleteSdb(cerberusAuthToken, sdbId, V2_SAFE_DEPOSIT_BOX_PATH)

        // verify that the sdb is not longer in the list
        def updatedSdbList = listSdbs(cerberusAuthToken, V2_SAFE_DEPOSIT_BOX_PATH)
        def isSdbPresentInUpdatedList = false

        updatedSdbList.getList("").each { sdbMeta ->
            if (sdbMeta.id == sdbId) {
                isSdbPresentInUpdatedList = true
            }
        }
        assertFalse("The created sdb should not be in the sdb listing call after deleting it", isSdbPresentInUpdatedList)
    }

    static Map "login user with multi factor authentication (or skip mfa if not required) and return auth data"(
            String username, String password, String otpSecret, String deviceId) {

        JsonPath loginResp = loginUser(username, password)
        String status = loginResp.getString("status")
        if (status == "success") {
            return loginResp.get("data.client_token")
        } else {
            def mfaResp = finishMfaUserAuth(
                    loginResp.getString("data.state_token"),
                    deviceId,
                    new Totp(otpSecret).now())

            return mfaResp.get('data.client_token')
        }
    }

    private static void assertIamPermissionsEquals(boolean isV1, def expectedIamPermissions, def actualIamPermissions) {
        assertEquals(expectedIamPermissions.size(), actualIamPermissions.size())
        for (def expectedPerm : expectedIamPermissions) {
            def found = false
            for (def actualPerm : actualIamPermissions) {
                if (isV1) {
                    if (expectedPerm.'iam_role_name' == actualPerm.'iam_role_name') {
                        found = true
                        assertEquals(expectedPerm.'account_id', actualPerm.'account_id')
                    }
                } else {
                    if (expectedPerm.'iam_principal_arn' == actualPerm.'iam_principal_arn') {
                        found = true
                    }
                }
            }
            assertTrue("The expected user permission was not found in the actual results", found)
        }
    }

    private static void assertUserGroupPermissionsEquals(def expectedUserGroupPermissions, def actualUserGroupPermissions) {

        assertEquals(expectedUserGroupPermissions.size(), actualUserGroupPermissions.size())
        for (def expectedPerm : expectedUserGroupPermissions) {
            def found = false
            for (def actualPerm : actualUserGroupPermissions) {
                if (expectedPerm.name == actualPerm.name) {
                    found = true
                    assertEquals(expectedPerm.'role_id', actualPerm.'role_id')
                    assertEquals(expectedPerm.name, actualPerm.name)
                }
            }
            assertTrue("The expected user permission was not found in the actual results", found)
        }
    }

    private static void assertSafeDepositBoxV1HasFields(def safeDepositBox, def name, def description, def categoryId,
                                                             def owner, def userGroupPermissions, def iamRolePermissions) {

        assertEquals(name, safeDepositBox.get('name'))
        assertEquals(description, safeDepositBox.get('description'))
        assertEquals(categoryId, safeDepositBox.get('category_id'))
        assertEquals(owner, safeDepositBox.get('owner'))
        assertIamPermissionsEquals(true, iamRolePermissions, safeDepositBox.getList('iam_role_permissions'))
        assertUserGroupPermissionsEquals(userGroupPermissions, safeDepositBox.getList('user_group_permissions'))
    }

    private static void assertSafeDepositBoxV2HasFields(def safeDepositBox, def name, def description, def categoryId,
                                                        def owner, def userGroupPermissions, def iamPrincipalPermissions) {

        assertEquals(name, safeDepositBox.get('name'))
        assertEquals(description, safeDepositBox.get('description'))
        assertEquals(categoryId, safeDepositBox.get('category_id'))
        assertEquals(owner, safeDepositBox.get('owner'))
        assertIamPermissionsEquals(false, iamPrincipalPermissions, safeDepositBox.getList('iam_principal_permissions'))
        assertUserGroupPermissionsEquals(userGroupPermissions, safeDepositBox.getList('user_group_permissions'))
    }
}
