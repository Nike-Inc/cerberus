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

import * as constants from '../constants/actions'

/**
 * adds a message to the messenger
 * @param message A string or JSX element to be rendered in the messenger component
 */
export function addNewMessage(message, id) {
    window.scrollTo(0, 0)
    return {
        type: constants.ADD_MESSAGE,
        payload: {
            message: message,
            id: id ? id : guid()
        }
    }
}

export function addNewMessageWithTimeout(message, timeout) {
    return (dispatch => {
        let id = guid()

        dispatch(addNewMessage(message, id))
        setTimeout(() => {
            dispatch(removeMessage(id))
        }, timeout)
    })
}

export function removeMessage(id) {
    return {
        type: constants.REMOVE_MESSAGE,
        payload: id
    }
}

export function clearAllMessages() {
    return {
        type: constants.CLEAR_ALL_MESSAGES
    }
}

function guid() {
    function s4() {
        return Math.floor((1 + Math.random()) * 0x10000)
          .toString(16)
          .substring(1);
    }
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
      s4() + '-' + s4() + s4() + s4();
}