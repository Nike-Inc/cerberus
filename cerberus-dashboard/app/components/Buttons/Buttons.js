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

import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'
import './Buttons.scss'
import '../../assets/images/remove-red.svg'

/**
 * Dumb Component to display the buttons for a permissions row.
 *
 * @param index value to be passed to handleRemoveClicked when the remove button
 * @param handleRemoveClicked the function that will be called when the remove button is clicked
 */
// TODO make this be REMOVE button and generify for all components to be able to use
export default class PermissionButtons extends Component {
    static propTypes = {
        handleRemoveClicked: PropTypes.func.isRequired
    }

    render() {
        const {handleRemoveClicked} = this.props

        return(
            <div className='permissions-row-buttons'>
                <div className='permissions-row-buttons permission-remove' onClick={
                    handleRemoveClicked
                }></div>
            </div>
        )
    }
}