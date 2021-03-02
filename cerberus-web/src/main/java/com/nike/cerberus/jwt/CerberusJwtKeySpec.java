package com.nike.cerberus.jwt;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

public class CerberusJwtKeySpec extends SecretKeySpec {
    //todo maybe implement destroyable
    private String kid;

    public CerberusJwtKeySpec(byte[] key, String algorithm, String kid) {
        super(key, algorithm);
        this.kid = kid;
    }

    public CerberusJwtKeySpec(SecretKey secretKey, String kid) {
        super(secretKey.getEncoded(), secretKey.getAlgorithm());
        this.kid = kid;
    }

    public String getKid() {
        return kid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CerberusJwtKeySpec keySpec = (CerberusJwtKeySpec) o;
        return kid.equals(keySpec.kid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), kid);
    }
}