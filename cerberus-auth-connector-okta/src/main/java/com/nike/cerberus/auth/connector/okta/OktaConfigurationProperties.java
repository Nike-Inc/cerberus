package com.nike.cerberus.auth.connector.okta;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OktaConfigurationProperties {
  @NotBlank // TODO this didn't work
  private String apiKey;
  @NotBlank private String baseUrl;
}
