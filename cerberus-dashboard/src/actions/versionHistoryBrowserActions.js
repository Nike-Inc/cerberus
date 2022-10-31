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
import axios from 'axios';
import * as actions from '../constants/actions';
import * as messengerActions from './messengerActions';
import ApiError from '../components/ApiError/ApiError';
import downloadjs from "downloadjs";

import log from "../utils/logger";

export function fetchVersionDataForSdb(safeDepositBodId, token) {
    return function (dispatch) {
        return axios({
            url: `/v1/sdb-secret-version-paths/${safeDepositBodId}`,
            headers: { 'X-Cerberus-Token': token },
            timeout: 60 * 1000 // 1 minute
        })
            .then((response) => {
                dispatch(updatePathsWithHistory(response.data));
            })
            .catch(({ response }) => {
                let msg = 'Failed to fetch version data for sdb';
                log.error(msg, response);
                dispatch(messengerActions.addNewMessage(<ApiError message={msg} response={response} />));
            });
    };
}

export function updatePathsWithHistory(paths) {
    return {
        type: actions.FETCHED_VERSION_DATA_FOR_SDB,
        payload: paths
    };
}

export function fetchPathVersionData(path, token, pageNumber, perPage) {
    return function (dispatch) {
        return axios({
            url: `/v1/secret-versions/${path}`,
            params: {
                limit: perPage,
                offset: Math.ceil(pageNumber * perPage)
            },
            headers: { 'X-Cerberus-Token': token },
            timeout: 60 * 1000 // 1 minute
        })
            .then((response) => {
                dispatch(updateVersionDataForPath(path, response.data));
            })
            .catch(({ response }) => {
                let msg = 'Failed to fetch path version data';
                log.error(msg, response);
                dispatch(messengerActions.addNewMessage(<ApiError message={msg} response={response} />));
            });
    };
}

export function updateVersionDataForPath(path, data) {
    return {
        type: actions.FETCHED_VERSION_DATA_FOR_PATH,
        payload: {
            data: data,
            path: path
        }
    };
}

export function handleBreadCrumbHomeClick() {
    return {
        type: actions.CLEAR_VERSION_PATH_SELECTED,
    };
}


export function updatePerPage(perPage) {
    return {
        type: actions.UPDATE_VERSION_PATHS_PER_PAGE,
        payload: {
            perPage: perPage
        }
    };
}

export function updatePageNumber(pageNumber) {
    return {
        type: actions.UPDATE_VERSION_PATHS_PAGE_NUMBER,
        payload: {
            pageNumber: pageNumber
        }
    };
}

export function resetVersionBrowserState() {
    return {
        type: actions.RESET_VERSION_BROWSER_STATE
    };
}

export function fetchVersionedSecureDataForPath(path, versionId, token) {
    let request = {
        url: `/v1/secret/${path}`,
        headers: { 'X-Cerberus-Token': token },
        timeout: 60 * 1000 // 1 minute
    };

    if (versionId !== 'CURRENT') {
        request['params'] = {
            versionId: versionId
        };
    }

    return function (dispatch) {
        return axios(request)
            .then((response) => {
                dispatch(updateVersionedSecureDataForPath(versionId, response.data.data));
            })
            .catch(({ response }) => {
                let msg = 'Failed to fetch versioned secure data for path';
                log.error(msg, response);
                dispatch(messengerActions.addNewMessage(<ApiError message={msg} response={response} />));
            });
    };
}

export function downloadSecureFileVersion(path, versionId, token) {
    console.log("Version ID: " + versionId);
    let request = {
        url: `/v1/secure-file/${path}`,
        headers: {
            'X-Cerberus-Token': token,
        },
        responseType: 'blob',
        timeout: 60 * 1000 // 1 minute
    };

    if (versionId !== 'CURRENT') {
        request['params'] = {
            versionId: versionId
        };
    }

    return function (dispatch) {
        return axios(request)
            .then((response) => {
                let reader = new window.FileReader();
                reader.readAsArrayBuffer(response.data);
                reader.onload = function () {
                    let pathParts = path.split('/');
                    downloadjs(reader.result, pathParts[pathParts.length - 1]);
                };
                // dispatch(updateVersionedSecureDataForPath(versionId, new Uint8Array(response.data), 'FILE'))
            })
            .catch(({ response }) => {
                let msg = 'Failed to fetch version data for secure file path';
                log.error(msg, response);
                dispatch(messengerActions.addNewMessage(<ApiError message={msg} response={response} />));
            });
    };
}

export function updateVersionedSecureDataForPath(versionId, secureData, type) {
    return {
        type: actions.ADD_SECURE_DATA_FOR_VERSION,
        payload: {
            type: type,
            versionId: versionId,
            secureData: secureData
        }
    };
}
