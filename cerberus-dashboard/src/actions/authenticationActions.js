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
import environmentService from '../service/EnvironmentService';
import { hashHistory } from 'react-router';
import axios from 'axios';
import * as constants from '../constants/actions';
import * as appActions from './appActions';
import * as messengerActions from './messengerActions';
import * as cms from '../constants/cms';
import * as headerActions from './headerActions';
import * as cmsUtils from '../utils/cmsUtils';
import ApiError from '../components/ApiError/ApiError';
import ConfirmationBox from '../components/ConfirmationBox/ConfirmationBox';
import * as modalActions from './modalActions';
import * as manageSDBActions from './manageSafetyDepositBoxActions';
import * as workerTimers from 'worker-timers';
import { getLogger } from "../utils/logger";

var log = getLogger('authentication-actions');

const AUTH_ACTION_TIMEOUT = 60000; // 60 seconds in milliseconds

/**
 * These are the actions for authentication events that will trigger the related reducers to change application state
 */

/**
 * This action is dispatched when we have a valid response object from the CMS Auth endpoint
 * @param response The json response object from the ldap auth cms endpoint
 * @param sessionExpirationCheckIntervalId ID of the interval that checks to see if the user's session has expired
 * @returns {{type: string, payload: {tokenData: *}}} The object to dispatch to trigger the reducer to update the auth state
 */
export function loginUserSuccess(response, sessionExpirationCheckIntervalId) {
    return {
        type: constants.LOGIN_USER_SUCCESS,
        payload: {
            tokenData: response.data.client_token,
            sessionExpirationCheckIntervalId: sessionExpirationCheckIntervalId
        }
    };
}
/**
 * This action is dispatched to let the app state know that the auth request is in progress.
 * @returns {{type: string}} The object to dispatch to trigger the reducer to update the auth state
 */
export function loginUserRequest() {
    return {
        type: constants.LOGIN_USER_REQUEST
    };
}

/**
 * Updates the state to indicate that MFA code challenge was triggered.
 * @returns {{type: string}} The object to dispatch to trigger the reducer to update the challenge sent state
 */
export function loginMfaChallenge() {
    return {
        type: constants.LOGIN_MFA_CHALLENGE,
    };
}

/**
 * Updates the state to indicate that MFA is required for the given user to authenticate.
 * @returns {{type: string}} The object to dispatch to trigger the reducer to update the auth state
 */
export function loginMfaRequired(response) {
    return {
        type: constants.LOGIN_MFA_REQUIRED,
        payload: {
            stateToken: response.data.data.state_token,
            mfaDevices: response.data.data.devices,
        }
    };
}

/**
 * Updates the state to indicate that the user is successfully authenticated.
 */
function handleUserLogin(response, dispatch, redirectToWelcome = true) {
    let leaseDurationInSeconds = response.data.data.client_token.lease_duration;
    const millisecondsPerSecond = 1000;
    const bestGuessOfRequestLatencyInMilliseconds = 120 * millisecondsPerSecond; // take 2 minutes off of duration to account for latency

    let now = new Date();
    let cerberusAuthTokenExpiresDateInMilliseconds = (now.getTime() + ((leaseDurationInSeconds * millisecondsPerSecond) - bestGuessOfRequestLatencyInMilliseconds));

    let tokenExpiresDate = new Date();
    let token = response.data.data.client_token.client_token;
    tokenExpiresDate.setTime(cerberusAuthTokenExpiresDateInMilliseconds);

    log.debug(`Setting session timeout to ${tokenExpiresDate}`);

    let timeToExpireTokenInMillis = tokenExpiresDate.getTime() - now.getTime();

    let sessionExpirationCheckIntervalInMillis = 2000;
    let sessionExpirationCheckIntervalId = workerTimers.setInterval(() => {
        let currentTimeInMillis = new Date().getTime();
        let sessionExpirationTimeInMillis = tokenExpiresDate.getTime();
        if (currentTimeInMillis >= sessionExpirationTimeInMillis) {
            dispatch(handleSessionExpiration());
        }
    }, sessionExpirationCheckIntervalInMillis);

    let sessionWarningTimeoutId = workerTimers.setTimeout(() => {
        dispatch(warnSessionExpiresSoon(token));
    }, timeToExpireTokenInMillis - 120000);  // warn two minutes before expiration

    dispatch(setSessionWarningTimeoutId(sessionWarningTimeoutId));

    sessionStorage.setItem('token', JSON.stringify(response.data));
    sessionStorage.setItem('tokenExpiresDate', tokenExpiresDate);
    sessionStorage.setItem('userRespondedToSessionWarning', false);
    dispatch(messengerActions.clearAllMessages());
    dispatch(loginUserSuccess(response.data, sessionExpirationCheckIntervalId));
    dispatch(appActions.fetchSideBarData(token));
    if (redirectToWelcome) {
        hashHistory.push("/");
    }
}

