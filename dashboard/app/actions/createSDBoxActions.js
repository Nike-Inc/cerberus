import React from 'react'
import axios from 'axios'
import * as constants from '../constants/actions'
import * as cms from '../constants/cms'
import * as authActions from '../actions/authenticationActions'
import environmentService from 'EnvironmentService'
import * as messengerActions from '../actions/messengerActions'
import * as modalActions from '../actions/modalActions'
import ApiError from '../components/ApiError/ApiError'
import * as humps from 'humps'

import { getLogger } from 'logger'
var log = getLogger('create-new-sdb-actions')

export function submitCreateNewSDB(data, token) {

    let formData = humps.decamelizeKeys(data)

    log.debug("submitting data to create sdb cms endpoint\n" + JSON.stringify(formData, null, 2))

    return function(dispatch) {
        dispatch(submittingNewSDBRequest())
        axios({
            method: 'post',
            url: `${environmentService.getDomain()}${cms.BUCKET_RESOURCE}`,
            headers: {'X-Vault-Token': token},
            data: formData,
            timeout: 10 * 1000 // 10 seconds
        })
        .then(function(response) {
            dispatch(modalActions.popModal())
            dispatch(authActions.refreshAuth(token, `/manage-safe-deposit-box/${response.data.id}`))
            dispatch(clearVaultData())
        })
        .catch(function (response) {
            log.error('Failed to create new SDB', response)
            dispatch(messengerActions.addNewMessage(<ApiError message="Failed to create new SDB" response={response} />))
            dispatch(resetSubmittingNewSDBRequest())
        })
    }
}

export function initCreateNewSDB(categoryId) {
    return {
        type: constants.CREATE_NEW_SDB_INIT,
        payload: { categoryId: categoryId }
    }
}

export function submittingNewSDBRequest() {
    return {
        type: constants.SUBMITTING_NEW_SDB_REQUEST
    }
}

export function resetSubmittingNewSDBRequest() {
    return {
        type: constants.RESET_SUBMITTING_NEW_SDB_REQUEST
    }
}

export function clearVaultData() {
    return {
        type: constants.CLEAR_VAULT_DATA
    }
}
