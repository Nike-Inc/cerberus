package com.nike.cerberus.jwt;

import lombok.Data;

@Data
/**
 * https://tools.ietf.org/html/rfc7517 Only supports RSA private key for now
 * https://tools.ietf.org/html/rfc7517#section-9.3 not parse alg or sig because optional
 */
public class JsonWebKeySet {
  private JsonWebKey[] keys;

  @Data
  static class JsonWebKey {
    private String kid;
    private String n;
    private String e;
  }
}
