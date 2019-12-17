package com.nike.cerberus.auth.connector.okta;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class OktaConfigurationProperties {
  @NotBlank // TODO this didn't work
  private String apiKey;
  @NotBlank
  private String baseUrl;
}
