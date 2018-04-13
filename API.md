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

    + Body

            {
                "auth_data": "xxxxxxxxxxxxxxxxxxxxx-base64-encoded-KMS-encrypted-string-xxxxxxxxxxxxxxxxxxxxxxxxxxxx"
            }

    + The response will be a simple JSON payload with the encrypted data. Once you have the encrypted string, you need to make a call to AWS Key Management Service (KMS) to decrypt the response. The decrypted response will contain the body below with the token needed to access Cerberus

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

    + Body

            {
                "auth_data": "xxxxxxxxxxxxxxxxxxxxx-base64-encoded-KMS-encrypted-string-xxxxxxxxxxxxxxxxxxxxxxxxxxxx"
            }

    + The response will be a simple JSON payload with the encrypted data. Once you have the encrypted string, you need to make a call to AWS Key Management Service (KMS) to decrypt the response. The decrypted response will contain the body below with the token needed to access Cerberus

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

## Files [v1/secure-file/{PATH}]

### Read Secure File at a path [GET]

Calling GET on a virtual path without the list=true parameter will return 404.

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/octet-stream)
    
    + Headers

            Content-Length: 14895
            Content-Disposition: attachment; filename="cacerts"
            Content-Type: application/octet-stream

    + Body

        [14895 bytes of binary data]
            
        
+ Response 401 (application/json)

    + Body

            {
              "error_id": "9c4dc9de-2ce2-4b55-9bda-dbd8e2397879",
              "errors": [
                {
                  "code": 99105,
                  "message": "X-Vault-Token or X-Cerberus-Token header is malformed or invalid."
                }
              ]
            }

+ Response 404 (application/json)

    + Body

            {
              "error_id": "6b13cdaa-ce64-473d-9228-5cf9bf0e51a9",
              "errors": [
                {
                  "code": 99996,
                  "message": "Not found"
                }
              ]
            }

### [GET] Read secure file version at a path [v1/secure-file/{PATH}?versionId={VERSION_ID}]

Gets a specific version of a secret from the change history stored in Cerberus 

+ Parameters

    + path: category/sdb-slug/path/to/secret1 (String) - The path to the secret for which to get versions

+ Query Parameters

    + versionId: 1234-4567-8903-0098-7543 (String) - The ID of the desired secret version

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/octet-stream)
    
    + Headers

            Content-Length: 14895
            Content-Disposition: attachment; filename="cacerts"
            Content-Type: application/octet-stream

    + Body

        [14895 bytes of binary data]

+ Response 401 (application/json)

    + Body

            {
              "error_id": "9c4dc9de-2ce2-4b55-9bda-dbd8e2397879",
              "errors": [
                {
                  "code": 99105,
                  "message": "X-Vault-Token or X-Cerberus-Token header is malformed or invalid."
                }
              ]
            }

+ Response 404 (application/json)

    + Body

            {
              "error_id": "6b13cdaa-ce64-473d-9228-5cf9bf0e51a9",
              "errors": [
                {
                  "code": 99996,
                  "message": "Not found"
                }
              ]
            }

### Create secure file at a path [POST]

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0
            Content-Type: multipart/form-data; boundary=----------4123509835381001

    + Body
            
            ------------4123509835381001
            Content-Disposition: form-data; name="file-content"; filename="foo.pem"
            Content-Type: application/x-pem-file
 
 
            PEM87a.............,...........D..;
            ------------4123509835381001

+ Response 204


+ Response 401 (application/json)

    + Body

            {
              "error_id": "9c4dc9de-2ce2-4b55-9bda-dbd8e2397879",
              "errors": [
                {
                  "code": 99105,
                  "message": "X-Vault-Token or X-Cerberus-Token header is malformed or invalid."
                }
              ]
            }

+ Response 404 (application/json)

    + Body

            {
              "error_id": "6b13cdaa-ce64-473d-9228-5cf9bf0e51a9",
              "errors": [
                {
                  "code": 99996,
                  "message": "Not found"
                }
              ]
            }


### Get secure file metadata [HEAD]

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200

    + Headers

            Content-Length: 14895
            Content-Disposition: attachment; filename="cacerts"
            Content-Type: application/octet-stream