/**
 * This action will let the state know that we are performing the auth
 * It will then perform the authentication and update state after success
 * @param username The Username of the user to auth
 * @param password The password of the user to auth
 * @returns {Function} The object to dispatch
 */
export function loginUser(username, password) {
    return function (dispatch) {
        dispatch(loginUserRequest());
        return axios({
            url: environmentService.getDomain() + cms.USER_AUTH_PATH,
            auth: {
                username: username,
                password: password
            },
            timeout: AUTH_ACTION_TIMEOUT
        })
            .then(function (response) {
                if (response.data.status === cms.MFA_REQUIRED_STATUS) {
                    dispatch(loginMfaRequired(response));
                } else {
                    handleUserLogin(response, dispatch);
                }
            })
            .catch(function ({ response }) {
                log.error('Failed to login user', response);

                dispatch(messengerActions.addNewMessage(
                    <div className="login-error-msg-container">
                        <div className="login-error-msg-header">Failed to Login</div>
                        <div className="login-error-msg-content-wrapper">
                            <div className="login-error-msg-label">Server Message:</div>
                            <div className="login-error-msg-cms-msg">{cmsUtils.parseCMSError(response)}</div>
                        </div>
                    </div>
                ));

                dispatch(resetAuthState());
            });
    };
}

/**
 * This action re-authenticates a user with MFA, if MFA is required
 * @param otpToken The security token of the user to auth
 * @param mfaDeviceId ID of the MFA security device
 * @param stateToken Identifying token for the authentication request
 */
export function finalizeMfaLogin(otpToken, mfaDeviceId, stateToken) {
    return function (dispatch) {
        dispatch(loginUserRequest());
        return axios({
            method: 'post',
            url: environmentService.getDomain() + cms.USER_AUTH_MFA_PATH,
            data: {
                state_token: stateToken,
                device_id: mfaDeviceId,
                otp_token: otpToken
            },
            timeout: AUTH_ACTION_TIMEOUT
        })
            .then(function (response) {
                handleUserLogin(response, dispatch);
            })
            .catch(function ({ response }) {
                log.error('Failed to finalize MFA login', response);

                dispatch(messengerActions.addNewMessage(
                    <div className="login-error-msg-container">
                        <div className="login-error-msg-header">Failed to Login</div>
                        <div className="login-error-msg-content-wrapper">
                            <div className="login-error-msg-label">Server Message:</div>
                            <div className="login-error-msg-cms-msg">{cmsUtils.parseCMSError(response)}</div>
                        </div>
                    </div>
                ));

                dispatch(resetAuthState());
            });
    };
}

/**
 * This action triggers a challenge for a user with MFA, if MFA is required
 * @param mfaDeviceId ID of the MFA security device
 * @param stateToken Identifying token for the authentication request
 */
export function triggerCodeChallenge(mfaDeviceId, stateToken) {
    return function (dispatch) {
        dispatch(loginMfaChallenge());
        return axios({
            method: 'post',
            url: environmentService.getDomain() + cms.USER_AUTH_MFA_PATH,
            data: {
                state_token: stateToken,
                device_id: mfaDeviceId
            },
            timeout: AUTH_ACTION_TIMEOUT
        })
            .catch(function ({ response }) {
                log.error('Failed to trigger challenge', response);

                dispatch(messengerActions.addNewMessage(
                    <div className="login-error-msg-container">
                        <div className="login-error-msg-header">Failed to Trigger Challenge</div>
                        <div className="login-error-msg-content-wrapper">
                            <div className="login-error-msg-label">Server Message:</div>
                            <div className="login-error-msg-cms-msg">{cmsUtils.parseCMSError(response)}</div>
                        </div>
                    </div>
                ));
                dispatch(resetAuthState());
            });
    };
}

