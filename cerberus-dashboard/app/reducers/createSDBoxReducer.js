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
