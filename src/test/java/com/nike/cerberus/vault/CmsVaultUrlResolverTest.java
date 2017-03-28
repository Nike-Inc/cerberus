package com.nike.cerberus.vault;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CmsVaultUrlResolverTest {

    @Test
    public void testResolve() {
        String vaultAddr = "vault-address";
        CmsVaultUrlResolver resolver = new CmsVaultUrlResolver(vaultAddr);
        assertEquals(vaultAddr, resolver.resolve());
    }

}