/**
 * This action triggers a push notification challenge for a user with MFA, if MFA is required
 * @param mfaDeviceId ID of the MFA security device
 * @param stateToken Identifying token for the authentication request
 */
export function triggerPushChallenge(mfaDeviceId, stateToken) {
    return function (dispatch) {
        dispatch(loginMfaChallenge());
        return axios({
            method: 'post',
            url: environmentService.getDomain() + cms.USER_AUTH_MFA_PATH,
            data: {
                state_token: stateToken,
                device_id: mfaDeviceId,
                is_push: true
            },
            timeout: AUTH_ACTION_TIMEOUT
        })
            .then(function (response) {
                handleUserLogin(response, dispatch);
            })
            .catch(function ({ response }) {
                log.error('Failed to finalize MFA login', response);

                dispatch(messengerActions.addNewMessage(
                    <div className="login-error-msg-container">
                        <div className="login-error-msg-header">Failed to Login</div>
                        <div className="login-error-msg-content-wrapper">
                            <div className="login-error-msg-label">Server Message:</div>
                            <div className="login-error-msg-cms-msg">{cmsUtils.parseCMSError(response, true)}</div>
                        </div>
                    </div>
                ));
                dispatch(resetAuthState());
            });
    };
}

/**
 * This action is dispatched to renew a users session token
 */
export function refreshAuth(token, redirectPath = '/', redirect = true) {
    return function (dispatch) {
        dispatch(loginUserRequest());
        return axios({
            url: environmentService.getDomain() + cms.USER_AUTH_PATH_REFRESH,
            headers: { 'X-Cerberus-Token': token },
            timeout: AUTH_ACTION_TIMEOUT
        })
            .then(function (response) {
                dispatch(handleRemoveSessionExpirationCheck());
                dispatch(removeSessionWarningTimeout());
                workerTimers.setTimeout(function () {
                    handleUserLogin(response, dispatch, false);
                    if (redirect) {
                        hashHistory.push(redirectPath);
                    }
                }, 2000);

            })
            .catch(function ({ response }) {
                // Clears View Token Modal upon max refresh token limit to prevent errors
                dispatch(modalActions.clearAllModals());
                log.error('Failed to login user', response);
                dispatch(resetAuthState());
                hashHistory.push('dashboard/#/login');
                dispatch(messengerActions.addNewMessage(<ApiError message="Failed to refresh user token" response={response} />));
            });
    };
}

/**
 * This action is dispatched to log a user out
 */
export function logoutUser(token) {
    return function (dispatch) {
        return axios({
            method: 'delete',
            url: environmentService.getDomain() + cms.TOKEN_DELETE_PATH,
            headers: { 'X-Cerberus-Token': token },
            timeout: AUTH_ACTION_TIMEOUT
        })
            .then(function () {
                sessionStorage.removeItem('token');
                sessionStorage.removeItem('tokenExpiresDate');
                sessionStorage.removeItem('userRespondedToSessionWarning');
                dispatch(handleRemoveSessionExpirationCheck());
                dispatch(removeSessionWarningTimeout());
                dispatch(resetAuthState());
                dispatch(headerActions.mouseOutUsername());
                hashHistory.push('/login');
            })
            .catch(function ({ response }) {
                log.error('Failed to logout user', response);
                dispatch(messengerActions.addNewMessage(<ApiError message="Failed to Logout User" response={response} />));
            });
    };
}

/**
 * This action is dispatched to log a user out
 */
