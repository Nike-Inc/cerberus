import { createReducer } from '../utils'
import * as constants from '../constants/actions'

const initialState = {
    sideBar: {
        hasLoaded: false,
        isFetching: false,
        data: {}
    },
    cmsDomainData: {
        hasLoaded: false,
        categories: [],
        roles: []
    },
    metadata: {
        hasLoaded: false,
        version: 'unknown'
    }
}

export default createReducer(initialState, {
    // keeps track of the ajax call for fetching the data needed for populating the side bar
    [constants.FETCHING_SIDE_BAR_DATA]: (state) => {
        return Object.assign({}, state, {
            sideBar: { isFetching: true }
        })
    },
    // lets the app know that the side bar data has been fetched and saves the data to the store
    [constants.FETCHED_SIDE_BAR_DATA]: (state, payload) => {
        return Object.assign({}, state, {
            sideBar: {
                hasLoaded: true,
                isFetching: false,
                data: payload
            }
        })
    },
    // stores the domain data from CMS
    [constants.STORE_DOMAIN_DATA]: (state, payload) => {
        return Object.assign({}, state, {
            cmsDomainData: {
                hasLoaded: true,
                categories: payload.categories,
                roles: payload.roles
            }
        })
    },

    [constants.RESET_SIDEBAR_DATA]: (state) => {
        return initialState
    },

    // stores the metadata about the dashboard into the state
    [constants.STORE_DASHBOARD_METADATA]: (state, payload) => {
        return Object.assign({}, state, {
            metadata: {
                hasLoaded: true,
                version: payload.version
            }
        })
    }
})
