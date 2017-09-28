package com.nike.cerberus.hystrix;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.CreateAliasRequest;
import com.amazonaws.services.kms.model.CreateAliasResult;
import com.amazonaws.services.kms.model.CreateKeyRequest;
import com.amazonaws.services.kms.model.CreateKeyResult;
import com.amazonaws.services.kms.model.DescribeKeyRequest;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.amazonaws.services.kms.model.EncryptResult;
import com.amazonaws.services.kms.model.GetKeyPolicyRequest;
import com.amazonaws.services.kms.model.GetKeyPolicyResult;
import com.amazonaws.services.kms.model.PutKeyPolicyRequest;
import com.amazonaws.services.kms.model.PutKeyPolicyResult;
import com.amazonaws.services.kms.model.ScheduleKeyDeletionRequest;
import com.amazonaws.services.kms.model.ScheduleKeyDeletionResult;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Hystrix wrapper around AWSKMSClient
 * <p>
 * Most of these commands should execute in their own Thread Pools because they have unique limits.
 * http://docs.aws.amazon.com/kms/latest/developerguide/limits.html#requests-per-second-table
 */
public class HystrixKmsClient extends AWSKMSClient {

    private static final String KMS = "KMS";

    private static final Logger LOGGER = LoggerFactory.getLogger(HystrixKmsClient.class);

    private final AWSKMSClient client;

    public HystrixKmsClient(AWSKMSClient client) {
        this.client = client;
    }

    public EncryptResult encrypt(EncryptRequest request) {
        // Default AWS limit was 1200 shared as of Aug 2017
        return execute("KmsEncryptDecrypt", "KmsEncrypt", () -> client.encrypt(request));
    }

    public CreateKeyResult createKey(CreateKeyRequest request) {
        // Default AWS limit was 5 as of Aug 2017
        return execute("KmsCreateKey", () -> client.createKey(request));
    }

    public CreateAliasResult createAlias(CreateAliasRequest request) {
        // Default AWS limit was 5 as of Aug 2017
        return execute("KmsCreateAlias", () -> client.createAlias(request));
    }

    public DescribeKeyResult describeKey(DescribeKeyRequest request) {
        // Default AWS limit was 30 as of Aug 2017
        return execute("KmsDescribeKey", () -> client.describeKey(request));
    }

    public ScheduleKeyDeletionResult scheduleKeyDeletion(ScheduleKeyDeletionRequest request) {
        // Default AWS limit was 5 as of Aug 2017
        return execute("KmsScheduleKeyDeletion", () -> client.scheduleKeyDeletion(request));
    }

    public GetKeyPolicyResult getKeyPolicy(GetKeyPolicyRequest request) {
        // Default AWS limit was 30 as of Aug 2017
        return execute("KmsGetKeyPolicy", () -> client.getKeyPolicy(request));
    }

    public PutKeyPolicyResult putKeyPolicy(PutKeyPolicyRequest request) {
        // Default AWS limit was 5 as of Aug 2017
        return execute("KmsPutKeyPolicy", () -> client.putKeyPolicy(request));
    }

    /**
     * Execute a function that returns a value in a ThreadPool unique to that command.
     */
    private static <T> T execute(String commandKey, Supplier<T> function) {
        return execute(commandKey, commandKey, function);
    }

    /**
     * Execute a function that returns a value in a specified ThreadPool
     */
    private static <T> T execute(String threadPoolName, String commandKey, Supplier<T> function) {
        try {
            return new HystrixCommand<T>(buildSetter(threadPoolName, commandKey)) {

                @Override
                protected T run() {
                    try {
                        return function.get();
                    } catch (AmazonServiceException e) {
                        if (e.getStatusCode() >= 400 && e.getStatusCode() < 500) {
                            // convert 4xx error codes to bad request
                            throw new HystrixBadRequestException(commandKey + " " + e.toString(), e);
                        } else {
                            throw e;
                        }
                    }
                }
            }.execute();
        } catch (HystrixRuntimeException | HystrixBadRequestException e) {
            LOGGER.error("commandKey: " + commandKey);
            if (e.getCause() instanceof RuntimeException) {
                // Convert back to the underlying exception type
                throw (RuntimeException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    private static HystrixCommand.Setter buildSetter(String threadPoolName, String commandKey) {
        return HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(KMS))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(threadPoolName))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
    }

}
