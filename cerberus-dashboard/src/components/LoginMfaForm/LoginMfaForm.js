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
import { reduxForm, touch } from 'redux-form';
import {finalizeMfaLogin, triggerCodeChallenge, triggerPushChallenge} from '../../actions/authenticationActions';
import MfaDeviceSelect from '../MfaDeviceSelect/MfaDeviceSelect';
import './LoginFormMfa.scss';

const formName = 'login-mfa-form';

/**
 * This is the smart component for the create new bucket page,
 * This component contains the actual for and handle passing needed things into the dumb component views
 */
export const fields = ['otpToken', 'mfaDeviceId'];

// define our client side form validation rules
const validate = values => {
    const errors = {};

    if (!values.otpToken) {
        errors.otpToken = 'Required';
    }
    if (!values.mfaDeviceId) {
        errors.mfaDeviceId = 'Required';
    }

    return errors;
};

class LoginMfaForm extends Component {
    static propTypes = {
        fields: PropTypes.object.isRequired,
        stateToken: PropTypes.string.isRequired,
        isAuthenticating: PropTypes.bool.isRequired,
        isChallengeSent: PropTypes.bool.isRequired,
        dispatch: PropTypes.func.isRequired,
    };

    constructor(props) {
        super(props);

        this.handleMfaDeviceTouch = function (formName) {
            this.props.dispatch(touch(formName, 'mfaDeviceId'));
        };
    }

    render() {
        const { fields: { otpToken, mfaDeviceId }, stateToken, handleSubmit, isAuthenticating, isChallengeSent, mfaDevices,
            dispatch, selectedDeviceId, shouldDisplaySendCodeButton, shouldDisplaySendPushButton } = this.props;

        return (
            <div >
                <h3 id="mfa-required-header" className="text-color-">MFA is required.</h3>
                <form id={formName} onSubmit={handleSubmit(data => (shouldDisplaySendPushButton) ?
                                                            dispatch(triggerPushChallenge(selectedDeviceId, stateToken, shouldDisplaySendPushButton)) :
                                                            dispatch(finalizeMfaLogin(data.otpToken, data.mfaDeviceId, stateToken)))}>
                    <div>
                        <label className='ncss-label'>MFA Devices:</label>
                        <div id='top-section'>
                            <MfaDeviceSelect {...mfaDeviceId}
                                             dispatch={dispatch}
                                             mfaDevices={mfaDevices}
                                             handleBeingTouched={() => { this.handleMfaDeviceTouch(formName); }} />
                        </div>

                        <div id='otp-div' className='ncss-form-group'>
                            <div className={((otpToken.touched && otpToken.error) ? 'ncss-input-container error' : 'ncss-input-container')}>
                                {!shouldDisplaySendPushButton &&
                                    <label className='ncss-label'>One-Time Passcode:</label>
                                }

                                <div id='otp-input-row'>

                                    {shouldDisplaySendCodeButton &&
                                    <button id={isChallengeSent ? 'sent-mfa-btn' : 'send-code-mfa-btn'} type='button' className='ncss-btn-offwhite ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                                            onClick={function () {
                                                dispatch(triggerCodeChallenge(selectedDeviceId, stateToken, shouldDisplaySendPushButton));
                                            }
                                            }
                                            disabled={isChallengeSent}
                                    >{isChallengeSent ? "Sent" : "Send Code"}</button>
                                    }
                                    {!shouldDisplaySendPushButton &&
                                    <input type='text'
                                           className='ncss-input pt2-sm pr4-sm pb2-sm pl4-sm r'
                                           placeholder='Please enter your OTP token'
                                           autoComplete="off"
                                           autoFocus={true}
                                           {...otpToken} />
                                    }
                                </div>
                                {otpToken.touched && otpToken.error && <div className='ncss-error-msg'>{otpToken.error}</div>}
                            </div>
                        </div>
                    </div>
                        <div id='login-form-submit-container'>
                            <div id="login-help">
                                <a target="_blank" href="/dashboard/help/index.html">Need help?</a>
                            </div>
                            {shouldDisplaySendPushButton &&
                                <button id={isChallengeSent ? 'sent-mfa-btn' : 'send-code-mfa-btn'}
                                        type='submit'
                                        className='ncss-btn-offwhite ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                                        onClick={function () {
                                                dispatch(triggerPushChallenge(selectedDeviceId, stateToken));
                                            }
                                        }
                                        disabled={isChallengeSent}
                                >{isChallengeSent ? "Push Notification Sent" : "Send Push Notification"}</button>
                            }
                            {!shouldDisplaySendPushButton &&
                            <button id='login-mfa-btn'
                                    type='submit'
                                    className='ncss-btn-offwhite ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                                    disabled={isAuthenticating}>Login</button>
                            }
                    </div>
                </form>
            </div>
        );
    }
}


const mapStateToProps = state => ({
    shouldDisplaySendCodeButton: state.auth.shouldDisplaySendCodeButton,
    shouldDisplaySendPushButton: state.auth.shouldDisplaySendPushButton,
    selectedDeviceId: state.auth.selectedDeviceId,
    mfaDevices: state.auth.mfaDevices,
    stateToken: state.auth.stateToken,
    isAuthenticating: state.auth.isAuthenticating,
    isChallengeSent: state.auth.isChallengeSent,
    statusText: state.auth.statusText,
    initialValues: {
        redirectTo: state.routing.locationBeforeTransitions.query.next || '/',
    }
});

const form = reduxForm(
    {
        form: formName,
        fields: fields,
        validate
    }
)(LoginMfaForm);

export default connect(mapStateToProps)(form);