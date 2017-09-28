package com.nike.cerberus.hystrix;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.model.VaultAuthResponse;
import com.nike.vault.client.model.VaultClientTokenResponse;
import com.nike.vault.client.model.VaultListResponse;
import com.nike.vault.client.model.VaultPolicy;
import com.nike.vault.client.model.VaultTokenAuthRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;


/**
 * Hystrix wrapper for VaultAdminClient.
 */
@Singleton
public class HystrixVaultAdminClient {

    private static final String VAULT = "Vault";

    private static final Logger LOGGER = LoggerFactory.getLogger(HystrixVaultAdminClient.class);
    private final VaultAdminClient vaultAdminClient;

    @Inject
    public HystrixVaultAdminClient(final VaultAdminClient vaultAdminClient) {
        this.vaultAdminClient = vaultAdminClient;
    }

    public VaultAuthResponse createOrphanToken(VaultTokenAuthRequest vaultTokenAuthRequest) {
        return execute("VaultCreateOrphanToken", () -> {
            try {
                return vaultAdminClient.createOrphanToken(vaultTokenAuthRequest);
            } catch (Exception e) {
                LOGGER.warn("createOrphanToken failed, retrying...", e);
                return vaultAdminClient.createOrphanToken(vaultTokenAuthRequest);
            }
        });
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
        try {
            return new HystrixCommand<T>(buildSetter(commandKey)) {

                @Override
                protected T run() {
                    return function.get();
                }
            }.execute();
        } catch (HystrixRuntimeException e) {
            LOGGER.error("commandKey:" + commandKey);
            if (e.getCause() instanceof RuntimeException) {
                // Convert back to the underlying exception type
                throw (RuntimeException) e.getCause();
            } else {
                throw e;
            }
        }

    }

    /**
     * Execute a function with void return type
     */
    private static void execute(String commandKey, Runnable function) {
        try {
            new HystrixCommand<Void>(buildSetter(commandKey)) {

                @Override
                protected Void run() {
                    function.run();
                    return null;
                }
            }.execute();
        } catch (HystrixRuntimeException e) {
            LOGGER.error("commandKey:" + commandKey, e);
            if (e.getCause() instanceof RuntimeException) {
                // Convert back to the underlying exception type
                throw (RuntimeException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    private static HystrixCommand.Setter buildSetter(String commandKey) {
        return HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(VAULT))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
    }
}
