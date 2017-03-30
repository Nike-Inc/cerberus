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
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

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

    private final String rootUserArn;

    private final String adminRoleArn;

    private final String cmsRoleArn;

    private final ObjectMapper objectMapper;

    @Inject
    public KmsPolicyService(@Named(ROOT_USER_ARN_PROPERTY) String rootUserArn,
                            @Named(ADMIN_ROLE_ARN_PROPERTY) String adminRoleArn,
                            @Named(CMS_ROLE_ARN_PROPERTY) String cmsRoleArn) {
        this.rootUserArn = rootUserArn;
        this.adminRoleArn = adminRoleArn;
        this.cmsRoleArn = cmsRoleArn;

        objectMapper = new ObjectMapper();
    }

    public String generateStandardKmsPolicy(final String iamRoleAccountId, final String iamRoleName) {
        return generateStandardKmsPolicy(String.format(AwsIamRoleArnParser.AWS_IAM_ROLE_ARN_TEMPLATE,
                iamRoleAccountId, iamRoleName));
    }

    /***
     * Please note that currently in the AWS Core SDK 1.11.108 that Policy.fromJson strips hyphens from AWS ARN Principals
     * and Hyphens are valid in IAM role names. We will need to manually use JsonNodes and not rely on fromJson
     *
     * When you manually instantiate a Principal you can specify true/false for striping hyphens,
     * when deserializing with fromJson this seems to always get set to true.
     *
     * @param policyString - The KMS key policy as a String
     * @param iamRoleArn - The IAM Role that is supposed to have decrypt permissions
     * @return true if the policy is valid, false if the policy contains an ID because the ARN had been deleted and recreated
     */
    public boolean isPolicyValid(String policyString, String iamRoleArn) {
        // The below json node stuff is lame, should be able to use Objects created from Policy.fromJson(string)
        // todo file Github issue and or PR with AWS SDK project
        try {
            JsonNode policy = null;
            policy = objectMapper.readTree(policyString);
            JsonNode statements = policy.get("Statement");
            for (JsonNode statement : statements) {
                if (CERBERUS_CONSUMER_SID.equals(statement.get("Sid").textValue())) {
                    String statementAWSPrincipal = statement.get("Principal").get("AWS").textValue();
                    if (iamRoleArn.equals(statementAWSPrincipal)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // if we can't deserialize we will assume policy has been corrupted manually and regenerate it
            logger.error("Failed to validate policy, did someone manually edit the kms policy?", e);
        }

        return false;
    }

    public String generateStandardKmsPolicy(String iamRoleArn) {
        Policy kmsPolicy = new Policy();

        Statement rootUserStatement = new Statement(Statement.Effect.Allow);
        rootUserStatement.withId("Root User Has All Actions");
        rootUserStatement.withPrincipals(new Principal(AWS_PROVIDER, rootUserArn, false));
        rootUserStatement.withActions(KmsActions.AllKmsActions);
        rootUserStatement.withResources(new Resource("*"));

        Statement keyAdministratorStatement = new Statement(Statement.Effect.Allow);
        keyAdministratorStatement.withId("Admin Role Has All Actions");
        keyAdministratorStatement.withPrincipals(new Principal(AWS_PROVIDER, adminRoleArn, false));
        keyAdministratorStatement.withActions(KmsActions.AllKmsActions);
        keyAdministratorStatement.withResources(new Resource("*"));

        Statement instanceUsageStatement = new Statement(Statement.Effect.Allow);
        instanceUsageStatement.withId("CMS Role Key Access");
        instanceUsageStatement.withPrincipals(new Principal(AWS_PROVIDER, cmsRoleArn, false));
        instanceUsageStatement.withActions(KmsActions.EncryptAction,
                KmsActions.DecryptAction,
                KmsActions.AllReEncryptActions,
                KmsActions.AllGenerateDataKeyActions,
                KmsActions.DescribeKey);
        instanceUsageStatement.withResources(new Resource("*"));

        Statement iamRoleUsageStatement = new Statement(Statement.Effect.Allow);
        iamRoleUsageStatement.withId(CERBERUS_CONSUMER_SID);
        iamRoleUsageStatement.withPrincipals(
                new Principal(AWS_PROVIDER, iamRoleArn, false));
        iamRoleUsageStatement.withActions(KmsActions.DecryptAction);
        iamRoleUsageStatement.withResources(new Resource("*"));

        kmsPolicy.withStatements(rootUserStatement,
                keyAdministratorStatement,
                instanceUsageStatement,
                iamRoleUsageStatement);

        return kmsPolicy.toJson();
    }

    private enum KmsActions implements Action {
        AllKmsActions("kms:*"),

        EncryptAction("kms:Encrypt"),

        DecryptAction("kms:Decrypt"),

        AllReEncryptActions("kms:ReEncrypt*"),

        AllGenerateDataKeyActions("kms:GenerateDataKey*"),

        DescribeKey("kms:DescribeKey");

        private final String action;

        KmsActions(String action) {
            this.action = action;
        }

        @Override
        public String getActionName() {
            return action;
        }
    }
}