export function handleSessionExpiration() {
    return function (dispatch) {
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('tokenExpiresDate');
        sessionStorage.removeItem('userRespondedToSessionWarning');
        dispatch(handleRemoveSessionExpirationCheck());
        dispatch(removeSessionWarningTimeout());
        dispatch(expireSession());
        dispatch(modalActions.clearAllModals());
        dispatch(manageSDBActions.resetToInitialState());
        dispatch(appActions.resetToInitialState());
    };
}

export function resetAuthState() {
    return {
        type: constants.RESET_USER_AUTH_STATE
    };
}

export function expireSession() {
    return {
        type: constants.SESSION_EXPIRED
    };
}

export function removeSessionExpirationCheck() {
    return {
        type: constants.REMOVE_SESSION_EXPIRATION_CHECK_INTERVAL
    };
}

export function removeSessionWarningTimeoutId() {
    return {
        type: constants.REMOVE_SESSION_WARNING_TIMEOUT
    };
}

export function setSessionWarningTimeoutId(id) {
    return {
        type: constants.SET_SESSION_WARNING_TIMEOUT_ID,
        payload: {
            sessionWarningTimeoutId: id
        }
    };
}

export function handleRemoveSessionExpirationCheck() {
    return function (dispatch, getState) {
        let sessionExpirationCheckIntervalId = getState().auth.sessionExpirationCheckIntervalId;
        log.debug(`Removing session expiration check interval, id: ${sessionExpirationCheckIntervalId}`);
        try {
            workerTimers.clearInterval(sessionExpirationCheckIntervalId);
        } catch (err) {
            console.log(`Failed to clear auth token timeout, id=${sessionExpirationCheckIntervalId}`);
            console.log(err);
        }
        dispatch(removeSessionExpirationCheck());
    };
}

export function removeSessionWarningTimeout() {
    return function (dispatch, getState) {
        let sessionWarningTimeoutId = getState().auth.sessionWarningTimeoutId;
        log.debug(`Removing warning timeout, id: ${sessionWarningTimeoutId}`);
        try {
            workerTimers.clearTimeout(getState().auth.sessionWarningTimeoutId);
        } catch (err) {
            console.log(`Failed to clear session warning timeout, id=${sessionWarningTimeoutId}`);
            console.log(err);
        }
        dispatch(removeSessionWarningTimeoutId());
    };
}

export function handleUserRespondedToSessionWarning() {
    return function () {
        sessionStorage.setItem('userRespondedToSessionWarning', true);
    };
}

/**
 * Warn the user at specified time that their session is about to expire
 * @param timeToWarnInMillis - Time at which to warn user
 * @param tokenStr - Token string
 */
export function setSessionWarningTimeout(timeToWarnInMillis, tokenStr) {
    return function (dispatch) {
        let userHasRespondedToSessionWarning = sessionStorage.getItem('userRespondedToSessionWarning') === "true";

        if (!userHasRespondedToSessionWarning) {
            let sessionWarningTimeoutId = workerTimers.setTimeout(() => {
                dispatch(warnSessionExpiresSoon(tokenStr));
            }, timeToWarnInMillis);

            dispatch(setSessionWarningTimeoutId(sessionWarningTimeoutId));
        }
    };
}

/**
 * Sets id to currently selected device id
 * @param selectedDeviceId - current selected device id string
 */
export function setSelectedDeviceId(selectedDeviceId) {
    return {
        type: constants.SET_SELECTED_MFA_DEVICE,
        payload: {
            selectedDeviceId: selectedDeviceId
        }
    };
}

/**
 * Warn user that session is about to expire, and give the option to refresh session
 * @param tokenStr - Token string
 */
export function warnSessionExpiresSoon(tokenStr) {

    return function (dispatch) {
        let yes = () => {
            dispatch(refreshAuth(tokenStr, "/", false));
            dispatch(handleUserRespondedToSessionWarning());
            dispatch(modalActions.popModal());
        };

        let no = () => {
            dispatch(handleUserRespondedToSessionWarning());
            dispatch(modalActions.popModal());
        };

        let conf = <ConfirmationBox handleYes={yes}
            handleNo={no}
            message="Your session is about to expire. Would you like to stay logged in?" />;

        dispatch(modalActions.pushModal(conf));
    };
}
