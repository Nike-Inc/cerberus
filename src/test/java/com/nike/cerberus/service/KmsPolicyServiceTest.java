package com.nike.cerberus.service;

import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.KMSActions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
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

        expectedPolicyStream.close();
    }

    @Test
    public void test_that_isPolicyValid_returns_true_with_a_valid_policy() throws IOException {
        InputStream policy = getClass().getClassLoader()
                .getResourceAsStream("com/nike/cerberus/service/valid-cerberus-iam-auth-kms-key-policy.json");
        String policyJsonAsString = IOUtils.toString(policy, "UTF-8");

        assertTrue(kmsPolicyService.isPolicyValid(policyJsonAsString, CERBERUS_CONSUMER_IAM_ROLE_ARN));

        policy.close();
    }

    @Test
    public void test_that_isPolicyValid_returns_false_with_an_invalid_policy() throws IOException {
        InputStream policy = getClass().getClassLoader()
                .getResourceAsStream("com/nike/cerberus/service/invalid-cerberus-iam-auth-kms-key-policy.json");
        String policyJsonAsString = IOUtils.toString(policy, "UTF-8");

        assertFalse(kmsPolicyService.isPolicyValid(policyJsonAsString, CERBERUS_CONSUMER_IAM_ROLE_ARN));

        policy.close();
    }

    @Test
    public void test_that_isPolicyValid_returns_false_when_cms_does_not_have_delete_permission() throws IOException {
        InputStream policy = getClass().getClassLoader()
                .getResourceAsStream("com/nike/cerberus/service/invalid-cerberus-kms-key-policy-cms-cannot-delete.json");
        String policyJsonAsString = IOUtils.toString(policy, "UTF-8");

        assertFalse(kmsPolicyService.isPolicyValid(policyJsonAsString, CERBERUS_CONSUMER_IAM_ROLE_ARN));

        policy.close();
    }

    @Test
    public void test_that_isPolicyValid_returns_false_when_a_non_standard_policy_is_supplied() {
        assertFalse(kmsPolicyService.isPolicyValid(null, CERBERUS_CONSUMER_IAM_ROLE_ARN));
    }

    @Test
    public void test_that_cmsCanScheduleKeyDeletion_returns_true_when_cms_can_delete() throws IOException {
        InputStream policy = getClass().getClassLoader()
                .getResourceAsStream("com/nike/cerberus/service/valid-cerberus-iam-auth-kms-key-policy.json");
        String policyJsonAsString = IOUtils.toString(policy, "UTF-8");

        assertTrue(kmsPolicyService.cmsHasKeyDeletePermissions(policyJsonAsString));

        policy.close();
    }

    @Test
    public void test_that_cmsCanScheduleKeyDeletion_returns_false_when_cms_cannot_delete() throws IOException {
        InputStream policy = getClass().getClassLoader()
                .getResourceAsStream("com/nike/cerberus/service/invalid-cerberus-kms-key-policy-cms-cannot-delete.json");
        String policyJsonAsString = IOUtils.toString(policy, "UTF-8");

        assertFalse(kmsPolicyService.cmsHasKeyDeletePermissions(policyJsonAsString));

        policy.close();
    }

    @Test
    public void test_that_overwriteCMSPolicy_returns_policy_that_includes_missing_actions() throws IOException {
        InputStream policy = getClass().getClassLoader()
                .getResourceAsStream("com/nike/cerberus/service/invalid-cerberus-kms-key-policy-cms-cannot-delete.json");
        String policyJsonAsString = IOUtils.toString(policy, "UTF-8");

        Action actionNotIncludedInInvalidJson1 = KMSActions.ScheduleKeyDeletion;
        Action actionNotIncludedInInvalidJson2 = KMSActions.CancelKeyDeletion;
        String result = kmsPolicyService.overwriteCMSPolicy(policyJsonAsString);
        assertFalse(StringUtils.equals(policyJsonAsString, result));
        assertTrue(StringUtils.contains(result, actionNotIncludedInInvalidJson1.getActionName()));
        assertTrue(StringUtils.contains(result, actionNotIncludedInInvalidJson2.getActionName()));
        assertTrue(kmsPolicyService.cmsHasKeyDeletePermissions(result));

        policy.close();
    }

    @Test
    public void test_that_overwriteCMSPolicy_does_not_fail_with_valid_policy() throws IOException {
        InputStream policy = getClass().getClassLoader()
                .getResourceAsStream("com/nike/cerberus/service/valid-cerberus-iam-auth-kms-key-policy.json");
        String policyJsonAsString = IOUtils.toString(policy, "UTF-8");

        String result = kmsPolicyService.overwriteCMSPolicy(policyJsonAsString);
        assertFalse(StringUtils.equals(policyJsonAsString, result));
        assertTrue(kmsPolicyService.cmsHasKeyDeletePermissions(result));

        policy.close();
    }

    @Test
    public void test_that_statementAllowsAction_returns_true_when_action_in_statement() {
        Action action = KMSActions.CancelKeyDeletion;
        Statement statement = new Statement(Statement.Effect.Allow).withActions(action);
        assertTrue(kmsPolicyService.statementIncludesAction(statement, action));
    }

    @Test
    public void test_that_generateStandardCMSPolicyStatement_returns_a_valid_statement() {

        Statement result = kmsPolicyService.generateStandardCMSPolicyStatement();
        assertEquals(KmsPolicyService.CERBERUS_MANAGEMENT_SERVICE_SID, result.getId());
        assertEquals(Statement.Effect.Allow, result.getEffect());
        assertTrue(kmsPolicyService.cmsHasKeyDeletePermissions(new Policy().withStatements(result).toJson()));
    }

    @Test
    public void test_that_removePolicyFromStatement_removes_the_given_statement() {

        String removeId = "remove id";
        String keepId = "keep id";
        Statement statementToRemove = new Statement(Statement.Effect.Allow).withId(removeId).withActions(KMSActions.AllKMSActions);
        Statement statementToKeep = new Statement(Statement.Effect.Deny).withId(keepId).withActions(KMSActions.AllKMSActions);
        Policy policy = new Policy("policy", Lists.newArrayList(statementToKeep, statementToRemove));

        kmsPolicyService.removeStatementFromPolicy(policy, removeId);

        assertTrue(policy.getStatements().contains(statementToKeep));
        assertFalse(policy.getStatements().contains(statementToRemove));

    }

    @Test
    public void test_that_removeConsumerPrincipalFromPolicy_removes_cms_statement() throws IOException {
        InputStream policy = getClass().getClassLoader()
                .getResourceAsStream("com/nike/cerberus/service/valid-cerberus-iam-auth-kms-key-policy.json");
        String policyJsonAsString = IOUtils.toString(policy, "UTF-8");

        String result = kmsPolicyService.removeConsumerPrincipalFromPolicy(policyJsonAsString);
        assertTrue(StringUtils.contains(policyJsonAsString, KmsPolicyService.CERBERUS_CONSUMER_SID));
        assertFalse(StringUtils.contains(result, KmsPolicyService.CERBERUS_CONSUMER_SID));

        policy.close();
    }
}