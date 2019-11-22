# Cerberus API Tests

All the tests require that the following environment variable be set

Environment Variable | Description
-------------------- | ------------------
CERBERUS_API_URL     | The Cerberus API URL to Test

When caching is enabled, you may also configure the following environment variable to get around caching

Environment Variable | Default Value | Description
-------------------- | ------------------
SLEEP_IN_MILLISECONDS     | 0             | Milliseconds to sleep before executing frequently used API actions

The tests also require that you manually set up an integration test SDB in the environment you are testing.
(TODO This can probably be automated away with TestNG before suite)

The SDB needs to be configured as follows

Field       | Value
----------- | -----------------------------------------------------------------
Name        | Cerberus Integration Tests SDB 
Category    | Applications
Owner       | some-group-you-belong-to
Description | This SDB is used for integration testing, do not delete this SDB

User Group Permissions

User Group                      | Role
------------------------------- | -------
some-group-your-auto-user-is-in | write


IAM Role Permissions

IAM Principal ARN                              | Role
---------------------------------------------- | -----
arn:aws:iam::1111111:role/cerberus-api-tester  | write


Secure Data (add the following Secure Data, after the SDB is created)

Field       | Value
----------- | -----------------------------------------------------------------
Path        | default-test-secret
Key         | default-test-key
Value       | default-test-value

This sub module contains API tests that can validate the composed Cerberus API

### IAM Principal API Tests

This is a series of tests that validate that IAM authenticated Cerberus principals can interact with the Cerberus API 
in the manner that is expected

The following environment variables are required to run this test

Environment Variable | Description
---------------------|---------------------------
TEST_ACCOUNT_ID      | The account id to use when authenticating with Cerberus using the IAM Auth endpoint
TEST_ROLE_NAME       | The role name to use when authenticating with Cerberus using the IAM Auth endpoint
TEST_REGION          | The region to use when authenticating with Cerberus using the IAM Auth endpoint

You can run this only these tests with the following command

    CERBERUS_API_URL=http://127.0.0.1:9000 \
    TEST_ACCOUNT_ID=11111111 \
    TEST_ROLE_NAME=cerberus-api-tester \
    TEST_REGION=us-west-2 \
    gradlew clean -Dtest.single=CerberusIamApiTests cerberus-api-tests:test

### IAM Principal API V2 Tests

This is a series of tests that validate that IAM authenticated Cerberus principals can interact with the Cerberus API
in the manner that is expected

The following environment variables are required to run this test

Environment Variable | Description
TEST_ACCOUNT_ID      | The account id to use when authenticating with Cerberus using the IAM Auth endpoint
TEST_ROLE_NAME       | The role name to use when authenticating with Cerberus using the IAM Auth endpoint
TEST_REGION          | The region to use when authenticating with Cerberus using the IAM Auth endpoint

You can run this only these tests with the following command

    CERBERUS_API_URL=http://127.0.0.1:9000 \
    TEST_ACCOUNT_ID=11111111 \
    TEST_ROLE_NAME=cerberus-api-tester \
    TEST_REGION=us-west-2 \
    gradlew clean -Dtest.single=CerberusIamApiV2Tests cerberus-api-tests:test

### User Principal API Tests

This is a series of tests that validate that user authenticated Cerberus principals can interact with the Cerberus API 
in the manner that is expected

The following environment variables are required to run this test

Environment Variable    | Description
----------------------- | ------------------------------------------------------------------
TEST_USER_EMAIL         | The email address for a test user for testing user based endpoints
TEST_USER_PASSWORD      | The password for a test user for testing user based endpoints
TEST_USER_OTP_SECRET    | The secret for the test users OTP MFA (OTP == Google auth)
TEST_USER_OTP_DEVICE_ID | The device id for the test users OTP MFA (OTP == Google auth)

You can run this only these tests with the following command

    CERBERUS_API_URL=http://127.0.0.1:9000 \
    TEST_USER_EMAIL=cerberus-automated-test-user@nike.com \
    TEST_USER_PASSWORD=${PASSWORD} \
    TEST_USER_OTP_SECRET=${OTP_SECRET} \
    TEST_USER_OTP_DEVICE_ID=111111 \
    gradlew clean -Dtest.single=CerberusUserApiTests cerberus-api-tests:test

### Admin API Tests

This is a series of tests that validate that an admin can call admin-specific endpoints

Note: These tests are disabled by default and need to be enabled and triggered manually

