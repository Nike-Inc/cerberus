# Cerberus Management Service

[![][travis img]][travis]
[![Coverage Status](https://coveralls.io/repos/github/Nike-Inc/cerberus-management-service/badge.svg?branch=master)](https://coveralls.io/github/Nike-Inc/cerberus-management-service)
[![][license img]][license]

The Cerberus Management Service (CMS) is a core component of the Cerberus [REST API](http://engineering.nike.com/cerberus/docs/architecture/rest-api)
that facilities user and AWS IAM role authentication and the management of Safe Deposit Boxes (SDBs), an abstraction on top of Hashicorp's Vault.

To learn more about Cerberus, please visit the [Cerberus website](http://engineering.nike.com/cerberus/).

## Getting Started

### Running with persistent data,

If you wish to persist data permanently you can install Vault and MySQL locally

**MySQL** is required to run the application locally.

To get MySQL setup on OS X:

    $ brew install mysql
    $ mysql.server restart
    $ mysql_secure_installation

You'll need to create a database and user for it.  Run the following SQL against your mysql database:

    CREATE DATABASE IF NOT EXISTS cms;

    CREATE USER 'cms'@'localhost' IDENTIFIED BY '<YOUR DB PASSWORD HERE>';

    GRANT ALL ON cms.* TO 'cms'@'localhost';

## Configuration

### Configurable Properties

There are a few parameters that need to be configured for CMS to run properly, they are defined in this table.

property                                            | required | notes
---------------------------                         | -------- | ----------
JDBC.url                                            | Yes      | The JDBC url for the mysql db
JDBC.username                                       | Yes      | The JDBC user name for the mysql db
JDBC.password                                       | Yes      | The JDBC JDBC.password for the mysql db
root.user.arn                                       | Yes      | The arn for the root AWS user, needed to make the KMS keys deletable.
admin.role.arn                                      | Yes      | The arn for an AWS user, needed to make the KMS keys deletable.
cms.role.arn                                        | Yes      | The arn for the Instance profile for CMS instances, so they can admin KMS keys that they create.
cms.admin.group                                     | Yes      | Group that user can be identified by to get admin privileges, currently this just enables users to access `/v1/metadata` see API.md
cms.admin.roles                                     | No       | Comma separated list of ARNs that can auth and access admin endpoints.
cms.auth.connector                                  | Yes      | The user authentication connector implementation to use for user auth.
cms.user.token.ttl                                  | No       | By default user tokens are created with a TTL of 1h, you can override that with this param
cms.iam.token.ttl                                   | No       | By default IAM tokens are created with a TTL of 1h, you can override that with this param
cms.kms.policy.validation.interval.millis.override  | No       | By default CMS validates KMS key policies no more than once per minute, you can override that with this param
cms.auth.token.hash.salt                            | Yes      | The string value which CMS will use to salt auth tokens
cms.encryption.cmk.arns                             | Yes      | Development AWS KMS CMK ARNs for use in local encryption of secrets

KMS Policies are bound to IAM Principal IDs rather than ARNs themselves. Because of this, we validate the policy at authentication time
to ensure that if an IAM role has been deleted and re-created, that we grant access to the new principal ID.
The API limit for this call is low, so the `cms.kms.policy.validation.interval.millis.override` property is used to throttle this validation.

For local dev see the `Development` section.

For deployed environments they are configured via the CLI, which will generate a props file and stuff it into S3 encrypted with KMS.

    cerberus --debug \
    -e demo \
    -r us-west-2 \
    create-cms-config \
    --admin-group cerberus-admins \
    -P cms.auth.connector=com.nike.cerberus.auth.connector.onelogin.OneLoginAuthConnector \
    -P auth.connector.onelogin.api_region=us \
    -P auth.connector.onelogin.client_id=$ONE_LOGIN_CLIENT_ID \
    -P auth.connector.onelogin.client_secret=$ONE_LOGIN_CLIENT_SECRET \
    -P auth.connector.onelogin.subdomain=nike

See [Creating an environment](http://engineering.nike.com/cerberus/docs/administration-guide/creating-an-environment) for more information.

CMS will download the props file at startup time and load the props into Guice.

### User Authentication

#### Auth Connector Interface

The User authentication contract is defined by the [AuthConnector](https://github.com/Nike-Inc/cerberus-management-service/blob/master/src/main/java/com/nike/cerberus/auth/connector/AuthConnector.java) interface.  

The only included implementation of this interface targets
OneLogin.  We expect to implement more connectors in the near future.

##### OneLogin Auth Connector

property                              | required | notes
------------------------------------- | -------- | ----------
cms.auth.connector                    | Yes      | com.nike.cerberus.auth.connector.onelogin.OneLoginAuthConnector
auth.connector.onelogin.api_region    | Yes      | `us` or `eu`
auth.connector.onelogin.client_id     | Yes      | The OneLogin API client id
auth.connector.onelogin.client_secret | Yes      | The OneLogin API client secret
auth.connector.onelogin.subdomain     | Yes      | Your orgs OneLogin subdomain [xxxxx].onelogin.com

**Assumption: The current implementation looks up group membership for a user via the member_of field on the getUserById API response.**

##### Okta Auth Connector

Multi-factor authentication is only enabled if it is required in Okta for the authenticating user.

property                              | required | notes
------------------------------------- | -------- | ----------
cms.auth.connector                    | Yes      | com.nike.cerberus.auth.connector.okta.OktaAuthConnector
auth.connector.okta.api_key           | Yes      | The Okta API key
auth.connector.okta.base_url          | Yes      | The Okta base url (e.g. `"https://example.okta.com"` or `"https://example.oktapreview.com"`)

##### Okta MFA Auth Connector

Multi-factor authentication is enabled for all users, even if it is not required in Okta.

property                              | required | notes
------------------------------------- | -------- | ----------
cms.auth.connector                    | Yes      | com.nike.cerberus.auth.connector.okta.OktaMFAAuthConnector
auth.connector.okta.api_key           | Yes      | The Okta API key
auth.connector.okta.base_url          | Yes      | The Okta base url (e.g. `"https://example.okta.com"` or `"https://example.oktapreview.com"`)

## Development

First, a few properties must be configured in `src/main/resources/cms-local-overrides.conf`

You'll need a few pieces of information before you can run the application:

- The DB password you setup earlier
- The group that identifies which users are administrators
- The root user ARN for your AWS account
- The AWS IAM role ARN that represents administrators and CMS instances
- The authentication connector class that is used to authenticate users and get their group membership
- The string value which CMS will use to salt auth tokens
- Development AWS KMS CMK ARNs for use in local encryption of secrets
```
    # Database connection details.
    JDBC.url="jdbc:mysql://localhost:3306/cms?useUnicode=true&characterEncoding=utf8&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false"
    JDBC.username="cms"
    JDBC.password="<YOUR DB PASSWORD HERE>"

    # Group that user can be identified by to get admin privileges.
    cms.admin.group="<YOUR ADMIN GROUP>"

    # AWS ARNs used when setting up KMS keys for IAM role authentication.
    root.user.arn="arn:aws:iam::<YOUR AWS ACCOUNT ID>:root"
    admin.role.arn="arn:aws:iam::<YOUR AWS ACCOUNT ID>:role/<YOUR IAM ROLE FOR ADMINS>"
    cms.role.arn="arn:aws:iam::<YOUR AWS ACCOUNT ID>:role/<YOUR IAM ROLE FOR CMS>"

    # Auth Connector
    cms.auth.connector=<YOUR AUTH CONNECTOR CLASS>

    # Encryption
    cms.auth.token.hash.salt=changeMe 
    cms.encryption.cmk.arns="arn:aws:kms:<AWS REGION>:<YOUR AWS ACCOUNT ID>:key/<KEY ID 1>,arn:aws:kms:<AWS REGION>:<YOUR AWS ACCOUNT ID>:key/<KEY ID 2>"
```

### Dashboard

To debug/test changes to the dashboard, run each of the following tasks in new command line terminals (each are blocking tasks).

Using Embedded MySQL:

1. `gradlew startMysqlAndDashboard`
    - Starts an embedded instance of MySql in the background
    - Starts the Dashboard as a blocking process 
    - This task needs to be run as Admin in Windows, ensure that you start the IDE or Terminals as Admin
    - Once you see `successfully started MySQL and Dashboard` proceed to next step
1. `gradlew runCMS`
    - Starts CMS as a blocking process
    - To debug, use the `debugCMS` gradle task instead and attach remote debugger to port 5005
    - You will need to make sure your env is set as described http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
    - Now you should have a complete CMS system running locally.

Above should work on Windows, Mac, and Linux.

Using local instance of MySQL server:

1. If setting up for the first time:
    1.  `mysql` -- Run this command as an admin in your Terminal
    1.  Run the following in the `mysql` console:
    ```
        CREATE DATABASE IF NOT EXISTS cms;
        CREATE USER 'cms'@'localhost' IDENTIFIED BY '<YOUR PASSWORD HERE>';
        GRANT ALL ON cms.* TO 'cms'@'localhost’;
    ```
1. `mysql.server start`
    - Starts MySql server
1. `gradlew runCMS`
    - Starts CMS as a blocking process
    - To debug, use the `debugCMS` gradle task instead and attach remote debugger to port 5005
    - You will need to make sure your env is set as described http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
    - Now you should have a complete CMS system running locally.
1. `gradlew runDashboardAndReverseProxy`
    - Runs the dashboard and reverse proxy to interact with CMS, which sometimes better than curling or using postman.
    - Runs an express server and reverse proxy to expose `http://localhost:9001/dashboard/`
    - By default this command runs the dashboard through Webpack. You can run the Dashboard from the CMS jar by changing the `loadDashboardFromCms` variable in `dashboard/server.js` to true

Above should work on Windows, Mac, and Linux.

### Cerberus Management Service (CMS)

To debug/test changes to CMS, run each of the following tasks in new command line terminals (each are blocking tasks).

Using Embedded MySQL:

1. `gradlew startMysqlAndCms`
    - Starts an embedded instance of MySql in the background
    - Starts CMS as a blocking process 
    - This task needs to be run as Admin in Windows, ensure that you start the IDE or Terminals as Admin
    - Once you see `successfully started MySQL and CMS` proceed to next step

Above should work on Windows, Mac, and Linux.

Using local instance of MySQL server:

1. If setting up for the first time:
    1.  `mysql` -- Run this command as an admin in your Terminal
    1.  Run the following in the `mysql` console:
    ```
        CREATE DATABASE IF NOT EXISTS cms;
        CREATE USER 'cms'@'localhost' IDENTIFIED BY '<YOUR PASSWORD HERE>';
        GRANT ALL ON cms.* TO 'cms'@'localhost’;
    ```
1. `mysql.server start`
    - Starts MySql server
1. `gradlew runCMS`
    - Starts CMS as a blocking process
    - To debug, use the `debugCMS` gradle task instead and attach remote debugger to port 5005
    - You will need to make sure your env is set as described http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
    - Now you should have a complete CMS system running locally.
    - Navigate to localhost:8080/dashboard in your browser to log in

Above should work on Windows, Mac, and Linux.

### From the CLI

With Vault and MySQL running

    ./gradlew clean build

    ./debugShadowJar.sh -Dvault.addr=http://localhost:8200 -Dvault.token=<token>


## Setting up your IDE

Import the build.gradle file.

## API documentation

See [API.md](API.md)

## License

Cerberus Management Service is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

[travis]:https://travis-ci.org/Nike-Inc/cerberus-management-service
[travis img]:https://api.travis-ci.org/Nike-Inc/cerberus-management-service.svg?branch=master

[license]:LICENSE.txt
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg
