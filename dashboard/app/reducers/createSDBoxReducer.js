import { createReducer } from '../utils'
import * as constants from '../constants/actions'

const initialState = {
    selectedCategoryId: null,
    isSubmitting: false
}

/**
 * Reducer / State for managing state of create new sdb form state
 */
export default createReducer(initialState, {
    // sets the preselected category info when a user clicks the plus next to a category to create a new SDB
    [constants.CREATE_NEW_SDB_INIT]: (state, payload) => {
        return Object.assign({}, state, {
            selectedCategoryId: payload.categoryId,
            isSubmitting: false
        })
    },
    [constants.SUBMITTING_NEW_SDB_REQUEST]: (state) => {
        return Object.assign({}, state, {
            isSubmitting: true
        })
    },
    [constants.RESET_SUBMITTING_NEW_SDB_REQUEST]: (state) => {
        return Object.assign({}, state, {
            isSubmitting: false
        })
    }
})
