import { createReducer } from '../utils'
import * as constants from '../constants/actions'

const initialState = {
    metadata: {},
    perPage: 100,
    pageNumber: 0
}

export default createReducer(initialState, {
    [constants.STORE_METADATA]: (state, payload) => {
        return Object.assign({}, state, {
            metadata: payload.metadata
        })
    },

    [constants.UPDATE_METADATA_PER_PAGE]: (state, payload) => {
        return Object.assign({}, state, {
            perPage: payload.perPage
        })
    },

    [constants.UPDATE_METADATA_PAGE_NUMBER]: (state, payload) => {
        return Object.assign({}, state, {
            pageNumber: payload.pageNumber
        })
    }
})
