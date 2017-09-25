import * as actions from '../constants/actions'
import axios from 'axios'
import ApiError from '../components/ApiError/ApiError'
import environmentService from 'EnvironmentService'
import * as cms from '../constants/cms'
import * as messengerActions from '../actions/messengerActions'
import { getLogger } from 'logger'
var log = getLogger('metadata')

export function fetchMetadata(token, pageNumber, perPage) {
    return function(dispatch) {
        return axios({
            url: environmentService.getDomain() + cms.RETRIEVE_METADATA,
            params: {
                limit: perPage,
                offset: Math.ceil(pageNumber * perPage)
            },
            headers: {'X-Vault-Token': token},
            timeout: 10000
        })
        .then(function (response) {
            let metadata = response.data
            if (metadata) {
                dispatch(storeMetadata(metadata))
                window.scrollTo(0, 0)
            } else {
                log.warn("Metadata was null or undefined")
            }
        })
        .catch(function (response) {
            log.error('Failed to get metadata', response)
            dispatch(messengerActions.addNewMessage(<ApiError message="Failed to retrieve metadata" response={response} />))
        })
    }
}

function storeMetadata(metadata) {
    return {
        type: actions.STORE_METADATA,
        payload: {
            metadata: metadata
        }
    }
}

export function updatePerPage(perPage) {
    return {
        type: actions.UPDATE_PER_PAGE,
        payload: {
            perPage: perPage
        }
    }
}

export function updatePageNumber(pageNumber) {
    return {
        type: actions.UPDATE_PAGE_NUMBER,
        payload: {
            pageNumber: pageNumber
        }
    }
}