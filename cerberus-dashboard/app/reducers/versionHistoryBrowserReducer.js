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

import * as action from '../constants/actions'
import { createReducer } from '../utils'

const initialState = {
    hasFetchedPathsWithHistory: false,
    pathsWithHistory: [],

    hasFetchedVersionPathData: false,
    versionPathSelected: '',
    versionPathData: {},
    versionPathPerPage: 5,
    versionPathPageNumber: 0,
    versionPathSecureDataMap: {}
}

export default createReducer(initialState, {
    [action.FETCHED_VERSION_DATA_FOR_SDB]: (state, payload) => {
        return Object.assign({}, state, {
            pathsWithHistory: payload,
            hasFetchedPathsWithHistory: true
        })
    },
    [action.FETCHED_VERSION_DATA_FOR_PATH]: (state, payload) => {
        return Object.assign({}, state, {
            hasFetchedVersionPathData: true,
            versionPathSelected: payload.path,
            versionPathData: payload.data
        })
    },
    [action.UPDATE_VERSION_PATHS_PER_PAGE]: (state, payload) => {
        return Object.assign({}, state, {
            versionPathPerPage: payload.perPage
        })
    },
    [action.UPDATE_VERSION_PATHS_PAGE_NUMBER]: (state, payload) => {
        return Object.assign({}, state, {
            versionPathPageNumber: payload.pageNumber
        })
    },
    [action.CLEAR_VERSION_PATH_SELECTED]: (state) => {
        return Object.assign({}, state, {
            hasFetchedVersionPathData: initialState.hasFetchedVersionPathData,
            versionPathSelected: initialState.versionPathSelected,
            versionPathData: initialState.versionPathData,
            versionPathPerPage: initialState.versionPathPerPage,
            versionPathPageNumber: initialState.versionPathPageNumber,
            versionPathSecureDataMap: initialState.versionPathSecureDataMap
        })
    },
    [action.RESET_VERSION_BROWSER_STATE]: () => {
        return initialState
    },
    [action.ADD_SECURE_DATA_FOR_VERSION]: (state, payload) => {
        let existingMap = state.versionPathSecureDataMap
        let newMap = {}
        let versionId = payload.versionId

        for (let key in existingMap) {
            if (existingMap.hasOwnProperty(key)) {
                newMap[key] = existingMap[key]
            }
        }
        newMap[versionId] = payload.secureData

        return Object.assign({}, state, {
            versionPathSecureDataMap: newMap
        })
    }
})