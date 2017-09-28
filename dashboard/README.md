# Cerberus Management Dashboard

This project is a self-service web UI for administration of [Safe Deposit Boxes (SDBs)](http://engineering.nike.com/cerberus/docs/architecture/vault#safe-deposit-box-sdb),
access control, and data in Cerberus. It is implemented as a [React](https://facebook.github.io/react/) single-page application (SPA).

To learn more about the dashboard and view screenshots, please see the [documentation](http://engineering.nike.com/cerberus/docs/user-guide/dashboard).

To learn more about Cerberus, please visit the [Cerberus website](http://engineering.nike.com/cerberus/).

## Development

This project has a couple scripts that are integrated into NPM tasks that enable running the Cerberus stack that resides behind the router locally.
The `npm run dev*` tasks will start the locally webpack server and configure a reverse proxy to point at vault that the scripts will start and bootstrap for you.
The Reverse proxy will point at a local [Cerberus Management Service (CMS)](https://github.com/Nike-Inc/cerberus-management-service).

Instructions assume development machine is MacOS with [brew](http://brew.sh/) installed.

### Steps to run Dashboard locally with CMS

1. Follow the [instructions in the CMS project](https://github.com/Nike-Inc/cerberus-management-service)
1. Optional but recommended install [multitail](https://www.vanheusden.com/multitail/) with `brew install multitail`
   1. Optionally tail the logs, e.g. `multitail logs/*`
1. Open the Dashboard in a browser, e.g. `open http://localhost:9000/dashboard/`
1. Login with actual credentials based on the configured AuthProvider for CMS 
1. Start development
   1. Note: Hot Module reloading is on so changes to code will auto refresh the app
1. Optionally connect a remote debugger from CMS project using default settings in IntelliJ `-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005`


### Troubleshooting

* Delete CMS database if Vault data is out of sync