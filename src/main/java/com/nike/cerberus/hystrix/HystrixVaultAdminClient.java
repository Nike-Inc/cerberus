package com.nike.cerberus.hystrix;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandGroupKey.Factory;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.model.*;

/**
 * Hystrix wrapper for VaultAdminClient.
 */
@Singleton
public class HystrixVaultAdminClient {

    private final VaultAdminClient vaultAdminClient;

    private static Setter setter(String commandKey) {
        Setter setter = Setter.withGroupKey(Factory.asKey("vault"))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionTimeoutInMilliseconds(10000));
        return setter;
    }

    @Inject
    public HystrixVaultAdminClient(final VaultAdminClient vaultAdminClient) {
        this.vaultAdminClient = vaultAdminClient;
    }

    public VaultAuthResponse createOrphanToken(VaultTokenAuthRequest vaultTokenAuthRequest) {
        return new HystrixCommand<VaultAuthResponse>(setter("createOrphanToken")) {

            @Override
            protected VaultAuthResponse run() throws Exception {
                Thread.sleep(100);
                return vaultAdminClient.createOrphanToken(vaultTokenAuthRequest);
            }
        }.execute();
    }

    public VaultClientTokenResponse lookupToken(String vaultToken) {
        return new HystrixCommand<VaultClientTokenResponse>(setter("lookupToken")) {

            @Override
            protected VaultClientTokenResponse run() throws Exception {
                return vaultAdminClient.lookupToken(vaultToken);
            }
        }.execute();
    }

    public void revokeOrphanToken(final String vaultToken) {
        new HystrixCommand<Void>(setter("revokeOrphanToken")) {

            @Override
            protected Void run() throws Exception {
                vaultAdminClient.revokeOrphanToken(vaultToken);
                return null;
            }
        }.execute();
    }

    public VaultListResponse list(final String path) {
        return new HystrixCommand<VaultListResponse>(setter("list")) {

            @Override
            protected VaultListResponse run() throws Exception {
                Thread.sleep(200);
                return vaultAdminClient.list(path);
            }
        }.execute();
    }

    public void delete(final String path) {
        new HystrixCommand<Void>(setter("delete")) {

            @Override
            protected Void run() throws Exception {
                vaultAdminClient.delete(path);
                return null;
            }
        }.execute();
    }

    public void putPolicy(final String name, final VaultPolicy policy) {
        new HystrixCommand<Void>(setter("putPolicy")) {

            @Override
            protected Void run() throws Exception {
                vaultAdminClient.putPolicy(name, policy);
                return null;
            }
        }.execute();
    }

    public void deletePolicy(final String name) {
        new HystrixCommand<Void>(setter("deletePolicy")) {

            @Override
            protected Void run() throws Exception {
                vaultAdminClient.deletePolicy(name);
                return null;
            }
        }.execute();
    }
}
