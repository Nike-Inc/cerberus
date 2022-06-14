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

package com.nike.cerberus.api.util

import com.nike.cerberus.util.PropUtils
import com.thedeanda.lorem.Lorem
import com.thedeanda.lorem.LoremIpsum
import org.apache.commons.lang3.RandomStringUtils

import java.security.NoSuchAlgorithmException

import static com.nike.cerberus.api.CerberusCompositeApiActions.partition
import static io.restassured.RestAssured.*

class TestUtils {

    private static boolean hasBeenConfigured = false;

    private static String partition = PropUtils.getPropWithDefaultValue("TEST_PARTITION", "aws")

    private TestUtils() {
        // no constructing
    }

    static void configureRestAssured() throws NoSuchAlgorithmException {
        if (!hasBeenConfigured) {
            baseURI = PropUtils.getRequiredProperty("CERBERUS_API_URL", "The Cerberus API URL to Test")

            System.out.println("Configuring rest assured to use baseURI: " + baseURI)

            enableLoggingOfRequestAndResponseIfValidationFails()

            // allow us to ping instances directly and not go through the load balancer
            useRelaxedHTTPSValidation()

            config.getHttpClientConfig().reuseHttpClientInstance()

            System.out.print("Performing sanity check get on the health check.")
            get(baseURI + "/healthcheck").then().statusCode(200)
            System.out.println(" Success!")

            System.out.print("Ensuring /info path is exposed")
            get(baseURI + "/info").then().statusCode(200)
            System.out.println(" Success!")

            hasBeenConfigured = true
        }
    }

    static String generateRandomSdbName() {
        return "${RandomStringUtils.randomAlphabetic(5,10)} ${RandomStringUtils.randomAlphabetic(5,10)}"
    }

    static String generateRandomSdbDescription() {
        Lorem lorem =new LoremIpsum();
        return lorem.getWords(50);
    }

    static Map generateSdbJson(String description,
                                       String owner,
                                       List<Map<String, String>> userGroupPermissions,
                                       List<Map<String, String>> iamPrincipalPermissions) {
        return [
                description: description,
                owner: owner,
                'user_group_permissions': userGroupPermissions,
                'iam_role_permissions': iamPrincipalPermissions
        ]
    }

    static String updateArnWithPartition(String arn) {
        String[] arnParts = arn.split(":")
        arnParts[1] = partition
        return arnParts.collect().join(":")
    }
}