The following environment variables are required to run this test

Environment Variable        | Description
TEST_ADMIN_CERBERUS_TOKEN   | A Cerberus auth token with permissions to call admin endpoints

You can run this only these tests with the following command

    CERBERUS_API_URL=http://127.0.0.1:9000 \
    TEST_ADMIN_CERBERUS_TOKEN=0000-0000-0000-0000
    gradlew clean -Dtest.single=AdminApiTests cerberus-api-tests:test

### Invalid Auth API Tests

This is a series of tests that neither users nor IAM roles can make calls without a valid auth token  

You can run this only these tests with the following command

    CERBERUS_API_URL=http://127.0.0.1:9000 \
    gradlew clean -Dtest.single=FailedAuthenticationApiTests cerberus-api-tests:test

### Negative User Permissions API Tests

This is a series of tests that validate read and write users cannot perform actions in Cerberus beyond what is allowed
by their role permissions

The following environment variables are required to run this test

Environment Variable    | Description
----------------------- | ------------------------------------------------------------------
TEST_ACCOUNT_ID         | The account id to use when authenticating with Cerberus using the IAM Auth endpoint
TEST_ROLE_NAME          | The role name to use when authenticating with Cerberus using the IAM Auth endpoint
TEST_REGION             | The region to use when authenticating with Cerberus using the IAM Auth endpoint
TEST_USER_EMAIL         | The email address for a test user for testing user based endpoints
TEST_USER_PASSWORD      | The password for a test user for testing user based endpoints
TEST_USER_OTP_SECRET    | The secret for the test users OTP MFA (OTP == Google auth)
TEST_USER_OTP_DEVICE_ID | The device id for the test users OTP MFA (OTP == Google auth)

**Note: In these tests, the test user is given limited permissions to the SDB, while the IAM principal retains
(ownership) permission to delete/clean up any test artifacts created in Cerberus. Thus both IAM principal and user
credentials are required

You can run this only these tests with the following command

    CERBERUS_API_URL=http://127.0.0.1:9000 \
    TEST_ACCOUNT_ID=11111111 \
    TEST_ROLE_NAME=cerberus-api-tester \
    TEST_REGION=us-west-2 \
    TEST_USER_EMAIL=cerberus-automated-test-user@nike.com \
    TEST_USER_PASSWORD=${PASSWORD} \
    TEST_USER_OTP_SECRET=${OTP_SECRET} \
    TEST_USER_OTP_DEVICE_ID=111111 \
    gradlew clean -Dtest.single=NegativeUserPermissionsApiTests cerberus-api-tests:test

### Negative IAM Permissions API Tests

This is a series of tests that validate read and write IAM principals cannot perform actions in Cerberus beyond what is
allowed by their role permissions

The following environment variables are required to run this test

Environment Variable    | Description
----------------------- | ------------------------------------------------------------------
TEST_ACCOUNT_ID         | The account id to use when authenticating with Cerberus using the IAM Auth endpoint
TEST_ROLE_NAME          | The role name to use when authenticating with Cerberus using the IAM Auth endpoint
TEST_REGION             | The region to use when authenticating with Cerberus using the IAM Auth endpoint
TEST_USER_EMAIL         | The email address for a test user for testing user based endpoints
TEST_USER_PASSWORD      | The password for a test user for testing user based endpoints
TEST_USER_OTP_SECRET    | The secret for the test users OTP MFA (OTP == Google auth)
TEST_USER_OTP_DEVICE_ID | The device id for the test users OTP MFA (OTP == Google auth)

**Note: In these tests, the test IAM principal is given limited permissions to the SDB, while the user retains
(ownership) permission to delete/clean up any test artifacts created in Cerberus. Thus both IAM principal and user
credentials are required

You can run this only these tests with the following command

    CERBERUS_API_URL=http://127.0.0.1:9000 \
    TEST_ACCOUNT_ID=11111111 \
    TEST_ROLE_NAME=cerberus-api-tester \
    TEST_REGION=us-west-2 \
    TEST_USER_EMAIL=cerberus-automated-test-user@nike.com \
    TEST_USER_PASSWORD=${PASSWORD} \
    TEST_USER_OTP_SECRET=${OTP_SECRET} \
    TEST_USER_OTP_DEVICE_ID=111111 \
    gradlew clean -Dtest.single=NegativeIamPermissionsApiTests cerberus-api-tests:test