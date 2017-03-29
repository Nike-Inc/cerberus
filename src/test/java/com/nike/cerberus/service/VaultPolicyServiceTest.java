package com.nike.cerberus.service;

import com.nike.cerberus.util.Slugger;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.model.VaultPolicy;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class VaultPolicyServiceTest {

    String sdbName = "my-sdb-name";
    String sdbPath = "/secret/foo";
    String read = "read";


    Slugger slugger = new Slugger();
    VaultAdminClient vaultAdminClient;
    VaultPolicyService vaultPolicyService;

    @Before
    public void setup() {
        vaultAdminClient = mock(VaultAdminClient.class);
        vaultPolicyService = new VaultPolicyService(vaultAdminClient, slugger);
    }

    @Test
    public void test_buildPolicyName_read() {
        String policyName = vaultPolicyService.buildPolicyName(sdbName, read);
        assertEquals("my-sdb-name-read", policyName);
    }

    @Test
    public void test_buildPolicy_owner() {
        VaultPolicy policy = vaultPolicyService.buildPolicy(sdbPath, VaultPolicyService.OWNER_CAPABILITIES);
        assertEquals("path \"secret//secret/foo*\" { capabilities = [\"create\", \"read\", \"update\", \"delete\", \"list\"] }", policy.getRules());
    }

    @Test
    public void test_buildPolicy_read() {
        VaultPolicy policy = vaultPolicyService.buildPolicy(sdbPath, VaultPolicyService.READ_CAPABILITIES);
        assertEquals("path \"secret//secret/foo*\" { capabilities = [\"read\", \"list\"] }", policy.getRules());
    }

    @Test
    public void test_buildPolicy_write() {
        VaultPolicy policy = vaultPolicyService.buildPolicy(sdbPath, VaultPolicyService.WRITE_CAPABILITIES);
        assertEquals("path \"secret//secret/foo*\" { capabilities = [\"create\", \"read\", \"update\", \"delete\", \"list\"] }", policy.getRules());
    }

    @Test
    public void test_buildOwnerPolicy() {
        VaultPolicy policy = vaultPolicyService.buildOwnerPolicy(sdbPath);
        assertEquals("path \"secret//secret/foo*\" { capabilities = [\"create\", \"read\", \"update\", \"delete\", \"list\"] }", policy.getRules());
    }

    @Test
    public void test_buildWritePolicy() {
        VaultPolicy policy = vaultPolicyService.buildWritePolicy(sdbPath);
        assertEquals("path \"secret//secret/foo*\" { capabilities = [\"create\", \"read\", \"update\", \"delete\", \"list\"] }", policy.getRules());
    }

    @Test
    public void test_buildReadPolicy() {
        VaultPolicy policy = vaultPolicyService.buildReadPolicy(sdbPath);
        assertEquals("path \"secret//secret/foo*\" { capabilities = [\"read\", \"list\"] }", policy.getRules());
    }

}