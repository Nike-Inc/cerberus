package com.nike.cerberus.hystrix;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.model.VaultAuthResponse;
import com.nike.vault.client.model.VaultClientTokenResponse;
import com.nike.vault.client.model.VaultListResponse;
import com.nike.vault.client.model.VaultPolicy;
import com.nike.vault.client.model.VaultTokenAuthRequest;

import java.util.function.Supplier;


/**
 * Hystrix wrapper for VaultAdminClient.
 */
@Singleton
public class HystrixVaultAdminClient {

    private static final String VAULT = "Vault";

    private final VaultAdminClient vaultAdminClient;

    @Inject
    public HystrixVaultAdminClient(final VaultAdminClient vaultAdminClient) {
        this.vaultAdminClient = vaultAdminClient;
    }

    public VaultAuthResponse createOrphanToken(VaultTokenAuthRequest vaultTokenAuthRequest) {
        return execute("VaultCreateOrphanToken", () -> vaultAdminClient.createOrphanToken(vaultTokenAuthRequest));
    }

    public VaultClientTokenResponse lookupToken(String vaultToken) {
        return execute("VaultLookupToken", () -> vaultAdminClient.lookupToken(vaultToken));
    }

    public void revokeOrphanToken(final String vaultToken) {
        execute("VaultRevokeOrphanToken", () -> vaultAdminClient.revokeOrphanToken(vaultToken));
    }

    public VaultListResponse list(final String path) {
        return execute("VaultList", () -> vaultAdminClient.list(path));
    }

    public void delete(final String path) {
        execute("VaultDelete", () -> vaultAdminClient.delete(path));
    }

    public void putPolicy(final String name, final VaultPolicy policy) {
        execute("VaultPutPolicy", () -> vaultAdminClient.putPolicy(name, policy));
    }

    public void deletePolicy(final String name) {
        execute("VaultDeletePolicy", () -> vaultAdminClient.deletePolicy(name));
    }

    /**
     * Execute a function that returns a value
     */
    private static <T> T execute(String commandKey, Supplier<T> function) {
        return new HystrixCommand<T>(buildSetter(commandKey)) {

            @Override
            protected T run() throws Exception {
                return function.get();
            }
        }.execute();
    }

    /**
     * Execute a function with void return type
     */
    private static void execute(String commandKey, Runnable function) {
        new HystrixCommand<Void>(buildSetter(commandKey)) {

            @Override
            protected Void run() throws Exception {
                function.run();
                return null;
            }
        }.execute();
    }

    private static HystrixCommand.Setter buildSetter(String commandKey) {
        return HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(VAULT))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
    }
}