+ Response 401 (application/json)

    + Body

            {
              "error_id": "9c4dc9de-2ce2-4b55-9bda-dbd8e2397879",
              "errors": [
                {
                  "code": 99105,
                  "message": "X-Vault-Token or X-Cerberus-Token header is malformed or invalid."
                }
              ]
            }

+ Response 404 (application/json)

    + Body

            {
              "error_id": "6b13cdaa-ce64-473d-9228-5cf9bf0e51a9",
              "errors": [
                {
                  "code": 99996,
                  "message": "Not found"
                }
              ]
            }

### [GET] List secure file summaries [v1/secure-files]

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200

    + Body

            {
                "has_next": false,
                "next_offset": null,
                "limit": 100,
                "offset": 0,
                "file_count_in_result": 3,
                "total_file_count": 3,
                file_summaries: [
                    {
                        "sdb_id": "0000-0000-0000-0000",
                        "path": "/category/sdb-slug/ssh.pem",
                        "name": "ssh.pem",
                        "size_in_bytes": "1983",
                        "created_by": "user0@example.com",
                        "created_ts": "2016-04-05T04:19:51Z",
                        "last_updated_by": "user@example.com"
                        "last_updated_ts": "2016-04-05T05:19:59Z",
                    },
                    {
                        "sdb_id": "0000-0000-0000-0000",
                        "path": "/category/sdb-slug/cacerts",
                        "name": "cacerts",
                        "size_in_bytes": "1002",
                        "created_by": "user0@example.com",
                        "created_ts": "2016-04-05T04:19:51Z",
                        "last_updated_by": "user@example.com"
                        "last_updated_ts": "2016-04-05T05:19:59Z",
                    },
                    {
                        "sdb_id": "0000-0000-0000-0000",
                        "path": "/category/sdb-slug/bar.privatekey",
                        "name": "bar.privatekey",
                        "size_in_bytes": "2087",
                        "created_by": "user0@example.com",
                        "created_ts": "2016-04-05T04:19:51Z",
                        "last_updated_by": "user@example.com"
                        "last_updated_ts": "2016-04-05T05:19:59Z",
                    }
                ]
            }


+ Response 401 (application/json)

    + Body

            {
              "error_id": "9c4dc9de-2ce2-4b55-9bda-dbd8e2397879",
              "errors": [
                {
                  "code": 99105,
                  "message": "X-Vault-Token or X-Cerberus-Token header is malformed or invalid."
                }
              ]
            }

+ Response 404 (application/json)

    + Body

            {
              "error_id": "6b13cdaa-ce64-473d-9228-5cf9bf0e51a9",
              "errors": [
                {
                  "code": 99996,
                  "message": "Not found"
                }
              ]
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
              "auth" : null,
              "metadata" : {}
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

### [GET] Read secret version at a path [v1/secret/{PATH}?versionId={VERSION_ID}]

Gets a specific version of a secret from the change history stored in Cerberus 

+ Parameters

    + path: category/sdb-slug/path/to/secret1 (String) - The path to the secret for which to get versions

+ Query Parameters

    + versionId: 1234-4567-8903-0098-7543 (String) - The ID of the desired secret version

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
                   "username" : "anotheruser"
                 },
                 "wrap_info" : null,
                 "warnings" : null,
                 "auth" : null,
                 "metadata" : {
                     "version_id": "1234-4567-8903-0098-7543",      
                     "action": "UPDATE",
                     "version_created_by": "user0@example.com",
                     "version_created_ts": "2016-04-05T04:19:51Z",
                     "action_principal": "user@example.com"
                     "action_ts": "2016-04-05T05:19:59Z"
                 }
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

# Versions

## SDB Secret Version Paths

### [GET] Secret Version Paths for an SDB [v1/sdb-secret-version-paths/{SDB_ID}]

Gets a list of paths for secrets that have a change history in Cerberus

** Note: This endpoint lists version metadata for all secret types (e.g. key-value objects, and files)

+ Parameters

    + sdbId: 0000-0000-0000-0000 (String) - The ID of the SDB for which to get versions


