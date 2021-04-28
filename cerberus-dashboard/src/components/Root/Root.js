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

import { OktaAuth, toRelativeUrl } from '@okta/okta-auth-js';
import { LoginCallback, SecureRoute, Security } from '@okta/okta-react';
import axios from "axios";
import { replace } from "connected-react-router";
import React, { Component } from "react";
import { connect } from "react-redux";
import { Route, Switch } from "react-router";
import {
    handleUserLogin
} from "../../actions/authenticationActions";
import "../../assets/styles/reactSelect.scss";
import App from "../App/App";
import NotFound from '../NotFound/NotFound';
import * as cms from "../../constants/cms";
import environmentService from "../../service/EnvironmentService";

class Root extends Component {

    constructor(props) {
        super(props);

        this.oktaAuth = new OktaAuth({
            issuer: process.env.REACT_APP_AUTH_ENDPOINT,
            clientId: process.env.REACT_APP_CLIENT_ID,
            redirectUri: `${window.location.origin}/dashboard/callback`,
            postLogoutRedirectUri: `${window.location.origin}`,
            scope: ['openid', 'email'],
            pkce: true,
            tokenManager: {
                autoRenew: true,
                storage: 'sessionStorage',
            },
        });

        this.restoreOriginalUri = async (_oktaAuth, originalUri) => {
            console.log("Restoring URI to: ", originalUri, window.location.origin);
            console.log("Relative URL: ", toRelativeUrl(originalUri, window.location.origin));
            this.props.dispatch(replace(toRelativeUrl(originalUri, window.location.origin)));
            // this.props.dispatch(replace('/dashboard'));
        }
        
        this.AUTH_ACTION_TIMEOUT = 60000; // 60 seconds in milliseconds

        this.getToken = this.getToken.bind(this);
        this.handleUserLoginAfterTokenExchange = this.handleUserLoginAfterTokenExchange.bind(this);
    }

    componentDidMount() {
        let oauthTokenStorage = JSON.parse(sessionStorage.getItem("okta-token-storage"));
        let oauthToken = oauthTokenStorage?.idToken?.value;
        console.log(oauthToken)

        if (oauthToken) {
            this.getToken(oauthToken);
        }
    }

    handleUserLoginAfterTokenExchange(response) {
        console.log("calling handle user login")
        console.log(this.props);
        handleUserLogin(response, this.props.dispatch, true);
    }

    async getToken(oauthToken) {
        await axios({
            method: 'post',
            url: environmentService.getDomain() + cms.TOKEN_EXCHANGE_PATH,
            data: {
                token: oauthToken,
            },
            timeout: this.AUTH_ACTION_TIMEOUT,
            headers: {
                'Content-Type': 'application/json',
            }
        }).then((response) => {
            console.log(`${environmentService.getDomain() + cms.TOKEN_EXCHANGE_PATH} Response: `, response)
            this.handleUserLoginAfterTokenExchange(response)
            console.log("exchange token successful")
        }).catch((error) => {
            console.log("Failed to exchange OAuth token")
            console.log(error)
        });
    };

    render() {

        let { oktaAuth, restoreOriginalUri } = this;

        return (
            <Security oktaAuth={oktaAuth} restoreOriginalUri={restoreOriginalUri}>
                <Switch>
                    <Route path="/dashboard/callback" component={LoginCallback} />
                    <SecureRoute path="/" component={App} />
                    <Route path="*" component={NotFound} />
                </Switch>
            </Security>
        )
    }
}

const mapStateToProps = state => ({});

export default connect(mapStateToProps)(Root);