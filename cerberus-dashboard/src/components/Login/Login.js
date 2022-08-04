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

import React from 'react';
import { Component } from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as messengerActions from '../../actions/messengerActions';
import Messenger from '../Messenger/Messenger';
import './Login.scss';
import LoginUserForm from '../LoginUserForm/LoginUserForm';
import LoginMfaForm from '../LoginMfaForm/LoginMfaForm';

import {LoginRequired} from "@nike/aegis-auth-react";
import client from "../../modules/AegisAuthClient";

class LoginForm extends Component {
    static propTypes = {
        dispatch: PropTypes.func.isRequired,
        isMfaRequired: PropTypes.bool.isRequired,
    };

    componentDidMount() {
        this.props.dispatch(messengerActions.clearAllMessages());
    }
    render() {
        const { isSessionExpired, isMfaRequired } = this.props;

        return (
            <div id='login-container' className=''>
                <div id='login-form-div'>
                    <header>
                        <div id='logo-container'>
                            <div className='cerberus-logo'></div>
                        </div>
                      <LoginRequired client={client}>
                        <h1 className='ncss-brand'>CERBERUS MANAGEMENT DASHBOARD</h1>
                      </LoginRequired>

                    </header>
                    {isSessionExpired &&
                        <div id="session-expired-message">
                            Your session has expired and you are required to re-authenticate
                        </div>
                    }
                    <Messenger />
                    {!isMfaRequired && <LoginUserForm />}
                    {isMfaRequired && <LoginMfaForm />}
                </div>
            </div>
        );
    }
}

const mapStateToProps = state => ({
    isSessionExpired: state.auth.isSessionExpired,
    isMfaRequired: state.auth.isMfaRequired,
    statusText: state.auth.statusText,
    initialValues: {
        redirectTo: state.router.location.query.next || '/'
    }
});

export default connect(mapStateToProps)(LoginForm);
