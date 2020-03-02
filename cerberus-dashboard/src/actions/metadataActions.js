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
import * as actions from '../constants/actions';
import axios from 'axios';
import ApiError from '../components/ApiError/ApiError';
import environmentService from '../service/EnvironmentService';
import * as cms from '../constants/cms';
import * as messengerActions from './messengerActions';
import { getLogger } from "../utils/logger";
var log = getLogger('metadata');

export function fetchMetadata(token, pageNumber, perPage) {
    return function (dispatch) {
        return axios({
            url: environmentService.getDomain() + cms.RETRIEVE_METADATA,
            params: {
                limit: perPage,
                offset: Math.ceil(pageNumber * perPage)
            },
            headers: { 'X-Cerberus-Token': token },
            timeout: 10000
        })
            .then(function (response) {
                let metadata = response.data;
                if (metadata) {
                    dispatch(storeMetadata(metadata));
                    window.scrollTo(0, 0);
                } else {
                    log.warn("Metadata was null or undefined");
                }
            })
            .catch(function (response) {
                log.error('Failed to get metadata', response);
                dispatch(messengerActions.addNewMessage(<ApiError message="Failed to retrieve metadata" response={response} />));
            });
    };
}

function storeMetadata(metadata) {
    return {
        type: actions.STORE_METADATA,
        payload: {
            metadata: metadata
        }
    };
}

export function updatePerPage(perPage) {
    return {
        type: actions.UPDATE_METADATA_PER_PAGE,
        payload: {
            perPage: perPage
        }
    };
}

export function updatePageNumber(pageNumber) {
    return {
        type: actions.UPDATE_METADATA_PAGE_NUMBER,
        payload: {
            pageNumber: pageNumber
        }
    };
}
