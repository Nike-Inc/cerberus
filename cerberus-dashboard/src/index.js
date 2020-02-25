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
import { Router, Route, IndexRoute, hashHistory } from "react-router";
import { Provider } from "react-redux";
import { syncHistoryWithStore } from "react-router-redux";

import App from "./components/App/App";
import LandingView from "./components/LandingView/LandingView";
import SDBMetadataList from "./components/SDBMetadataList/SDBMetadataList";
import ManageSafeDepositBox from "./components/ManageSafeDepositBox/ManageSafeDepositBox";
import NotFound from "./components/NotFound/NotFound";
import configureStore from "./store/configureStore";
import {
  loginUserSuccess,
  handleSessionExpiration,
  setSessionWarningTimeout
} from "./actions/authenticationActions";
import * as workerTimers from "worker-timers";
import { getLogger } from "./utils/logger";
import "./assets/styles/reactSelect.scss";

var log = getLogger("main");

/**
 * This is our redux data store for storing all data retrieved from API Calls and any other state that needs
 * to be maintained.
 */
const store = configureStore(window.__INITIAL_STATE__);

/**
 * Grab token from session storage
 */
let token = JSON.parse(sessionStorage.getItem("token"));

// use session token to register user as logged in
if (token !== null && token !== "") {
  let dateString = sessionStorage.getItem("tokenExpiresDate");
  let tokenExpiresDate = new Date(dateString);
  let now = new Date();

  log.debug(`Token expires on ${tokenExpiresDate}`);

  let dateTokenExpiresInMillis = tokenExpiresDate.getTime() - now.getTime();

  // warn two minutes before token expiration
  store.dispatch(
    setSessionWarningTimeout(
      dateTokenExpiresInMillis - 120000,
      token.data.client_token.client_token
    )
  );

  let sessionExpirationCheckIntervalInMillis = 2000;
  let sessionExpirationCheckIntervalId = workerTimers.setInterval(() => {
    let currentTimeInMillis = new Date().getTime();
    let sessionExpirationTimeInMillis = tokenExpiresDate.getTime();
    if (currentTimeInMillis >= sessionExpirationTimeInMillis) {
      store.dispatch(handleSessionExpiration());
    }
  }, sessionExpirationCheckIntervalInMillis);

  store.dispatch(loginUserSuccess(token, sessionExpirationCheckIntervalId));
}

// Create an enhanced history that syncs navigation events with the store
const history = syncHistoryWithStore(hashHistory, store);

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
          <Route path="/" component={App}>
            <IndexRoute component={LandingView} />
            <Route
              path="manage-safe-deposit-box/:id"
              component={ManageSafeDepositBox}
            />
            <Route path="admin/sdb-metadata" component={SDBMetadataList} />
            <Route path="*" component={NotFound} />
          </Route>
          <Route path="*" component={NotFound} />
        </Router>
      </div>
    </Provider>
  </div>,
  document.getElementById("root")
);
