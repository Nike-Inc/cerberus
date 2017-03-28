package com.nike.cerberus.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KmsPolicyServiceTest {

    @Test
    public void testGenerateStandardKmsPolicy() {

        String rootUserArn = "root-arn";
        String adminRoleArn = "admin-role-arn";
        String cmsRoleArn = "cms-role-arn";
        String iamAccountId = "1234567890";
        String iamRoleName = "my-role-name";

        KmsPolicyService kmsPolicyService = new KmsPolicyService(rootUserArn, adminRoleArn, cmsRoleArn);

        // invoke method under test
        String actualResult = kmsPolicyService.generateStandardKmsPolicy(iamAccountId, iamRoleName);

        String expectedResult = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Sid\":\"Root User Has All Actions\",\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"root-arn\"},\"Action\":[\"kms:*\"],\"Resource\":[\"*\"]},{\"Sid\":\"Admin Role Has All Actions\",\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"admin-role-arn\"},\"Action\":[\"kms:*\"],\"Resource\":[\"*\"]},{\"Sid\":\"CMS Role Key Access\",\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"cms-role-arn\"},\"Action\":[\"kms:Encrypt\",\"kms:Decrypt\",\"kms:ReEncrypt*\",\"kms:GenerateDataKey*\",\"kms:DescribeKey\"],\"Resource\":[\"*\"]},{\"Sid\":\"Target IAM Role Has Decrypt Action\",\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws:iam::1234567890:role/my-role-name\"},\"Action\":[\"kms:Decrypt\"],\"Resource\":[\"*\"]}]}";

        assertEquals(expectedResult, actualResult);
    }

}