package com.nike.cerberus.security;

import java.util.List;
import lombok.Getter;

public class AuthenticationNotRequiredWhitelist {

  public AuthenticationNotRequiredWhitelist(List<String> authenticationNotRequiredWhitelist) {
    this.authenticationNotRequiredWhitelist = authenticationNotRequiredWhitelist;
  }

  @Getter private final List<String> authenticationNotRequiredWhitelist;
}
