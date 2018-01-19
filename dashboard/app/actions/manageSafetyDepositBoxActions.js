import React from 'react'
import axios from 'axios'
import environmentService from 'EnvironmentService'
import * as humps from 'humps'
import * as actions from '../constants/actions'
import * as cms from '../constants/cms'
import * as messengerActions from '../actions/messengerActions'
import * as authActions from '../actions/authenticationActions'
import * as modalActions from '../actions/modalActions'
import ApiError from '../components/ApiError/ApiError'
import ConfirmationBox from '../components/ConfirmationBox/ConfirmationBox'

import { getLogger } from 'logger'
var log = getLogger('manage-sdb-actions')

export function storeSDBData(data) {
    return {
        type: actions.STORE_SDB_DATA,
        payload: data
    }
}

/**
 * Fetches the Safe Deposit Box data from the Cerberus Management Service
 * @param sdbId The id of the SDB to fetch info on
 */
export function fetchSDBDataFromCMS(sdbId, token) {
    return function(dispatch) {
        return axios({
            url: `${environmentService.getDomain()}${cms.BUCKET_RESOURCE}/${sdbId}`,
            headers: {'X-Cerberus-Token': token}
        })
        .then((response) => {
            log.debug("Fetched SDB Data from CMS", response)
            dispatch(storeSDBData(humps.camelizeKeys(response.data)))
        })
        .catch((response) => {
            log.error("Failed to fetch SDB", response)
            dispatch(messengerActions.addNewMessage(<ApiError message="Failed to Fetch SDB Data from CMS" response={response} />))
        })
    }
}

export function togglePermVis() {
    return {
        type: actions.TOGGLE_PERM_VIS
    }
}

export function fetchSecureDataPathKeys(path, token) {
    return function(dispatch) {
        dispatch(fetchingKeys())
        return axios({
            url: `/v1/secret/${path}`,
            params: {
                list: true
            },
            headers: {'X-Cerberus-Token': token},
            timeout: 60 * 1000 // 1 minute
        })
        .then((response) => {
            dispatch(updateSecureDataPathKeys(response.data.data.keys))
        })
        .catch((response) => {
            // no keys for the SDB Yet
            if (response.status == 404) {
                dispatch(updateSecureDataPathKeys([]))
            } else {
                log.error("Failed to fetch keys for secure data path", response)
                dispatch(messengerActions.addNewMessage(<ApiError message={`Failed to Fetch list of keys for Path: ${path} from Cerberus`} response={response} />))
            }
        })
    }
}

export function updateSecureDataPathKeys(keys) {
    return {
        type: actions.FETCHED_SECURE_DATA_KEYS,
        payload: keys
    }
}

export function fetchingKeys() {
    return {
        type: actions.FETCHING_SECURE_DATA_KEYS
    }
}

export function updateNavigatedPath(newPath, token) {
    return function(dispatch) {
        dispatch(storeNewPath(newPath))
        dispatch(fetchSecureDataPathKeys(newPath, token))
    }
}

export function storeNewPath(newPath) {
    return {
        type: actions.UPDATE_NAVIGATED_PATH,
        payload: newPath
    }
}

export function getSecureData(path, token) {
    return function (dispatch) {
        dispatch(fetchingSecureData(path))
        axios({
            url: `/v1/secret/${path}`,
            headers: {'X-Cerberus-Token': token},
            timeout: 60 * 1000 // 1 minute
        })
        .then((response) => {
            dispatch(storeSecureData(path, response.data.data))
        })
        .catch((response) => {
            log.error("Failed to fetch Secure Data", response)
            dispatch(messengerActions.addNewMessage(<ApiError message={`Failed to Fetch Secret Path: ${path}`} response={response} />))
        })
    }
}

export function fetchingSecureData(path) {
    return {
        type: actions.FETCHING_SECURE_DATA,
        payload: path
    }
}

export function storeSecureData(path, data) {
    return {
        type: actions.FETCHED_SECURE_DATA,
        payload: {
            key: path,
            data: data
        }
    }
}

export function removeSecureDataFromLocalStore(key) {
    return {
        type: actions.REMOVE_SECRET_FROM_LOCAL_STORE,
        payload: key
    }
}

export function removeKeyForSecureDataNodeFromLocalStore(key) {
    return {
        type: actions.REMOVE_KEY_FOR_SECURE_DATA_FROM_LOCAL_STORE,
        payload: key
    }
}

export function showAddNewSecureData() {
    return {
        type: actions.SHOW_ADD_SECRET_FORM
    }
}

export function hideAddNewSecureData() {
    return {
        type: actions.HIDE_ADD_SECRET_FORM
    }
}

export function commitSecret(navigatedPath, data, token, isNewSecureDataPath) {
    let secureData = {}
    let key = data.path
    let fullPath = `${navigatedPath}${key}`

    data.kvMap.map((entry) => {
        secureData[entry.key] = entry.value
    })

    return function (dispatch) {
        let nestedFolder = key.split('/').length > 1;
        // let the UI / state know that we are saving the secret, so that the user can't spam the save button
        // but only if not a nested folder
        if (! nestedFolder) {
            dispatch(savingSecureData(fullPath))
        }

        // save the secret
        axios({
            method: 'post',
            url: `/v1/secret/${fullPath}`,
            data: secureData,
            headers: {'X-Cerberus-Token': token},
            timeout: 60 * 1000 // 1 minute
        })
        .then((response) => {
            // once saved we can use the data we have locally to update the state, without additional API calls
            dispatch(updateLocalStateAfterSaveCommit(navigatedPath, key, secureData, isNewSecureDataPath))
        })
        .catch((response) => {
            log.error("Failed to save Secure Data", response)
            dispatch(messengerActions.addNewMessage(<ApiError message={`Failed to save Secret on Path: ${fullPath}`} response={response} />))
        })
    }
}