+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/json)

    + Body

             [
                 "/category/sdb-slug/path/to/secret1",
                 "/category/sdb-slug/path/to/secret2"
             ]

## Secrets Versioning

### [GET] Secrets Versions [v1/secret-versions/{PATH}?limit={LIMIT}&offset={OFFSET}]

Gets a list of version metadata for the secret at the given path.

** Note: This endpoint lists version metadata for all secret types (e.g. key-value objects, and files)

+ Parameters

    + path: category/sdb-slug/path/to/secret1 (String) - The path to the secret for which to get versions

+ Query Parameters

    + limit (number) - OPTIONAL: The number of records to include in the metadata result. Defaults to 100
    + offset (number) - OPTIONAL: The offset to use when paginating records. Defaults to 0

+ Request (application/json)

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

+ Response 200 (application/json)

    + Body

            {
                "has_next": false,
                "next_offset": null,
                "limit": 100,
                "offset": 0,
                "version_count_in_result": 4,
                "total_version_count": 4,
                "secure_data_version_summaries": [
                    {
                        "id": "1234-4567-8903-0098-7543",
                        "sdb_id": "0000-0000-0000-0000",
                        "path": "/category/sdb-slug/path/to/secret1",
                        "action": "UPDATE",
                        "type": "OBJECT",
                        "size_in_bytes": "12983",
                        "version_created_by": "user0@example.com",
                        "version_created_ts": "2016-04-05T04:19:51Z",
                        "action_principal": "user@example.com"
                        "action_ts": "2016-04-05T05:19:59Z",
                    },
                    {
                        "id": "4567-8903-0098-7543-1234",
                        "sdb_id": "0000-0000-0000-0000",
                        "path": "/category/sdb-slug/path/to/secret1",
                        "action": "UPDATE",
                        "type": "OBJECT",
                        "size_in_bytes": "12654",
                        "version_created_by": "user@example.com",
                        "version_created_ts": "2016-04-05T04:19:51Z",
                        "action_principal": "admin@example.com"
                        "action_ts": "2016-03-23T02:32:10Z",
                    },
                    {
                        "id": "8903-0098-7543-1234-4567",
                        "sdb_id": "0000-0000-0000-0000",
                        "path": "/category/sdb-slug/path/to/secret1",
                        "action": "UPDATE",
                        "type": "OBJECT",
                        "size_in_bytes": "1987",
                        "version_created_by": "user2@example.com",
                        "version_created_ts": "2016-04-05T04:19:51Z",
                        "action_principal": "guest@example.com"
                        "action_ts": "2016-02-13T12:05:09Z",
                    },
                    {
                        "id": "CURRENT",
                        "sdb_id": "0000-0000-0000-0000",
                        "path": "/category/sdb-slug/path/to/secret1",
                        "action": "CREATE",
                        "type": "OBJECT",
                        "size_in_bytes": "3874",
                        "version_created_by": "user@example.com",
                        "version_created_ts": "2016-04-05T04:19:51Z",
                        "action_principal": "user@example.com"
                        "action_ts": "2016-04-05T04:19:51Z",
                    }
                ]
            }


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

## Restore a safe deposit box [v1/restore-sdb]

### [PUT]

    + Headers

            X-Cerberus-Token: AaAAAaaaAAAabCdEF0JkLMNZ01iGabcdefGHIJKLtClQabcCVabEYab1aDaZZz12a
            X-Cerberus-Client: MyClientName/1.0.0

    + Body

            {
                "name":"example",
                "path":"app/example/",
                "category":"Applications",
                "owner":"Owner",
                "description":"This is the description",
                "created_ts":"2016-04-12T12:55:41Z",
                "created_by":"user@example.com",
                "last_updated_ts":"2016-08-09T12:41:21Z",
                "last_updated_by":"guest@example.com",
                "user_group_permissions":{
                    "ReadUsers":"read"
                },
                "iam_role_permissions":{},
                "data":{
                    "category/sdb_name/path":{
                        "key1":"value1"
                    }
                }
            }

+ Response 204 No Content

## Healthcheck [/healthcheck]

### Healthcheck [GET]

+ Response 200

    + Body

            CMS is running Wed Jan 17 10:17:51 PST 2018
