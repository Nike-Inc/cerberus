import React from 'react'
import environmentService from 'EnvironmentService'
import { hashHistory } from 'react-router'
import axios from 'axios'
import * as constants from '../constants/actions'
import * as appActions from '../actions/appActions'
import * as messengerActions from '../actions/messengerActions'
import * as cms from '../constants/cms'
import * as headerActions from '../actions/headerActions'
import * as cmsUtils from '../utils/cmsUtils'
import ApiError from '../components/ApiError/ApiError'
import ConfirmationBox from '../components/ConfirmationBox/ConfirmationBox'
import * as modalActions from '../actions/modalActions'
import * as manageSDBActions from '../actions/manageSafetyDepositBoxActions'
import { getLogger } from 'logger'

var log = getLogger('authentication-actions')

const AUTH_ACTION_TIMEOUT = 10000 // 10 seconds in milliseconds

/**
 * These are the actions for authentication events that will trigger the related reducers to change application state
 */

/**
 * This action is dispatched when we have a valid response object from the CMS Auth endpoint
 * @param response The json response object from the ldap auth cms endpoint
 * @param authTokenTimeoutId ID of the timeout that expires the user session
 * @returns {{type: string, payload: {tokenData: *}}} The object to dispatch to trigger the reducer to update the auth state
 */
export function loginUserSuccess(response, authTokenTimeoutId) {
    return {
        type: constants.LOGIN_USER_SUCCESS,
        payload: {
            tokenData: response.data.client_token,
            authTokenTimeoutId: authTokenTimeoutId
        }
    }
}
/**
 * This action is dispatched to let the app state know that the auth request is in progress.
 * @returns {{type: string}} The object to dispatch to trigger the reducer to update the auth state
 */
export function loginUserRequest() {
    return {
        type: constants.LOGIN_USER_REQUEST
    }
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
    }
}

/**
 * Updates the state to indicate that the user is successfully authenticated.
 */
function handleUserLogin(response, dispatch, redirectToWelcome=true) {
    let leaseDurationInSeconds = response.data.data.client_token.lease_duration
    const millisecondsPerSecond = 1000
    const bestGuessOfRequestLatencyInMilliseconds = 120 * millisecondsPerSecond // take 2 minutes off of duration to account for latency

    let now = new Date()
    let vaultTokenExpiresDateInMilliseconds = (now.getTime() + ((leaseDurationInSeconds * millisecondsPerSecond) - bestGuessOfRequestLatencyInMilliseconds))

    let tokenExpiresDate = new Date()
    let token = response.data.data.client_token.client_token
    tokenExpiresDate.setTime(vaultTokenExpiresDateInMilliseconds)

    log.debug(`Setting session timeout to ${tokenExpiresDate}`)

    let timeToExpireTokenInMillis = tokenExpiresDate.getTime() - now.getTime()

    let sessionWarningTimeoutId = setTimeout(() => {
        dispatch(warnSessionExpiresSoon(token))
    }, timeToExpireTokenInMillis - 120000)  // warn two minutes before expiration

    dispatch(setSessionWarningTimeoutId(sessionWarningTimeoutId))

    let authTokenTimeoutId = setTimeout(() => {
        dispatch(handleSessionExpiration())
    }, timeToExpireTokenInMillis)

    sessionStorage.setItem('token', JSON.stringify(response.data))
    sessionStorage.setItem('tokenExpiresDate', tokenExpiresDate)
    sessionStorage.setItem('userRespondedToSessionWarning', false)
    dispatch(messengerActions.clearAllMessages())
    dispatch(loginUserSuccess(response.data, authTokenTimeoutId))
    dispatch(appActions.fetchSideBarData(token))
    if (redirectToWelcome) {
        hashHistory.push("/")
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
    return function(dispatch) {
        dispatch(loginUserRequest())
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
                dispatch(loginMfaRequired(response))
            } else {
                handleUserLogin(response, dispatch)
            }
        })
        .catch(function (response) {
            log.error('Failed to login user', response)

            dispatch(messengerActions.addNewMessage(
                <div className="login-error-msg-container">
                    <div className="login-error-msg-header">Failed to Login</div>
                    <div className="login-error-msg-content-wrapper">
                        <div className="login-error-msg-label">Server Message:</div>
                        <div className="login-error-msg-cms-msg">{cmsUtils.parseCMSError(response)}</div>
                    </div>
                </div>
            ))

            dispatch(resetAuthState())
        })
    }
}

/**
 * This action re-authenticates a user with MFA, if MFA is required
 * @param otpToken The security token of the user to auth
 * @param mfaDeviceId ID of the MFA security device
 * @param stateToken Identifying token for the authentication request
 */
export function finalizeMfaLogin(otpToken, mfaDeviceId, stateToken) {
    return function(dispatch) {
        dispatch(loginUserRequest())
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
            handleUserLogin(response, dispatch)
        })
        .catch(function (response) {
            log.error('Failed to finalize MFA login', response)

            dispatch(messengerActions.addNewMessage(
                <div className="login-error-msg-container">
                    <div className="login-error-msg-header">Failed to Login</div>
                    <div className="login-error-msg-content-wrapper">
                        <div className="login-error-msg-label">Server Message:</div>
                        <div className="login-error-msg-cms-msg">{cmsUtils.parseCMSError(response)}</div>
                    </div>
                </div>
            ))

            dispatch(resetAuthState())
        })
    }
}

