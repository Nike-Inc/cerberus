import { createReducer } from '../utils'
import * as constants from '../constants/actions'

const initialState = {
    displayUserContextMenu: false
}

export default createReducer(initialState, {
    // stores data needed to let the dumb view know that the user has clicked the username to display the context menu
    [constants.USERNAME_CLICKED]: (state) => {
        return Object.assign({}, state, {
            'displayUserContextMenu': true
        })
    },
    // stores data needed to let the dumb view know when to hide the context menu
    [constants.MOUSE_OUT_USERNAME]: (state) => {
        return Object.assign({}, state, {
            'displayUserContextMenu': false
        })
    }
})
