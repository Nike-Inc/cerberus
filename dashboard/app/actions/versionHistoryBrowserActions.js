import React from 'react'
import axios from 'axios'
import * as actions from '../constants/actions'
import * as messengerActions from '../actions/messengerActions'
import ApiError from '../components/ApiError/ApiError'

import log from 'logger'

export function fetchVersionDataForSdb(safeDepositBodId, token) {
    return function(dispatch) {
        return axios({
            url: `/v1/sdb-secret-version-paths/${safeDepositBodId}`,
            headers: {'X-Cerberus-Token': token},
            timeout: 60 * 1000 // 1 minute
        })
        .then((response) => {
            dispatch(updatePathsWithHistory(response.data))
        })
        .catch((response) => {
            let msg = 'Failed to fetch version data for sdb'
            log.error(msg, response)
            dispatch(messengerActions.addNewMessage(<ApiError message={msg} response={response} />))
        })
    }
}

export function updatePathsWithHistory(paths) {
    return {
        type: actions.FETCHED_VERSION_DATA_FOR_SDB,
        payload: paths
    }
}

export function fetchPathVersionData(path, token, pageNumber, perPage) {
    return function(dispatch) {
        return axios({
            url: `/v1/secret-versions/${path}`,
            params: {
                limit: perPage,
                offset: Math.ceil(pageNumber * perPage)
            },
            headers: {'X-Cerberus-Token': token},
            timeout: 60 * 1000 // 1 minute
        })
        .then((response) => {
            dispatch(updateVersionDataForPath(path, response.data))
        })
        .catch((response) => {
            let msg = 'Failed to fetch path version data'
            log.error(msg, response)
            dispatch(messengerActions.addNewMessage(<ApiError message={msg} response={response} />))
        })
    }
}

export function updateVersionDataForPath(path, data) {
    return {
        type: actions.FETCHED_VERSION_DATA_FOR_PATH,
        payload: {
            data: data,
            path: path
        }
    }
}

export function handleBreadCrumbHomeClick() {
    return {
        type: actions.CLEAR_VERSION_PATH_SELECTED,
    }
}


export function updatePerPage(perPage) {
    return {
        type: actions.UPDATE_VERSION_PATHS_PER_PAGE,
        payload: {
            perPage: perPage
        }
    }
}

export function updatePageNumber(pageNumber) {
    return {
        type: actions.UPDATE_VERSION_PATHS_PAGE_NUMBER,
        payload: {
            pageNumber: pageNumber
        }
    }
}

export function resetVersionBrowserState() {
    return {
        type: actions.RESET_VERSION_BROWSER_STATE
    }
}

export function fetchVersionedSecureDataForPath(path, versionId, token) {
    let request = {
        url: `/v1/secret/${path}`,
        headers: {'X-Cerberus-Token': token},
        timeout: 60 * 1000 // 1 minute
    }

    if (versionId !== 'CURRENT') {
        request['params'] = {
            versionId: versionId
        }
    }

    return function(dispatch) {
        return axios(request)
        .then((response) => {
            dispatch(updateVersionedSecureDataForPath(versionId, response.data.data))
        })
        .catch((response) => {
            let msg = 'Failed to fetch versioned secure data for path'
            log.error(msg, response)
            dispatch(messengerActions.addNewMessage(<ApiError message={msg} response={response} />))
        })
    }
}

export function updateVersionedSecureDataForPath(versionId, secureData) {
    return {
        type: actions.ADD_SECURE_DATA_FOR_VERSION,
        payload: {
            versionId: versionId,
            secureData: secureData
        }
    }
}