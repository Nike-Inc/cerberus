import React from 'react'
import { render } from 'react-dom'
import { Router, Route, IndexRoute, hashHistory } from 'react-router'
import { Provider } from 'react-redux'
import { syncHistoryWithStore } from 'react-router-redux'

import App from './components/App/App'
import LandingView from './components/LandingView/LandingView'
import ManageSDBox from './components/ManageSDBox/ManageSDBox'
import SDBMetadataList from './components/SDBMetadataList/SDBMetadataList'
import NotFound from './components/NotFound/NotFound'
import configureStore from './store/configureStore'
import { loginUserSuccess, handleSessionExpiration, setSessionWarningTimeout } from './actions/authenticationActions'
import { getLogger } from 'logger'
import './assets/styles/reactSelect.scss'

var log = getLogger('main')

/**
 * This is our redux data store for storing all data retrieved from API Calls and any other state that needs
 * to be maintained.
 */
const store = configureStore(window.__INITIAL_STATE__)

/**
 * Grab token from session storage
 */
let token = JSON.parse(sessionStorage.getItem('token'))

// use session token to register user as logged in
if (token != null && token != "") {
    let dateString = sessionStorage.getItem('tokenExpiresDate')
    let tokenExpiresDate = new Date(dateString)
    let now = new Date()
    
    log.debug(`Token expires on ${tokenExpiresDate}`)

    let dateTokenExpiresInMillis = tokenExpiresDate.getTime() - now.getTime()

    // warn two minutes before token expiration
    store.dispatch(setSessionWarningTimeout(dateTokenExpiresInMillis - 120000, token.data.client_token.client_token))

    let authTokenTimeoutId = setTimeout(() => {
        store.dispatch(handleSessionExpiration())
    }, dateTokenExpiresInMillis)

    store.dispatch(loginUserSuccess(token, authTokenTimeoutId))
}

// Create an enhanced history that syncs navigation events with the store
const history = syncHistoryWithStore(hashHistory, store)

/**
 * The Provider makes the dispatch method available to children components that connect to it.
 * The dispatcher is used to fire off actions such as a button being clicked, or submitting a form.
 *
 * Once an action fires off a reducer is triggered to manipulate the data in the store to change the state of
 * this app.
 *
 * Components that connect to the piece of state that has changed will have there inputs updated.
 *
 * This is an implementation of FLUX.
 */
render(
    <div>
        <Provider store={store}>
            <div>
                <Router history={history}>
                    <Route path='/' component={App}>
                        <IndexRoute component={LandingView}/>
                        <Route path='manage-safe-deposit-box/:id' component={ManageSDBox}/>
                        <Route path='admin/sdb-metadata' component={SDBMetadataList}/>
                        <Route path='*' component={NotFound} />
                    </Route>
                    <Route path='*' component={NotFound} />
                </Router>
            </div>
        </Provider>
    </div>,
    document.getElementById('root')
)