# Cerberus Management Service

A REST API to manage secrets and access to said secrets in the Cerberus system.

The `X-Cerberus-Client` header is for clients to self-report their name and version to Cerberus.  It is currently
optional but may be required in the future.

More information about using the API can be found in the various [clients](http://engineering.nike.com/cerberus/components/#clients)
as well as in the [integration tests](https://github.com/Nike-Inc/cerberus-integration-tests/) project.

# Group Authentication

## User Login [/v2/auth/user]

### Authenticate with Cerberus as a User [GET]

This endpoint will take a Users credentials, validate them with configured Auth Connector (e.g. Okta, OneLogin), and then generate a token.

+ Request (application/json)

    + Headers

            Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/json)

    + Body

            {
               "status": "success",
               "data": {
                   "client_token": {
                       "client_token": "AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a",
                       "policies": [
                           "web",
                           "stage"
                       ],
                       "metadata": {
                           "username": "john.doe@nike.com",
                           "is_admin": "false",
                           "groups": "Lst-CDT.CloudPlatformEngine.FTE,Lst-digital.platform-tools.internal",
                           "refresh_count": 1,
                           "max_refresh_count": 24
                       },
                       "lease_duration": 3600,
                       "renewable": true
                   }
               }
            }

+ Response 200 (application/json) (MFA Required)

    + Body

            {
              "status" : "mfa_req",
              "data" : {
                "user_id" : "13427265",
                "username" : "john.doe@nike.com",
                "state_token" : "5c7d1fd1914ffff5bcc2253b3c38ef85a3125bc1",
                "devices" : [ {
                  "id" : "111111",
                  "name" : "Google Authenticator"
                }, {
                  "id" : "22222",
                  "name" : "Google Authenticator"
                }, {
                  "id" : "33333",
                  "name" : "Google Authenticator"
                } ],
                "client_token" : null
              }
            }

+ Response 401 (application/json)

    + Body

            {
                "error_id":"ccc1cc1c-e111-11e1-11ce-111e11a111f1",
                "errors": [
                    {
                        "code":99106,
                        "message":"Invalid credentials"
                    }
                ]
            }

## User MFA Check [/v2/auth/mfa_check]

### Verify MFA token for a user [POST]

If the configured Auth Connector (e.g. Okta, OneLogin) requires Multi-Factor Authentication, this endpoint is used during the second step of the authentication process.

+ Request (application/json)

    + Headers

            X-Cerberus-Client: MyClientName/1.0.0

    + Body

            {
                "state_token": "jskljdklaj",
                "device_id": "123456",
                "otp_token": "111111"
            }

+ Response 200 (application/json)

    + Body

            {
               "status": "success",
               "data": {
                   "client_token": {
                       "client_token": "AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a",
                       "policies": [
                           "web",
                           "stage"
                       ],
                       "metadata": {
                           "username": "john.doe@nike.com",
                           "is_admin": "false",
                           "groups": "Lst-CDT.CloudPlatformEngine.FTE,Lst-digital.platform-tools.internal",
                           "refresh_count": 1,
                           "max_refresh_count": 24
                       },
                       "lease_duration": 3600,
                       "renewable": true
                   }
               }
            }

## User Refresh Token [/v2/auth/user/refresh]

### Refresh the user's token [GET]

This endpoint allows a user to exchange their current token for a new one with updated policies.
There is a limit to the number of times this call can be made with a token and is store in the metadata
refresh_count and max_refresh_count can be used to determine when a re-authentication is required.

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/json)

    + Body

            {
               "status": "success",
               "data": {
                   "client_token": {
                       "client_token": "AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a",
                       "policies": [
                           "web",
                           "stage"
                       ],
                       "metadata": {
                           "username": "john.doe@nike.com",
                           "is_admin": "false",
                           "groups": "Lst-CDT.CloudPlatformEngine.FTE,Lst-digital.platform-tools.internal",
                           "refresh_count": 2,
                           "max_refresh_count": 24
                       },
                       "lease_duration": 3600,
                       "renewable": true
                   }
               }
            }

