package com.nike.cerberus.jwt;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

@Data
@SuppressFBWarnings(justification = "It's just a POJO.")
/**
 * https://tools.ietf.org/html/rfc7517 Only supports RSA private key for now
 * https://tools.ietf.org/html/rfc7517#section-9.3 not parsing alg or sig because optional
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
