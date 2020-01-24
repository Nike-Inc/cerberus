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
import log from 'logger'

const initialState = {
    messages: []
}

export default createReducer(initialState, {
    [constants.ADD_MESSAGE]: (state, payload) => {
        let currentMessages = state.messages
        let newMessages = []

        currentMessages.forEach((message) => {
            newMessages.push(message)
        })

        newMessages.push({
            message: payload.message,
            id: payload.id
        })

        log.info("New Messages", newMessages)
        
        return Object.assign({}, state, {
            messages: newMessages
        })
    },
    [constants.REMOVE_MESSAGE]: (state, payload) => {
        let currentMessages = state.messages
        let newMessages = []
        //noinspection JSUnresolvedVariable
        let idToRemove = payload

        currentMessages.forEach((message) => {
            if (message.id != idToRemove) {
                newMessages.push(message)
            }
        })

        return Object.assign({}, state, {
            messages: newMessages
        })
    },
    [constants.CLEAR_ALL_MESSAGES]: (state) => {
        return Object.assign({}, state, {
            messages: []
        })
    }
})
