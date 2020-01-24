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
