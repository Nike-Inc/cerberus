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
 * Event to dispatch when user mouses over username to trigger context menu to be shown
 */
export function mouseOverUsername() {
    return {
        type: constants.USERNAME_CLICKED
    }
}

/**
 * Event to dispatch when user mouses over username to trigger context menu to be hidden
 */
export function mouseOutUsername() {
    return {
        type: constants.MOUSE_OUT_USERNAME
    }
}