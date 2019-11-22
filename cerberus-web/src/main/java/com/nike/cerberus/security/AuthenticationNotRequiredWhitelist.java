package com.nike.cerberus.security;

import lombok.Getter;

import java.util.List;

public class AuthenticationNotRequiredWhitelist {

  public AuthenticationNotRequiredWhitelist(List<String> authenticationNotRequiredWhitelist) {
    this.authenticationNotRequiredWhitelist = authenticationNotRequiredWhitelist;
  }

  @Getter private final List<String> authenticationNotRequiredWhitelist;
}