/**
 * After posting a secret to Cerberus we can update the local state to represent the new data
 *
 * @param prefix, the partial path to Cerberus Node / Secret (Sort of like a virtual folder)
 * @param key, the key to the Cerberus Node / Secret
 * @param data, the Cerberus secure data that was saved, we can store this and save an API call
 * @param isNewSecureDataPath, boolean, if its a new secret we need to hide the add new form
 */
export function updateLocalStateAfterSaveCommit(prefix, key, data, isNewSecureDataPath) {
    return (dispatch) => {
        // we will only store the data, if its not in a nested virtual folder
        let addSecretData = true;

        // Update the piece of state that stores the keys that are available for the given navigated path.
        // if a user added a nested secret that is in a folder
        // ex: foo/bar/bam we would want to add foo/ to the state, so the folder foo shows up in the ui
        let pieces = key.split('/')
        if (pieces.length > 1) {
            key = pieces[0] + '/'
            addSecretData = false
        }
        dispatch(updateStoredKeys(key))

        // Update the stored Cerberus secure data for the given complete path, if the key is not a virtual folder
        if (addSecretData) {
            log.info('ADDING KEY DATA')
            dispatch(storeSecureData(`${prefix}${key}`, data))
        }

        // if this is a new secure data path remove the create new form
        if (isNewSecureDataPath) {
            dispatch(hideAddNewSecureData())
        }

        // finally lets let the user know we saved there data
        dispatch(messengerActions.addNewMessageWithTimeout(`Successfully saved Secret at path: ${prefix}${key}`, 2500))
    }
}

export function deleteSecureDataPathConfirm(navigatedPath, label, token) {
    return (dispatch) => {
        let yes = () => {
            dispatch(deleteSecureDataPath(navigatedPath, label, token))
            dispatch(modalActions.popModal())
        }

        let no = () => {
            dispatch(modalActions.popModal())
        }

        let comp = <ConfirmationBox handleYes={yes}
                                    handleNo={no}
                                    message="Are you sure you want to delete this Secret Path."/>

        dispatch(modalActions.pushModal(comp))

    }
}

export function deleteSecureDataPath(navigatedPath, label, token) {
    return function (dispatch) {
        axios({
            method: 'delete',
            url: `/v1/secret/${navigatedPath}${label}`,
            headers: {'X-Cerberus-Token': token},
            timeout: 60 * 1000 // 1 minute
        })
            .then((response) => {
                dispatch(removeSecureDataFromLocalStore(`${navigatedPath}${label}`))
                dispatch(removeKeyForSecureDataNodeFromLocalStore(label))
            })
            .catch((response) => {
                log.error("Failed to delete Secure Data", response)
                dispatch(messengerActions.addNewMessage(<ApiError message={`Failed to delete Secret: ${path}`}
                                                                  response={response}/>))
            })
    }
}

export function deleteSDBConfirm(sdbId, token) {
    return (dispatch) => {
        let yes = () => {
            dispatch(deleteSDB(sdbId, token))
            dispatch(modalActions.popModal())
        }
        
        let no = () => {
            dispatch(modalActions.popModal())
        }
        
        let comp = <ConfirmationBox handleYes={yes} 
                                    handleNo={no} 
                                    message="Are you sure you want to delete this Safe Deposit Box."/>
        
        dispatch(modalActions.pushModal(comp))
        
    }
}

export function deleteSDB(sdbId, token) {
    return function(dispatch) {
        return axios({
            method: 'delete',
            url: `${environmentService.getDomain()}${cms.BUCKET_RESOURCE}/${sdbId}`,
            headers: {'X-Cerberus-Token': token}
        })
        .then((response) => {
            log.debug("Deleted SDB", response)
            dispatch(resetToInitialState())
            dispatch(authActions.refreshAuth(token, `/`))
        })
        .catch((response) => {
            log.error("Failed to delete SDB", response)
            dispatch(messengerActions.addNewMessage(<ApiError message="Failed to delete SDB Data from CMS" response={response} />))
        })
    }
}

export function submitEditSDBRequest(sdbId, data, token) {

    let formData = humps.decamelizeKeys(data)

    log.debug("submitting data to edit sdb cms endpoint\n" + JSON.stringify(formData, null, 2))

    return function(dispatch) {
        dispatch(submittingEditSDBRequest())
        axios({
            method: 'put',
            url: `${environmentService.getDomain()}${cms.BUCKET_RESOURCE}/${sdbId}`,
            headers: {'X-Cerberus-Token': token},
            data: formData,
            timeout: 10 * 1000 // 10 seconds
        })
        .then(function(response) {
            dispatch(fetchSDBDataFromCMS(sdbId, token))
            dispatch(modalActions.popModal())
            dispatch(resetSubmittingEditSDBRequest())
        })
        .catch(function (response) {
            log.error('Failed to edit SDB', response)
            dispatch(messengerActions.addNewMessage(<ApiError message="Failed to edit SDB" response={response} />))
            dispatch(resetSubmittingEditSDBRequest())
        })
    }
}

export function submittingEditSDBRequest() {
    return {
        type: actions.SUBMITTING_EDIT_SDB_REQUEST
    }
}

export function resetSubmittingEditSDBRequest() {
    return {
        type: actions.RESET_SUBMITTING_EDIT_SDB_REQUEST
    }
}

export function savingSecureData(path) {
    return {
        type: actions.SAVING_SECURE_DATA,
        payload: path
    }
}

export function updateStoredKeys(key) {
    return {
        type: actions.ADD_SECURE_DATA_KEY_IF_NOT_PRESET,
        payload: key
    }
}

export function resetToInitialState() {
    return {
        type: actions.RESET_SDB_DATA
    }
}
