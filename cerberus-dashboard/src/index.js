/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from "react";
import { render } from "react-dom";
// import { Router, Route, IndexRoute, hashHistory } from "react-router";
import { Route, Router, Switch } from 'react-router-dom';
import { createBrowserHistory } from 'history';
import { Provider } from "react-redux";
import { syncHistoryWithStore } from "react-router-redux";

import App from "./components/App/App";
import configureStore from "./store/configureStore";
import {
    handleUserLogin
} from "./actions/authenticationActions";
import { getLogger } from "./utils/logger";
import "./assets/styles/reactSelect.scss";
import { AuthService, LoginCallback, SecureRoute, Security } from '@okta/okta-react';
import axios from "axios";
import environmentService from "./service/EnvironmentService";
import * as cms from "./constants/cms";
var log = getLogger("main");
const AUTH_ACTION_TIMEOUT = 60000; // 60 seconds in milliseconds

/**
 * This is our redux data store for storing all data retrieved from API Calls and any other state that needs
 * to be maintained.
 */
const store = configureStore(window.__INITIAL_STATE__);
const authService = new AuthService({
  issuer: process.env.REACT_APP_AUTH_ENDPOINT,
  client_id: process.env.REACT_APP_CLIENT_ID,
  redirect_uri: `${window.location.origin}/dashboard/login/callback`,
  postLogoutRedirectUri: `${window.location.origin}`,
  scope: ['openid', 'email'],
  pkce: true,
  tokenManager: {
    autoRenew: true,
    storage: 'sessionStorage',
  },
});

let oauthTokenStorage = JSON.parse(sessionStorage.getItem("okta-token-storage"));
let oauthToken = oauthTokenStorage?.idToken?.value;
console.log(oauthToken)

axios({
    method: 'post',
    url: environmentService.getDomain() + cms.TOKEN_EXCHANGE_PATH,
    data: {
      token: oauthToken,
    },
    timeout: AUTH_ACTION_TIMEOUT,
    headers: {
      'Content-Type': 'application/json',
    }
  })
    .then(function (response) {
      console.log("exchange token successful")
      console.log(response.status)
      handleUserLoginAfterTokenExchange(response)
  })
    .catch(function ({ response }) {
    //  TODO catch errors and handle timeout
      console.log("Failed to exchange OAuth token")
      console.log(response)
    });

let handleUserLoginAfterTokenExchange = (response) => {
    console.log("calling handle user login")
    handleUserLogin(response, store.dispatch, true);
}

// Create an enhanced history that syncs navigation events with the store
// const history = syncHistoryWithStore(hashHistory, store);
const history = createBrowserHistory();
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
          <Security authService={authService}>
            <Switch>
              <SecureRoute path="/" exact component={App} />
              <Route path="/dashboard/login/callback" component={LoginCallback} />
            </Switch>
          </Security>
        </Router>
      </div>
    </Provider>
  </div>,
  document.getElementById("root")
);

