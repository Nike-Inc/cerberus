# Cerberus

[![][travis img]][travis]
[![][coveralls img]][coveralls]
[![][license img]][license]

The Cerberus API is a cloud native, scalable Springboot application that can securely store application properties and files with robust auditing features.

Cerberus has an accessible user interface that offers teams there own self service portal for mapping various principals (Users and Applications) to what we call a Safe Deposit Box.

Safe Deposit Boxes can store properties (k,v pairs, json blobs, etc) and files (certificates, private key files, etc).

Cerberus has a robust versioning and audit features built in, so you can see who is doing what and revert data in an SDB if needed.

To learn more about Cerberus, please visit the [Cerberus website](http://engineering.nike.com/cerberus/).

## Getting Started for local development

### Configure Cerberus

Cerberus will look in `~/.cerberus/` for additional springboot configuration.
You can configure a `cerberus-local.yaml` file there that has your local specific conf.

See the [configuration section](#configuration) for details on required and optional configuration.

### Start Mysql

You need to configure and run MySQL locally

**MySQL Version 5.7** is required to run the application locally.

To get MySQL setup on OS X:

    $ brew install mysql@5.7
    $ mysql.server restart
    $ mysql_secure_installation

You'll need to create a database and user for it.  Run the following SQL against your mysql database:

    CREATE DATABASE IF NOT EXISTS cms;

    CREATE USER 'cms'@'localhost' IDENTIFIED BY '<YOUR DB PASSWORD HERE>';

    GRANT ALL ON cms.* TO 'cms'@'localhost';
    
### Ensure that you have AWS Credentials available

Ensure Credentials are available as outlined in the [AWS Java Credentials page](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html), we use the default provider chain.

For local development you can use a tool such as [gimme-aws-creds](https://github.com/Nike-Inc/gimme-aws-creds)

```bash
$ gimme-aws-creds --profile cerberus
Using password from keyring for justin.field@nike.com
Multi-factor Authentication required.
token:software:totp( GOOGLE ) : Justin.Field@nike.com selected
Enter verification code: 111111
writing role arn:aws:iam::111111111111:role/cerberus.admin.role to /Users/jfiel2/.aws/credentials
```
    
### Start Cerberus

Cerberus is a Spring boot application and this project makes use of the Springboot gradle plugin.
You can start cerberus with gradle

`./gradlew cerberus-web:bootRun`

You can start it with a remote debugger

`./gradlew cerberus-web:bootRun --debug-jvm`

You must build the dashboard once and after you make changes

`./gradlew cerberus-dashboard:buildDashboard cerberus-web:bootRun`

We have also including 2 convenience scripts that are nice because they give you pretty colors

```bash
./run.sh
```

This script builds the jar and starts the application listening but not breaking for a remote debugger on port 5006

```bash
./debug.sh
``` 

This script builds the jar and starts the application stopping automatically before spring initializes and waits for a remote debugger to attach on port 5006

## Configuration

Take a look at the [master configuration](cerberus-web/src/main/resources/cerberus.yaml), which contains all the available options and default values.
A reasonable approach would be to copy this file and place it ~/.cerberus/cerberus-${envName} and remove the default values you do not wish to override and configure any options you desire.

Remember that this is a Springboot app, so when you deploy it you can configure it like so.

```bash
LOG_DIR=/var/log/cerberus
LOG_OUT=${LOG_DIR}/stdout.log
LOG_ERR=${LOG_DIR}/stderr.log

# configure the jvm by using export JVM_BEHAVIOR_ARGS
. /path/to/some/file/that/does/advanced/jvm/config/

APP_SPECIFIC_JVM_ARGS="\
-Dspring.profiles.active=prod \
-Dspring.config.additional-location:/opt/cerberus/ \

java -jar \
    ${JVM_BEHAVIOR_ARGS} \
    ${APP_SPECIFIC_JVM_ARGS} \
    /opt/cerberus/cerberus-web.jar > ${LOG_OUT} 2> ${LOG_ERR}
```

In the above when the app starts it will look in the classpath and `/opt/cerberus/` for `cerberus.yml|yaml`, `cerberus-prod.yml|yaml`

## License

Cerberus Management Service is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

[travis]:https://travis-ci.org/Nike-Inc/cerberus
[travis img]:https://api.travis-ci.org/Nike-Inc/cerberus.svg?branch=master

[license]:LICENSE.txt
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg

[coveralls]:https://coveralls.io/github/Nike-Inc/cerberus
[coveralls img]:https://coveralls.io/repos/github/Nike-Inc/cerberus/badge.svg?branch=master