## App Login v2 [/v2/auth/iam-principal]

### Authenticate with Cerberus as an App [POST]

This endpoint takes IAM ARN information and generates a base64 encoded KMS encrypted payload. 

+ Request (application/json)

    + Headers

            X-Cerberus-Client: MyClientName/1.0.0

    + Body

            {
                "iam_principal_arn" : "arn:aws:iam::111111111:role/cerberus-api-tester",
                "region": "us-west-2"
            }

+ Response 200 (application/json)
    + The response will be a simple JSON payload with the encrypted data
            {
                "auth_data": "xxxxxxxxxxxxxxxxxxxxx-long-encrypted-string-xxxxxxxxxxxxxxxxxxxxxxxxxxxx"
            }
    + Once you have the encrypted string, you need to make a call to AWS Key Management Service (KMS) to decrypt the response. The decrypted response will contain the body below with the token needed to access Cerberus
    + Body

            {
              "client_token" : "AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a",
              "policies" : [ "foo-bar-read", "lookup-self" ],
              "metadata" : {
                "aws_region" : "us-west-2",
                "iam_principal_arn" : "arn:aws:iam::111111111:role/fake-role"
                "username" : "arn:aws:iam::111111111:role/fake-role"
                "is_admin": "false",
                "groups": "registered-iam-principals"
              },
              "lease_duration" : 3600,
              "renewable" : true
            }


## App Login [/v1/auth/iam-role]

### Authenticate with Cerberus as an App [POST]

This endpoint takes IAM ARN information and generates an base 64 encoded KMS encrypted payload of the below. The ARN if registered with an SDB will have kms decrypt permissions on the KMS key that the payload was encrypted with.

+ Request (application/json)

    + Headers

            X-Cerberus-Client: MyClientName/1.0.0

    + Body

            {
                "account_id" : "123456789012",
                "role_name": "web",
                "region": "us-west-2"
            }

+ Response 200 (application/json)

    + The response will be a simple JSON payload with the encrypted data
            {
                "auth_data": "xxxxxxxxxxxxxxxxxxxxx-long-encrypted-string-xxxxxxxxxxxxxxxxxxxxxxxxxxxx"
            }
    + Once you have the encrypted string, you need to make a call to AWS Key Management Service (KMS) to decrypt the response. The decrypted response will contain the body below with the token needed to access Cerberus
    + Body

            {
              "client_token" : "AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a",
              "policies" : [ "health-check-bucket-read", "lookup-self" ],
              "metadata" : {
                "aws_region" : "us-west-2",
                "aws_account_id" : "111111111",
                "aws_iam_role_name" : "fake-role",
                "username" : "arn:aws:iam::111111111:role/fake-role",
                "is_admin": "false",
                "groups": "registered-iam-principals"
              },
              "lease_duration" : 3600,
              "renewable" : true
            }


## Auth [/v1/auth]

### Logout of Cerberus [DELETE]

This endpoint will take the users `X-Cerberus-Token` header and revoke the token.

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 204 (application/json)

# Group Safe Deposit Box

## Safe Deposit Box V2 [/v2/safe-deposit-box]

### Get details for each authorized Safe Deposit Box [GET]

This endpoint will list all the Safe Deposit Box a user is authorized to see.

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/json)

    + Body

            [
                {
                    "id": "fb013540-fb5f-11e5-ba72-e899458df21a",
                    "name": "Web",
                    "path": "app/web",
                    "category_id": "f7ff85a0-faaa-11e5-a8a9-7fa3b294cd46"
                },
                {
                     "id": "06f82494-fb60-11e5-ba72-e899458df21a",
                     "name": "OneLogin",
                     "path": "shared/onelogin",
                     "category_id": "f7ffb890-faaa-11e5-a8a9-7fa3b294cd46"
                }
            ]

### Create a Safe Deposit Box [POST]

