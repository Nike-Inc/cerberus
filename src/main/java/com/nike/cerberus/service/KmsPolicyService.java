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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Helpful service for putting together the KMS policy documents to be associated with provisioned KMS keys.
 */
@Singleton
public class KmsPolicyService {

    private static final String ROOT_USER_ARN_PROPERTY = "root.user.arn";

    private static final String ADMIN_ROLE_ARN_PROPERTY = "admin.role.arn";

    private static final String CMS_ROLE_ARN_PROPERTY = "cms.role.arn";

    private static final String AWS_PROVIDER = "AWS";

    private final String rootUserArn;

    private final String adminRoleArn;

    private final String cmsRoleArn;

    @Inject
    public KmsPolicyService(@Named(ROOT_USER_ARN_PROPERTY) String rootUserArn,
                            @Named(ADMIN_ROLE_ARN_PROPERTY) String adminRoleArn,
                            @Named(CMS_ROLE_ARN_PROPERTY) String cmsRoleArn) {
        this.rootUserArn = rootUserArn;
        this.adminRoleArn = adminRoleArn;
        this.cmsRoleArn = cmsRoleArn;
    }

    public String generateStandardKmsPolicy(final String iamRoleAccountId, final String iamRoleName) {
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
        iamRoleUsageStatement.withId("Target IAM Role Has Decrypt Action");
        iamRoleUsageStatement.withPrincipals(
                new Principal(AWS_PROVIDER, String.format("arn:aws:iam::%s:role/%s", iamRoleAccountId, iamRoleName), false));
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
