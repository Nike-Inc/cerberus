/*
 * Copyright (c) 2016 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.service;

import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.PolicyReaderOptions;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.KMSActions;
import com.amazonaws.auth.policy.internal.JsonPolicyReader;
import com.amazonaws.services.kms.model.PutKeyPolicyRequest;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helpful service for putting together the KMS policy documents to be associated with provisioned KMS keys.
 */
@Singleton
public class KmsPolicyService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String ROOT_USER_ARN_PROPERTY = "root.user.arn";

    private static final String ADMIN_ROLE_ARN_PROPERTY = "admin.role.arn";

    private static final String CMS_ROLE_ARN_PROPERTY = "cms.role.arn";

    private static final String AWS_PROVIDER = "AWS";

    public static final String CERBERUS_CONSUMER_SID = "Target IAM Role Has Decrypt Action";

    protected static final String CERBERUS_MANAGEMENT_SERVICE_SID = "CMS Role Key Access";

    private final String rootUserArn;

    private final String adminRoleArn;

    private final String cmsRoleArn;

    private final JsonPolicyReader policyReader;

    private final AwsIamRoleArnParser awsIamRoleArnParser;

    @Inject
    public KmsPolicyService(@Named(ROOT_USER_ARN_PROPERTY) String rootUserArn,
                            @Named(ADMIN_ROLE_ARN_PROPERTY) String adminRoleArn,
                            @Named(CMS_ROLE_ARN_PROPERTY) String cmsRoleArn,
                            AwsIamRoleArnParser awsIamRoleArnParser) {

        this.rootUserArn = rootUserArn;
        this.adminRoleArn = adminRoleArn;
        this.cmsRoleArn = cmsRoleArn;
        this.awsIamRoleArnParser = awsIamRoleArnParser;

        PolicyReaderOptions policyReaderOptions = new PolicyReaderOptions();
        policyReaderOptions.setStripAwsPrincipalIdHyphensEnabled(false);
        policyReader = new JsonPolicyReader(policyReaderOptions);
    }

    /**
     * Validates that the given key policy grants appropriate permissions to CMS and allows the given IAM principal to
     * access the KMS key.
     *
     * @param policyJson - The KMS key policy as a String
     * @return true if the policy is valid, false if the policy contains an ID because the ARN had been deleted and recreated
     */
    public boolean isPolicyValid(String policyJson) {
        return consumerPrincipalIsAnArnAndNotAnId(policyJson) && cmsHasKeyDeletePermissions(policyJson);
    }

    /**
     * Check that the given IAM principal has permissions to access the KMS key.
     *
     * This is important because when an IAM principal is deleted and recreated with the same name, then the recreated
     * principal cannot access the KMS key until the key policy is regenerated -- updating the policy permissions to
     * allow the ARN of the recreated principal instead of the ID of the deleted principal.
     *
     * @param policyJson - The KMS key policy as a String
     */
    protected boolean consumerPrincipalIsAnArnAndNotAnId(String policyJson) {
        try {
            Policy policy = policyReader.createPolicyFromJsonString(policyJson);
            return policy.getStatements()
                    .stream()
                    .anyMatch(statement ->
                            StringUtils.equals(statement.getId(), CERBERUS_CONSUMER_SID) &&
                                    statement.getPrincipals()
                                            .stream()
                                            .anyMatch(principal -> awsIamRoleArnParser.isRoleArn(principal.getId())));
        } catch (Exception e) {
            // if we can't deserialize we will assume policy has been corrupted manually and regenerate it
            logger.error("Failed to validate policy, did someone manually edit the kms policy?", e);
        }

        return false;
    }

    /**
     * Validate that the IAM principal for the CMS has permissions to schedule and cancel deletion of the KMS key.
     * @param policyJson - The KMS key policy as a String
     */
    protected boolean cmsHasKeyDeletePermissions(String policyJson) {
        try {
            Policy policy = policyReader.createPolicyFromJsonString(policyJson);
            return policy.getStatements()
                    .stream()
                    .anyMatch(statement ->
                            StringUtils.equals(statement.getId(), CERBERUS_MANAGEMENT_SERVICE_SID) &&
                                    statementAppliesToPrincipal(statement, cmsRoleArn) &&
                                    statement.getEffect() == Statement.Effect.Allow &&
                                    statementIncludesAction(statement, KMSActions.ScheduleKeyDeletion) &&
                                    statementIncludesAction(statement, KMSActions.CancelKeyDeletion));
        } catch (Exception e) {
            logger.error("Failed to validate that CMS can delete KMS key, there may be something wrong with the policy", e);
        }

        return false;
    }

    /**
     * Overwrite the policy statement for CMS with the standard statement. Add the standard statement for CMS
     * to the policy if it did not already exist.
     *
     * @param policyJson - The KMS key policy in JSON format
     * @return - The updated JSON KMS policy containing a regenerated statement for CMS
     */
    protected String overwriteCMSPolicy(String policyJson) {
        Policy policy = policyReader.createPolicyFromJsonString(policyJson);
        removeStatementFromPolicy(policy, CERBERUS_MANAGEMENT_SERVICE_SID);
        Collection<Statement> statements = policy.getStatements();
        statements.add(generateStandardCMSPolicyStatement());
        return policy.toJson();
    }

    /**
     * Removes the 'Allow' statement for the consumer IAM principal.
     *
     * This is important when updating the KMS policy
     * because if the IAM principal has been deleted then the KMS policy will contain the principal 'ID' instead of the
     * ARN, which renders the policy invalid when calling {@link com.amazonaws.services.kms.AWSKMSClient#putKeyPolicy(PutKeyPolicyRequest)}.
     *
     * @param policyJson - Key policy JSON from which to remove consumer principal
     * @return - The updated key policy JSON
     */
    protected String removeConsumerPrincipalFromPolicy(String policyJson) {
        Policy policy = policyReader.createPolicyFromJsonString(policyJson);
        removeStatementFromPolicy(policy, CERBERUS_CONSUMER_SID);
        return policy.toJson();
    }

    protected void removeStatementFromPolicy(Policy policy, String statementId) {
        Collection<Statement> existingStatements = policy.getStatements();
        List<Statement> policyStatementsExcludingConsumer = existingStatements.stream()
                .filter(statement -> ! StringUtils.equals(statement.getId(), statementId))
                .collect(Collectors.toList());
        policyStatementsExcludingConsumer.add(generateStandardCMSPolicyStatement());
        policy.setStatements(policyStatementsExcludingConsumer);
    }

    /**
     * Validates that the given KMS key policy statement applies to the given principal
     */
    protected boolean statementAppliesToPrincipal(Statement statement, String principalArn) {

        return statement.getPrincipals()
                .stream()
                .anyMatch(principal ->
                        StringUtils.equals(principal.getId(), principalArn));
    }

    /**
     * Validates that the given KMS key policy statement includes the given action
     */
    protected boolean statementIncludesAction(Statement statement, Action action) {

        return statement.getActions()
                .stream()
                .anyMatch(statementAction ->
                        StringUtils.equals(statementAction.getActionName(), action.getActionName()));
    }

    /**
     * Generates the standard KMS key policy statement for the Cerberus Management Service
     */
    protected Statement generateStandardCMSPolicyStatement() {
        Statement cmsStatement = new Statement(Statement.Effect.Allow);
        cmsStatement.withId(CERBERUS_MANAGEMENT_SERVICE_SID);
        cmsStatement.withPrincipals(new Principal(AWS_PROVIDER, cmsRoleArn, false));
        cmsStatement.withActions(
                KMSActions.Encrypt,
                KMSActions.Decrypt,
                KMSActions.ReEncryptFrom,
                KMSActions.ReEncryptTo,
                KMSActions.GenerateDataKey,
                KMSActions.GenerateDataKeyWithoutPlaintext,
                KMSActions.GenerateRandom,
                KMSActions.DescribeKey,
                KMSActions.ScheduleKeyDeletion,
                KMSActions.CancelKeyDeletion);
        cmsStatement.withResources(new Resource("*"));

        return cmsStatement;
    }

    public String generateStandardKmsPolicy(String iamRoleArn) {
        Policy kmsPolicy = new Policy();

        Statement rootUserStatement = new Statement(Statement.Effect.Allow);
        rootUserStatement.withId("Root User Has All Actions");
        rootUserStatement.withPrincipals(new Principal(AWS_PROVIDER, rootUserArn, false));
        rootUserStatement.withActions(KMSActions.AllKMSActions);
        rootUserStatement.withResources(new Resource("*"));

        Statement keyAdministratorStatement = new Statement(Statement.Effect.Allow);
        keyAdministratorStatement.withId("Admin Role Has All Actions");
        keyAdministratorStatement.withPrincipals(new Principal(AWS_PROVIDER, adminRoleArn, false));
        keyAdministratorStatement.withActions(KMSActions.AllKMSActions);
        keyAdministratorStatement.withResources(new Resource("*"));

        Statement instanceUsageStatement = generateStandardCMSPolicyStatement();

        Statement iamRoleUsageStatement = new Statement(Statement.Effect.Allow);
        iamRoleUsageStatement.withId(CERBERUS_CONSUMER_SID);
        iamRoleUsageStatement.withPrincipals(
                new Principal(AWS_PROVIDER, iamRoleArn, false));
        iamRoleUsageStatement.withActions(KMSActions.Decrypt);
        iamRoleUsageStatement.withResources(new Resource("*"));

        kmsPolicy.withStatements(rootUserStatement,
                keyAdministratorStatement,
                instanceUsageStatement,
                iamRoleUsageStatement);

        return kmsPolicy.toJson();
    }
}
