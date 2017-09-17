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
            headers: {'X-Vault-Token': token}
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

export function fetchVaultPathKeys(path, token) {
    return function(dispatch) {
        dispatch(fetchingKeys())
        return axios({
            url: `/v1/secret/${path}`,
            params: {
                list: true
            },
            headers: {'X-Vault-Token': token},
            timeout: 60 * 1000 // 1 minute
        })
        .then((response) => {
            dispatch(updateVaultPathKeys(response.data.data.keys))
        })
        .catch((response) => {
            // no keys for the SDB Yet
            if (response.status == 404) {
                dispatch(updateVaultPathKeys([]))
            } else {
                log.error("Failed to fetch Vault Path Keys", response)
                dispatch(messengerActions.addNewMessage(<ApiError message={`Failed to Fetch list of keys for Path: ${path} from vault`} response={response} />))
            }
        })
    }
}

export function updateVaultPathKeys(keys) {
    return {
        type: actions.FETCHED_VAULT_KEYS,
        payload: keys
    }
}

export function fetchingKeys() {
    return {
        type: actions.FETCHING_VAULT_KEYS
    }
}

export function updateNavigatedPath(newPath, token) {
    return function(dispatch) {
        dispatch(storeNewPath(newPath))
        dispatch(fetchVaultPathKeys(newPath, token))
    }
}

export function storeNewPath(newPath) {
    return {
        type: actions.UPDATE_NAVIGATED_PATH,
        payload: newPath
    }
}

export function getVaultSecret(path, token) {
    return function (dispatch) {
        dispatch(fetchingVaultSecret(path))
        axios({
            url: `/v1/secret/${path}`,
            headers: {'X-Vault-Token': token},
            timeout: 60 * 1000 // 1 minute
        })
        .then((response) => {
            dispatch(storeVaultSecret(path, response.data.data))
        })
        .catch((response) => {
            log.error("Failed to fetch Vault Secret", response)
            dispatch(messengerActions.addNewMessage(<ApiError message={`Failed to Fetch Vault Path: ${path}`} response={response} />))
        })
    }
}

export function fetchingVaultSecret(path) {
    return {
        type: actions.FETCHING_VAULT_SECRET,
        payload: path
    }
}

export function storeVaultSecret(path, data) {
    return {
        type: actions.FETCHED_VAULT_SECRET,
        payload: {
            key: path,
            data: data
        }
    }
}

export function removeVaultSecretFromLocalStore(key) {
    return {
        type: actions.REMOVE_SECRET_FROM_LOCAL_STORE,
        payload: key
    }
}

export function removeVaultNodePathKeyFromLocalStore(key) {
    return {
        type: actions.REMOVE_VAULT_KEY_FROM_LOCAL_STORE,
        payload: key
    }
}

export function showAddNewVaultSecret() {
    return {
        type: actions.SHOW_ADD_SECRET_FORM
    }
}

export function hideAddNewVaultSecret() {
    return {
        type: actions.HIDE_ADD_SECRET_FORM
    }
}

export function commitSecret(navigatedPath, data, token, isNewVaultPath) {
    let vaultData = {}
    let key = data.path
    let fullPath = `${navigatedPath}${key}`

    data.kvMap.map((entry) => {
        vaultData[entry.key] = entry.value
    })

    return function (dispatch) {
        let nestedFolder = key.split('/').length > 1;
        // let the UI / state know that we are saving the secret, so that the user can't spam the save button
        // but only if not a nested folder
        if (! nestedFolder) {
            dispatch(savingVaultSecret(fullPath))
        }

        // save the secret
        axios({
            method: 'post',
            url: `/v1/secret/${fullPath}`,
            data: vaultData,
            headers: {'X-Vault-Token': token},
            timeout: 60 * 1000 // 1 minute
        })
        .then((response) => {
            // once saved we can use the data we have locally to update the state, without additional API calls
            dispatch(updateLocalStateAfterSaveCommit(navigatedPath, key, vaultData, isNewVaultPath))
        })
        .catch((response) => {
            log.error("Failed to save Vault Secret data", response)
            dispatch(messengerActions.addNewMessage(<ApiError message={`Failed to save Vault Secret data on Path: ${path}`} response={response} />))
        })
    }
}

/**
 * After posting a secret to Vault we can update the local state to represent the new data
 *
 * @param prefix, the partial path to Vault Node / Secret (Sort of like a virtual folder)
 * @param key, the key to the Vault Node / Secret
 * @param data, the Vault secret data that was saved, we can store this and save an API call
 * @param isNewVaultPath, boolean, if its a new secret we need to hide the add new form
 */
export function updateLocalStateAfterSaveCommit(prefix, key, data, isNewVaultPath) {
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

        // Update the stored vault secret data for the given complete path, if the key is not a virtual folder
        if (addSecretData) {
            log.info('ADDING KEY DATA')
            dispatch(storeVaultSecret(`${prefix}${key}`, data))
        }

        // if this is a new vault path remove the create new form
        if (isNewVaultPath) {
            dispatch(hideAddNewVaultSecret())
        }

        // finally lets let the user know we saved there data
        dispatch(messengerActions.addNewMessageWithTimeout(`Successfully saved Vault secret at path: ${prefix}${key}`, 2500))
    }
}

export function deleteVaultPathConfirm(navigatedPath, label, token) {
    return (dispatch) => {
        let yes = () => {
            dispatch(deleteVaultPath(navigatedPath, label, token))
            dispatch(modalActions.popModal())
        }

        let no = () => {
            dispatch(modalActions.popModal())
        }

        let comp = <ConfirmationBox handleYes={yes}
                                    handleNo={no}
                                    message="Are you sure you want to delete this Vault Path."/>

        dispatch(modalActions.pushModal(comp))

    }
}

export function deleteVaultPath(navigatedPath, label, token) {
    return function (dispatch) {
        axios({
            method: 'delete',
            url: `/v1/secret/${navigatedPath}${label}`,
            headers: {'X-Vault-Token': token},
            timeout: 60 * 1000 // 1 minute
        })
            .then((response) => {
                dispatch(removeVaultSecretFromLocalStore(`${navigatedPath}${label}`))
                dispatch(removeVaultNodePathKeyFromLocalStore(label))
            })
            .catch((response) => {
                log.error("Failed to delete Vault Secret", response)
                dispatch(messengerActions.addNewMessage(<ApiError message={`Failed to delete Vault Secret: ${path}`}
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
            headers: {'X-Vault-Token': token}
        })
        .then((response) => {
            log.debug("Deleted SDB", response)
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
            headers: {'X-Vault-Token': token},
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

export function savingVaultSecret(path) {
    return {
        type: actions.SAVING_VAULT_SECRET,
        payload: path
    }
}

export function updateStoredKeys(key) {
    return {
        type: actions.ADD_VAULT_KEY_IF_NOT_PRESET,
        payload: key
    }
}

export function resetToInitialState() {
    return {
        type: actions.RESET_SDB_DATA
    }
}