/**
 * This action is dispatched to renew a users session token
 */
export function refreshAuth(token, redirectPath='/', redirect=true) {
    return function(dispatch) {
        dispatch(loginUserRequest())
        return axios({
            url: environmentService.getDomain() + cms.USER_AUTH_PATH_REFRESH,
            headers: {'X-Vault-Token': token},
            timeout: AUTH_ACTION_TIMEOUT
        })
        .then(function (response) {
            dispatch(handleRemoveAuthTokenTimeout())
            dispatch(handleRemoveSessionWarningTimeout())
            setTimeout(function(){
                handleUserLogin(response, dispatch, false)
                if (redirect) {
                    hashHistory.push(redirectPath)
                }
            }, 2000);

        })
        .catch(function (response) {
            // Clears View Token Modal upon max refresh token limit to prevent errors
            dispatch(modalActions.clearAllModals())
            log.error('Failed to login user', response)
            dispatch(resetAuthState())
            hashHistory.push('dashboard/#/login')
            dispatch(messengerActions.addNewMessage(<ApiError message="Failed to refresh user token" response={response} />))
        })
    }
}

/**
 * This action is dispatched to log a user out
 */
export function logoutUser(token) {
    return function(dispatch) {
        return axios({
            method: 'delete',
            url: environmentService.getDomain() + cms.TOKEN_DELETE_PATH,
            headers: {'X-Vault-Token': token},
            timeout: AUTH_ACTION_TIMEOUT
        })
        .then(function () {
            sessionStorage.removeItem('token')
            sessionStorage.removeItem('tokenExpiresDate')
            sessionStorage.removeItem('userRespondedToSessionWarning')
            dispatch(handleRemoveAuthTokenTimeout())
            dispatch(handleRemoveSessionWarningTimeout())
            dispatch(resetAuthState())
            dispatch(headerActions.mouseOutUsername())
            hashHistory.push('/login')
        })
        .catch(function (response) {
            log.error('Failed to logout user', response)
            dispatch(messengerActions.addNewMessage(<ApiError message="Failed to Logout User" response={response} />))
        })
    }
}

/**
 * This action is dispatched to log a user out
 */
export function handleSessionExpiration() {
    return function(dispatch) {
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('tokenExpiresDate')
        sessionStorage.removeItem('userRespondedToSessionWarning')
        dispatch(handleRemoveAuthTokenTimeout())
        dispatch(handleRemoveSessionWarningTimeout())
        dispatch(expireSession())
        dispatch(modalActions.clearAllModals())
        dispatch(manageSDBActions.resetToInitialState())
        dispatch(appActions.resetToInitialState())
    }
}

export function resetAuthState() {
    return {
        type: constants.RESET_USER_AUTH_STATE
    }
}

export function expireSession() {
    return {
        type: constants.SESSION_EXPIRED
    }
}

export function removeAuthTokenTimeoutId() {
    return {
        type: constants.REMOVE_AUTH_TOKEN_TIMEOUT
    }
}

export function removeSessionWarningTimeoutId() {
    return {
        type: constants.REMOVE_SESSION_WARNING_TIMEOUT
    }
}

export function setSessionWarningTimeoutId(id) {
    return {
        type: constants.SET_SESSION_WARNING_TIMEOUT_ID,
        payload: {
            sessionWarningTimeoutId: id
        }
    }
}

export function handleRemoveAuthTokenTimeout() {
    return function(dispatch, getState) {
        clearTimeout(getState().auth.authTokenTimeoutId)
        dispatch(removeAuthTokenTimeoutId())
    }
}

export function handleRemoveSessionWarningTimeout() {
    return function(dispatch, getState) {
        clearTimeout(getState().auth.sessionWarningTimeoutId)
        dispatch(removeSessionWarningTimeoutId())
    }
}

export function handleUserRespondedToSessionWarning() {
    return function() {
        sessionStorage.setItem('userRespondedToSessionWarning', true)
    }
}

/**
 * Warn the user at specified time that their session is about to expire
 * @param timeToWarnInMillis - Time at which to warn user
 * @param tokenStr - Token string
 */
export function setSessionWarningTimeout(timeToWarnInMillis, tokenStr) {
    return function(dispatch) {
        let userHasRespondedToSessionWarning = sessionStorage.getItem('userRespondedToSessionWarning') === "true";

        if (! userHasRespondedToSessionWarning) {
            let sessionWarningTimeoutId = setTimeout(() => {
                dispatch(warnSessionExpiresSoon(tokenStr))
            }, timeToWarnInMillis)

            dispatch(setSessionWarningTimeoutId(sessionWarningTimeoutId))
        }
    }
}

/**
 * Warn user that session is about to expire, and give the option to refresh session
 * @param tokenStr - Token string
 */
export function warnSessionExpiresSoon(tokenStr) {

    return function(dispatch) {
        let yes = () => {
            dispatch(refreshAuth(tokenStr, "/", false))
            dispatch(handleUserRespondedToSessionWarning())
            dispatch(modalActions.popModal())
        }

        let no = () => {
            dispatch(handleUserRespondedToSessionWarning())
            dispatch(modalActions.popModal())
        }

        let conf = <ConfirmationBox handleYes={yes}
                                    handleNo={no}
                                    message="Your session is about to expire. Would you like to stay logged in?"/>

        dispatch(modalActions.pushModal(conf))
    }
}
