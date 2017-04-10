package com.nike.cerberus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the KMS Policy Service
 *
 *
 */
public class KmsPolicyServiceTest {

    private static final String CERBERUS_CONSUMER_IAM_ROLE_ARN = "arn:aws:iam::1234567890:role/cerberus-consumer";

    private KmsPolicyService kmsPolicyService;
    private ObjectMapper objectMapper;

    @Before
    public void before() {
        String rootUserArn = "arn:aws:iam::1111111111:root";
        String adminRoleArn = "arn:aws:iam::1111111111:role/admin";
        String cmsRoleArn = "arn:aws:iam::1111111111:role/cms-iam-role";
        kmsPolicyService = new KmsPolicyService(rootUserArn, adminRoleArn, cmsRoleArn);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void test_generateStandardKmsPolicy() throws IOException {
        InputStream expectedPolicyStream = getClass().getClassLoader()
                .getResourceAsStream("com/nike/cerberus/service/valid-cerberus-iam-auth-kms-key-policy.json");
        String expectedPolicyJsonAsString = IOUtils.toString(expectedPolicyStream, "UTF-8");
        JsonNode expectedPolicy = objectMapper.readTree(expectedPolicyJsonAsString);
        String minifiedPolicyJsonAsString = expectedPolicy.toString();

        // invoke method under test
        String actualPolicyJsonAsString = kmsPolicyService.generateStandardKmsPolicy(CERBERUS_CONSUMER_IAM_ROLE_ARN);

        assertEquals(minifiedPolicyJsonAsString, actualPolicyJsonAsString);
    }

    @Test
    public void test_that_generateStandardKmsPolicy_returns_true_with_a_valid_policy() throws IOException {
        InputStream policy = getClass().getClassLoader()
                .getResourceAsStream("com/nike/cerberus/service/valid-cerberus-iam-auth-kms-key-policy.json");
        String policyJsonAsString = IOUtils.toString(policy, "UTF-8");

        assertTrue(kmsPolicyService.isPolicyValid(policyJsonAsString, CERBERUS_CONSUMER_IAM_ROLE_ARN));
    }

    @Test
    public void test_that_generateStandardKmsPolicy_returns_false_with_an_invalid_policy() throws IOException {
        InputStream policy = getClass().getClassLoader()
                .getResourceAsStream("com/nike/cerberus/service/invalid-cerberus-iam-auth-kms-key-policy.json");
        String policyJsonAsString = IOUtils.toString(policy, "UTF-8");

        assertFalse(kmsPolicyService.isPolicyValid(policyJsonAsString, CERBERUS_CONSUMER_IAM_ROLE_ARN));
    }

    @Test
    public void test_that_generateStandardKmsPolicy_returns_false_when_a_non_standard_policy_is_supplied() {
        assertFalse(kmsPolicyService.isPolicyValid(null, CERBERUS_CONSUMER_IAM_ROLE_ARN));
    }

}