This endpoint will create a new Safe Deposit Box

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

    + Body

            {
                "name": "Stage",
                "description": "Sensitive configuration properties for the stage micro-service.",
                "category_id": "f7ff85a0-faaa-11e5-a8a9-7fa3b294cd46",
                "owner": "Lst-digital.platform-tools.internal",
                "user_group_permissions": [
                    {
                      "name": "Lst-CDT.CloudPlatformEngine.FTE",
                      "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ],
                "iam_principal_permissions": [
                    {
                        "iam_principal_arn": ""arn:aws:iam::1111111111:role/role-name"
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ]
            }

+ Response 201 (application/json)

    + Headers

            X-Refresh-Token: true
            Location: /v1/safe-deposit-box/a7d703da-faac-11e5-a8a9-7fa3b294cd46

    + Body

            {
                "id": "a7d703da-faac-11e5-a8a9-7fa3b294cd46",
                "name": "Stage",
                "description": "Sensitive configuration properties for the stage micro-service.",
                "path": "app/stage",
                "category_id": "f7ff85a0-faaa-11e5-a8a9-7fa3b294cd46",
                "owner": "Lst-digital.platform-tools.internal",
                "user_group_permissions": [
                    {
                        "id": "3fc6455c-faad-11e5-a8a9-7fa3b294cd46",
                        "name": "Lst-CDT.CloudPlatformEngine.FTE",
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ],
                "iam_principal_permissions": [
                    {
                        "id": "d05bf72e-faad-11e5-a8a9-7fa3b294cd46",
                        "iam_principal_arn": "arn:aws:iam::1111111111:role/role-name",
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ]
            }

### Get details for a specific authorized Safe Deposit Box [GET /v2/safe-deposit-box/{id}]

This endpoint returns details on a specific Safe Deposit Box.

+ Parameters

    + id (required, string, `a7d703da-faac-11e5-a8a9-7fa3b294cd46`) - The id of the Safe Deposit Box

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/json)

    + Body

            {
                "id": "a7d703da-faac-11e5-a8a9-7fa3b294cd46",
                "name": "Stage",
                "description": "Sensitive configuration properties for the stage micro-service.",
                "path": "app/stage",
                "category_id": "f7ff85a0-faaa-11e5-a8a9-7fa3b294cd46",
                "owner": "Lst-digital.platform-tools.internal",
                "user_group_permissions": [
                    {
                        "id": "3fc6455c-faad-11e5-a8a9-7fa3b294cd46",
                        "name": "Lst-CDT.CloudPlatformEngine.FTE",
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ],
                "iam_principal_permissions": [
                    {
                        "id": "d05bf72e-faad-11e5-a8a9-7fa3b294cd46",
                        "iam_principal_arn": "arn:aws:iam::1111111111:role/role-name",
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ]
            }


### Update a specific authorized Safe Deposit Box [PUT]

This endpoint allows a user to update the description, user group, and iam role mappings

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

    + Body

            {
                "description": "All configuration properties for the stage micro-service.",
                "owner": "Lst-Squad.Carebears",
                "user_group_permissions": [
                    {
                        "name": "Lst-CDT.CloudPlatformEngine.FTE",
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ],
                "iam_principal_permissions": [
                    {
                        "iam_principal_arn": ""arn:aws:iam::1111111111:role/role-name2"
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ]
            }

+ Response 200

    + Headers

            X-Refresh-Token: true

    + Body

            {
                "id": "a7d703da-faac-11e5-a8a9-7fa3b294cd46",
                "name": "Stage",
                "description": "Sensitive configuration properties for the stage micro-service.",
                "path": "app/stage",
                "category_id": "f7ff85a0-faaa-11e5-a8a9-7fa3b294cd46",
                "owner": "Lst-digital.platform-tools.internal",
                "user_group_permissions": [
                    {
                        "id": "3fc6455c-faad-11e5-a8a9-7fa3b294cd46",
                        "name": "Lst-CDT.CloudPlatformEngine.FTE",
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ],
                "iam_principal_permissions": [
                    {
                        "id": "d05bf72e-faad-11e5-a8a9-7fa3b294cd46",
                        "iam_principal_arn": "arn:aws:iam::1111111111:role/role-name",
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ]
            }

### Delete a specific authorized Safe Deposit Box [DELETE]

This endpoint allows a user to delete a safe deposit box that they own

+ Parameters

    + id (required, string, `a7d703da-faac-11e5-a8a9-7fa3b294cd46`) - The id of the Safe Deposit Box

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200

    + Headers

            X-Refresh-Token: true

## Safe Deposit Box V1 [/v1/safe-deposit-box]

## Get details for each authorized Safe Deposit Box [GET]

This endpoint will list all the Safe Deposit Box a user is authorized to see.

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/json)

    + Body

            [
                {
                    "id": "fb013540-fb5f-11e5-ba72-e899458df21a",
                    "name": "Web",
                    "path": "app/web",
                    "category_id": "f7ff85a0-faaa-11e5-a8a9-7fa3b294cd46"
                },
                {
                     "id": "06f82494-fb60-11e5-ba72-e899458df21a",
                     "name": "OneLogin",
                     "path": "shared/onelogin",
                     "category_id": "f7ffb890-faaa-11e5-a8a9-7fa3b294cd46"
                }
            ]

### Create a Safe Deposit Box [POST]

This endpoint will create a new Safe Deposit Box

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

    + Body

            {
                "name": "Stage",
                "description": "Sensitive configuration properties for the stage micro-service.",
                "category_id": "f7ff85a0-faaa-11e5-a8a9-7fa3b294cd46",
                "owner": "Lst-digital.platform-tools.internal",
                "user_group_permissions": [
                    {
                      "name": "Lst-CDT.CloudPlatformEngine.FTE",
                      "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ],
                "iam_role_permissions": [
                    {
                        "account_id": "123",
                        "iam_role_name": "stage",
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ]
            }

+ Response 201 (application/json)

    + Headers

            X-Refresh-Token: true
            Location: /v1/safe-deposit-box/a7d703da-faac-11e5-a8a9-7fa3b294cd46

    + Body

            {
                "id": "a7d703da-faac-11e5-a8a9-7fa3b294cd46"
            }


### Get details for a specific authorized Safe Deposit Box [GET /v1/safe-deposit-box/{id}]

This endpoint returns details on a specific Safe Deposit Box.

+ Parameters

    + id (required, string, `a7d703da-faac-11e5-a8a9-7fa3b294cd46`) - The id of the Safe Deposit Box

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/json)

    + body

            {
                "id": "a7d703da-faac-11e5-a8a9-7fa3b294cd46",
                "name": "Stage",
                "description": "Sensitive configuration properties for the stage micro-service.",
                "path": "app/stage",
                "category_id": "f7ff85a0-faaa-11e5-a8a9-7fa3b294cd46",
                "owner": "Lst-digital.platform-tools.internal",
                "user_group_permissions": [
                    {
                        "id": "3fc6455c-faad-11e5-a8a9-7fa3b294cd46",
                        "name": "Lst-CDT.CloudPlatformEngine.FTE",
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ],
                "iam_role_permissions": [
                    {
                        "id": "d05bf72e-faad-11e5-a8a9-7fa3b294cd46",
                        "account_id": "123",
                        "iam_role_name" :"stage",
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ]
            }

### Update a specific authorized Safe Deposit Box [PUT]

This endpoint allows a user to update the description, user group, and iam role mappings.

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

    + Body

            {
                "description": "All configuration properties for the stage micro-service.",
                "owner": "Lst-Squad.Carebears",
                "user_group_permissions": [
                    {
                        "name": "Lst-CDT.CloudPlatformEngine.FTE",
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ],
                "iam_role_permissions": [
                    {
                        "account_id": "123",
                        "iam_role_name" :"stage",
                        "role_id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46"
                    }
                ]
            }

+ Response 204

    + Headers

            X-Refresh-Token: true

### Delete a specific authorized Safe Deposit Box [DELETE]

This endpoint allows a user to delete a safe deposit box that they own.

+ Parameters

    + id (required, string, `a7d703da-faac-11e5-a8a9-7fa3b294cd46`) - The id of the Safe Deposit Box

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200

    + Headers

            X-Refresh-Token: true

# Secrets

## List Paths [v1/secret/{category}/{sdb-name}/{path}?list=true]

### List Paths [GET]

When listing paths, if a path ends with a '/' then it is a virtual path, i.e. a path that contains no
data but can be listed to further find additional sub-paths.  Calling GET on a virtual path without the 
list=true parameter will return 404.

+ Request (application/json)

    + Headers

        X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
        X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/json)

    + Body

        {
          "request_id" : "bbdd111111a2aaa3a",
          "lease_id" : "",
          "renewable" : false,
          "lease_duration" : 3600,
          "data" : {
            "keys" : [ "path1", "path2", "virtual-path1/" ]
          },
          "wrap_info" : null,
          "warnings" : null,
          "auth" : null
        }

## Secrets [v1/secret/{category}/{sdb-name}/{path}]

### Read Secrets at a path [GET]

Calling GET on a virtual path without the list=true parameter will return 404.

+ Request (application/json)

    + Headers

        X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
        X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/json)

    + Body

        {
          "request_id" : "aa11aaa1-1111-1a1a-1aa1-a1aa11aaa1a1",
          "lease_id" : "",
          "renewable" : false,
          "lease_duration" : 3600,
          "data" : {
            "password" : "secret",
            "username" : "someuser"
          },
          "wrap_info" : null,
          "warnings" : null,
          "auth" : null
        }
        
+ Response 403 (application/json)

    + Body

        {
            "errors": [
                "permission denied"
            ]
        }

+ Response 404 (application/json)

    + Body

        {
            "errors": []
        }

### Create/Update Secrets at a path [POST]

+ Request (application/json)

    + Headers

        X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
        X-Cerberus-Client: MyClientName/1.0.0

    + Body

        {
            "key1": "value1",
            "key2": "value2"
        }

+ Response 204

### Delete Secrets at a path [DELETE]

+ Request

    + Headers

        X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
        X-Cerberus-Client: MyClientName/1.0.0

+ Response 204

# Group Role

## Role List [/v1/role]

### Retrieve the role list [GET]

Lists all the roles that can be granted to an IAM Role or User Group on a Safe Deposit Box, e.g. owner, write, read.

+ Request

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/json)

    + Body

            [
                {
                    "id": "f7fff4d6-faaa-11e5-a8a9-7fa3b294cd46",
                    "name": "owner",
                    "created_ts": "2016-04-05T04:19:51Z",
                    "last_updated_ts": "2016-04-05T04:19:51Z",
                    "created_by": "system",
                    "last_updated_by": "system"
                },
                {
                    "id": "f80027ee-faaa-11e5-a8a9-7fa3b294cd46",
                    "name": "write",
                    "created_ts": "2016-04-05T04:19:51Z",
                    "last_updated_ts": "2016-04-05T04:19:51Z",
                    "created_by": "system",
                    "last_updated_by": "system"
                },
                {
                    "id": "f800558e-faaa-11e5-a8a9-7fa3b294cd46",
                    "name": "read",
                    "created_ts": "2016-04-05T04:19:51Z",
                    "last_updated_ts": "2016-04-05T04:19:51Z",
                    "created_by": "system",
                    "last_updated_by": "system"
                }
            ]

# Group Category

## Category List [/v1/category]

### Retrieve the category list [GET]

Lists all the possible categories that a safe deposit box can belong to.  By default there are two categories:

1. Applications - for SDBs owned by a single application.
1. Shared - for SDBs that are shared between many applications or groups.

SDBs under different categories have no functional difference. They are simply an organizational mechanism.

+ Request

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/json)

    + Body

            [
                {
                    "id": "f7ff85a0-faaa-11e5-a8a9-7fa3b294cd46",
                    "display_name": "Applications",
                    "path": "app",
                    "created_ts": "2016-04-05T04:19:51Z",
                    "last_updated_ts": "2016-04-05T04:19:51Z",
                    "created_by": "system",
                    "last_updated_by": "system"
                },
                {
                    "id": "f7ffb890-faaa-11e5-a8a9-7fa3b294cd46",
                    "display_name": "Shared",
                    "path": "shared",
                    "created_ts": "2016-04-05T04:19:51Z",
                    "last_updated_ts": "2016-04-05T04:19:51Z",
                    "created_by": "system",
                    "last_updated_by": "system"
                }
            ]


# Group Admin Endpoints

## SDB Metadata [/v1/metadata?limit={limit}&offset={offset}]

### Get metadata [GET]

Returns pageable metadata for all SDBs.
You can use has_next and next_offset from the response to paginate through all records.  
This endpoint does not return any secret data but can be used by Cerberus admins to look-up the contact information for an SDB.

+ Parameters
    + limit (number) - OPTIONAL: The number of records to include in the metadata result. Defaults to 100
    + offset (number) - OPTIONAL: The offset to use when paginating records. Defaults to 0

+ Request

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/json)
        
    + Body
    
            {
                "has_next": false,
                "next_offset": 0,
                "limit": 10,
                "offset": 0,
                "sdb_count_in_result": 3,
                "total_sdbcount": 3,
                "safe_deposit_box_metadata": [
                    {
                        "name": "dev demo",
                        "path": "app/dev-demo/",
                        "category": "Applications",
                        "owner": "Lst-Squad.Carebears",
                        "description": "test",
                        "created_ts": "2017-01-04T23:18:40-08:00",
                        "created_by": "justin.field@nike.com",
                        "last_updated_ts": "2017-01-04T23:18:40-08:00",
                        "last_updated_by": "justin.field@nike.com",
                        "user_group_permissions": {
                            "Application.FOO.User": "read"
                        },
                        "iam_role_permissions": {
                            "arn:aws:iam::265866363820:role/asdf": "write"
                        }
                    },
                    {
                        "name": "nike dev foo bar",
                        "path": "app/nike-dev-foo-bar/",
                        "category": "Applications",
                        "owner": "Lst-Squad.Carebears",
                        "description": "adsfasdfadsfasdf",
                        "created_ts": "2017-01-04T23:19:03-08:00",
                        "created_by": "justin.field@nike.com",
                        "last_updated_ts": "2017-01-04T23:19:03-08:00",
                        "last_updated_by": "justin.field@nike.com",
                        "user_group_permissions": {
                            "Lst-FOO-bar": "read"
                        },
                        "iam_role_permissions": {}
                    },
                    {
                        "name": "IaM W d WASD",
                        "path": "shared/iam-w-d-wasd/",
                        "category": "Shared",
                        "owner": "Lst-Squad.Carebears",
                        "description": "CAREBERS",
                        "created_ts": "2017-01-04T23:19:19-08:00",
                        "created_by": "justin.field@nike.com",
                        "last_updated_ts": "2017-01-04T23:19:19-08:00",
                        "last_updated_by": "justin.field@nike.com",
                        "user_group_permissions": {},
                        "iam_role_permissions": {}
                    }
                ]
            }

## Trigger Scheduled Job [/v1/admin/trigger-job/{job}]

### Trigger Scheduled Job [POST]

Manually trigger a job, e.g. ExpiredTokenCleanUpJob, HystrixMetricsProcessingJob, KmsKeyCleanUpJob, KpiMetricsProcessingJob.
A 400 response code is given if the job wasn't found.

+ Request

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 204

+ Response 400 (application/json)

    + Body

        {
            "error_id": "1111111c-cc1a-11a1-11b1-1a1c1c1a1a11",
            "errors": [
                {
                    "code": 99999,
                    "message": "Request will not be completed."
                }
            ]
        }

## Healthcheck [/healthcheck]

### Healthcheck [GET]

+ Response 204