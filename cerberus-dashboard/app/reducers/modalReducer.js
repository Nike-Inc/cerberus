import { createReducer } from '../utils'
import * as constants from '../constants/actions'
import { getLogger } from 'logger'
var log = getLogger('modal')

const initialState = {
    modalStack: []
}

export default createReducer(initialState, {
    [constants.PUSH_MODAL]: (state, payload) => {
        let newStack = []

        let modal = payload.modalComponent
        state.modalStack.map(modal => newStack.push(modal))        

        newStack.push(modal)

        return Object.assign({}, state, {
            modalStack: newStack
        })
    },

    [constants.POP_MODAL]: (state) => {
        let newStack = []
        state.modalStack.map(modal => newStack.push(modal))
        newStack.pop()
        
        return Object.assign({}, state, {
            modalStack: newStack
        })
    },

    [constants.CLEAR_ALL_MODALS]: (state) => {

        return Object.assign({}, state, {
            modalStack: []
        })
    }
})
