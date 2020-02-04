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
