package com.nike.cerberus.api.util

import com.nike.cerberus.util.PropUtils
import com.thedeanda.lorem.Lorem
import org.apache.commons.lang3.RandomStringUtils

import java.security.NoSuchAlgorithmException

import static io.restassured.RestAssured.*

class TestUtils {

    private static boolean hasBeenConfigured = false;

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
            hasBeenConfigured = true
        }
    }

    static String generateRandomSdbName() {
        return "${RandomStringUtils.randomAlphabetic(5,10)} ${RandomStringUtils.randomAlphabetic(5,10)}"
    }

    static String generateRandomSdbDescription() {
        return "${Lorem.getWords(50)}"
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
}
