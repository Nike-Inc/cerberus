export var simulateLogin = () => {
    sessionStorage.setItem('token', JSON.stringify({
        "client_token": "7f6808f1-ede3-2177-aa9d-45f507391310",
        "policies": [
            "web",
            "stage"
        ],
        "metadata": {
            "username": "john.doe@nike.com",
            "is_admin": "false",
            "groups": "Lst-CDT.CloudPlatformEngine.FTE,Lst-digital.platform-tools.internal"
        },
        "lease_duration": 3600,
        "renewable": true
    }))
}