package com.nike.cerberus.auth.connector.onelogin;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OneLoginConfigurationProperties {

  @NotBlank private String clientId;

  @NotBlank private String clientSecret;

  @NotBlank private String subDomain;

  @NotBlank(
      message =
          "You must specify the OneLogin API region, so that it can be used in the following template: 'https://api.${apiRegion}.onelogin.com'")
  private String apiRegion;
}
