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
    mfaDevices: [],
    isAdmin: false,
    groups: [],
    policies: null,
    authTokenTimeoutId: null,
    sessionWarningTimeoutId: null
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
            authTokenTimeoutId: payload.authTokenTimeoutId,
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
    // sets the id of the session warning timeout
    [constants.SET_SESSION_WARNING_TIMEOUT_ID]: (state, payload) => {
        return Object.assign({}, state, {
            sessionWarningTimeoutId: payload.sessionWarningTimeoutId,
        })
    },
    // removes the timeout id of the auth token expire
    [constants.REMOVE_AUTH_TOKEN_TIMEOUT]: (state) => {

        return Object.assign({}, state, {
            authTokenTimeoutId: null
        })
    },
    // removes the timeout id of the session warning
    [constants.REMOVE_SESSION_WARNING_TIMEOUT]: (state) => {

        return Object.assign({}, state, {
            sessionWarningTimeoutId: null
        })
    },
})
