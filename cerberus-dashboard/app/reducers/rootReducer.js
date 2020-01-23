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

import { combineReducers } from 'redux'
import { routerReducer } from 'react-router-redux'
import { reducer as formReducer } from 'redux-form'
import auth from './authenticationReducer'
import header from './headerStateReducer'
import app from './appReducer'
import nSDB from './createSDBoxReducer'
import messenger from './messengerReducer'
import manageSafetyDepositBox from './manageSafetyDepositBoxReducer'
import metadata from './metadataReducer'
import modal from './modalReducer'
import versionHistoryBrowser from './versionHistoryBrowserReducer'

export default combineReducers({
    auth,
    header,
    app,
    nSDB,
    manageSafetyDepositBox,
    modal,
    routing: routerReducer,
    form: formReducer,
    messenger,
    metadata,
    versionHistoryBrowser
})