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

import { createReducer } from '../utils'
import * as constants from '../constants/actions'

const initialState = {
    cerberusAuthToken: null,
    stateToken: null,
    userName: null,
    isAuthenticated: false,
    isAuthenticating: false,
    isSessionExpired: false,
    isMfaRequired: false,
    isChallengeSent: false,
    mfaDevices: [],
    isAdmin: false,
    groups: [],
    policies: null,
    sessionExpirationCheckIntervalId: null,
    sessionWarningTimeoutId: null,
    selectedDeviceId: null,
    shouldDisplaySendCodeButton: false
}

export default createReducer(initialState, {
    // lets the app know that the user is being authenticated
    [constants.LOGIN_USER_REQUEST]: (state) => {
        return Object.assign({}, state, {
            'isAuthenticating': true,
            'statusText': null
        })
    },
    // lets the app know that the user has been logged in and save the needed user data
    [constants.LOGIN_USER_SUCCESS]: (state, payload) => {
        return Object.assign({}, state, {
            isAuthenticating: false,
            isAuthenticated: true,
            isSessionExpired: false,
            isAdmin: payload.tokenData.metadata.is_admin,
            cerberusAuthToken: payload.tokenData.client_token,
            userName: payload.tokenData.metadata.username,
            groups: payload.tokenData.metadata.groups.split(/,/),
            policies: payload.tokenData.policies,
            sessionExpirationCheckIntervalId: payload.sessionExpirationCheckIntervalId,
        })
    },
    // logs the user out and resets user data
    [constants.RESET_USER_AUTH_STATE]: () => {
        return initialState
    },
    // logs the user out and resets user data, sets session expired true
    [constants.SESSION_EXPIRED]: () => {
        return Object.assign({}, initialState, {
            isSessionExpired: true
        })
    },
    // logs the user out and resets user data, sets session expired true
    [constants.LOGIN_MFA_REQUIRED]: (state, payload) => {
        return Object.assign({}, state, {
            isAuthenticating: false,
            isAuthenticated: false,
            isMfaRequired: true,
            stateToken: payload.stateToken,
            mfaDevices: payload.mfaDevices
        })
    },
    // lets the app know that the challenge has been sent
    [constants.LOGIN_MFA_CHALLENGE]: (state) => {
        return Object.assign({}, state, {
            isChallengeSent: true,
        })
    },
    // sets the id of the session warning timeout
    [constants.SET_SESSION_WARNING_TIMEOUT_ID]: (state, payload) => {
        return Object.assign({}, state, {
            sessionWarningTimeoutId: payload.sessionWarningTimeoutId,
        })
    },
    // removes the interval id of the session expiration check
    [constants.REMOVE_SESSION_EXPIRATION_CHECK_INTERVAL]: (state) => {

        return Object.assign({}, state, {
            sessionExpirationCheckIntervalId: null
        })
    },
    // removes the timeout id of the session warning
    [constants.REMOVE_SESSION_WARNING_TIMEOUT]: (state) => {
        return Object.assign({}, state, {
            sessionWarningTimeoutId: null
        })
    },
    // sets the id of the currently selected MFA device
    [constants.SET_SELECTED_MFA_DEVICE]: (state, payload) => {
        return Object.assign({}, state, {
            selectedDeviceId: payload.selectedDeviceId,
            shouldDisplaySendCodeButton: state.mfaDevices
                .filter(device => device.id === payload.selectedDeviceId)[0].requires_trigger
        })
    }
})
