package com.nike.cerberus.domain;

import java.util.List;

public class AuthKmsKeyMetadataResult {
  private List<AuthKmsKeyMetadata> authenticationKmsKeyMetadata;

  public AuthKmsKeyMetadataResult() {}

  public AuthKmsKeyMetadataResult(List<AuthKmsKeyMetadata> authenticationKmsKeyMetadata) {
    this.authenticationKmsKeyMetadata = authenticationKmsKeyMetadata;
  }

  public List<AuthKmsKeyMetadata> getAuthenticationKmsKeyMetadata() {
    return authenticationKmsKeyMetadata;
  }

  public void setAuthenticationKmsKeyMetadata(
      List<AuthKmsKeyMetadata> authenticationKmsKeyMetadata) {
    this.authenticationKmsKeyMetadata = authenticationKmsKeyMetadata;
  }
}
