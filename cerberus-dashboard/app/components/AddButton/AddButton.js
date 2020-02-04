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
import './AddButton.scss'
import '../../assets/images/add-green.svg'

/**
 * Component for an Add Button (a button with a plus and a message)
 */
export default class AddButton extends Component {
    static propTypes = {
        handleClick: PropTypes.func.isRequired,
        message: PropTypes.string.isRequired
    }

    render() {
        const {handleClick, message} = this.props
        return (
            <div className='permissions-add-new-permission-button-container btn ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                 onClick={() => {
                     handleClick()
                 }}>
                <div className='permissions-add-new-permission-add-icon ncss-glyph-plus-lg'></div>
                <div className='permissions-add-new-permission-add-label'>{message}</div>
            </div>
        )
    }
}
