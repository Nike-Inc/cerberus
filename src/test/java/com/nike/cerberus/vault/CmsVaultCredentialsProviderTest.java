package com.nike.cerberus.vault;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CmsVaultCredentialsProviderTest {

    @Test
    public void test_getCredentials_gives_expected_token() {
        String vaultToken = "vault-token";
        CmsVaultCredentialsProvider provider = new CmsVaultCredentialsProvider(vaultToken);
        assertEquals(vaultToken, provider.getCredentials().getToken());
    }

}