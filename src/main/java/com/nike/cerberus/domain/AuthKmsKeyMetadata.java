package com.nike.cerberus.domain;

import java.time.OffsetDateTime;

public class AuthKmsKeyMetadata {

    private String awsIamRoleArn;
    private String awsKmsKeyId;
    private String awsRegion;
    private OffsetDateTime createdTs;
    private OffsetDateTime lastUpdatedTs;
    private OffsetDateTime lastValidatedTs;

    public String getAwsIamRoleArn() {
        return awsIamRoleArn;
    }

    public AuthKmsKeyMetadata setAwsIamRoleArn(String awsIamRoleArn) {
        this.awsIamRoleArn = awsIamRoleArn;
        return this;
    }

    public String getAwsKmsKeyId() {
        return awsKmsKeyId;
    }

    public AuthKmsKeyMetadata setAwsKmsKeyId(String awsKmsKeyId) {
        this.awsKmsKeyId = awsKmsKeyId;
        return this;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public AuthKmsKeyMetadata setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
        return this;
    }

    public OffsetDateTime getCreatedTs() {
        return createdTs;
    }

    public AuthKmsKeyMetadata setCreatedTs(OffsetDateTime createdTs) {
        this.createdTs = createdTs;
        return this;
    }

    public OffsetDateTime getLastUpdatedTs() {
        return lastUpdatedTs;
    }

    public AuthKmsKeyMetadata setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
        this.lastUpdatedTs = lastUpdatedTs;
        return this;
    }

    public OffsetDateTime getLastValidatedTs() {
        return lastValidatedTs;
    }

    public AuthKmsKeyMetadata setLastValidatedTs(OffsetDateTime lastValidatedTs) {
        this.lastValidatedTs = lastValidatedTs;
        return this;
    }
}
