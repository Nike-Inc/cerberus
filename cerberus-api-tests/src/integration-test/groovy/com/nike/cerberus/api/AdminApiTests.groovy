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
import org.apache.http.HttpStatus
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test
import org.testng.collections.Maps

import java.security.NoSuchAlgorithmException

import static com.nike.cerberus.api.CerberusApiActions.cleanUpOrphanedAndInactiveRecords
import static com.nike.cerberus.api.CerberusApiActions.getSdbMetadata
import static com.nike.cerberus.api.CerberusApiActions.validatePUTApiResponse
import static com.nike.cerberus.api.CerberusCompositeApiActions.*

class AdminApiTests {

    private String cerberusAuthToken

    @BeforeTest(enabled = false)
    void beforeTest() throws NoSuchAlgorithmException {
        TestUtils.configureRestAssured()
        loadRequiredEnvVars()
    }

    private void loadRequiredEnvVars() {
        cerberusAuthToken = PropUtils.getRequiredProperty("TEST_ADMIN_CERBERUS_TOKEN",
                "A Cerberus auth token with permissions to call admin endpoints")
    }

    @Test(enabled = false)
    void "test that an admin can call the v1 cleanup endpoint"() {
        int deleteKmsKeysAfterNDaysOfInactivity = Integer.MAX_VALUE-1  // try not to actually delete any keys in this test
        cleanUpOrphanedAndInactiveRecords(cerberusAuthToken, deleteKmsKeysAfterNDaysOfInactivity)
    }

    @Test(enabled = false)
    void "test that an admin can call PUT v1 metadata endpoint"() {
        String schemaFilePath = "$NEGATIVE_JSON_SCHEMA_ROOT_PATH/put-metadata-category-null.json"
        // send invalid data so that actual data is not written, but only the permissions of this call is tested
        validatePUTApiResponse(cerberusAuthToken, "v1/metadata", HttpStatus.SC_BAD_REQUEST, schemaFilePath, Maps.newHashMap())
    }

    @Test(enabled = false)
    void "test that an admin can call GET v1 metadata endpoint"() {
        getSdbMetadata(cerberusAuthToken)
    }
}
