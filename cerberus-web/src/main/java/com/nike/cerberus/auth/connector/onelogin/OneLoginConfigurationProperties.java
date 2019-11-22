package com.nike.cerberus.auth.connector.onelogin;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class OneLoginConfigurationProperties {

  @NotBlank
  private String clientId;

  @NotBlank
  private String clientSecret;

  @NotBlank
  private String subDomain;

  @NotBlank(message = "You must specify the OneLogin API region, so that it can be used in the following template: 'https://api.${apiRegion}.onelogin.com'")
  private String apiRegion;
}
