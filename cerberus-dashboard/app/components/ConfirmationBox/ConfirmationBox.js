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

import './ConfirmationBox.scss'

export default class ConfirmationBox extends Component {
    static propTypes = {
        message: PropTypes.string.isRequired,
        handleYes: PropTypes.func.isRequired,
        handleNo: PropTypes.func.isRequired
    }

    render() {
        const {message, handleYes, handleNo} = this.props

        return (
            <div className="confirmation-box-container ncss-brand">
                <h2>Attention:</h2>
                <h4 className="confirmation-box-message">{message}</h4>
                <div className="confirmation-box-buttons">
                    <div className="confirmation-box-button ncss-btn-dark-grey  u-uppercase" onClick={handleNo}>No</div>
                    <div className="confirmation-box-button ncss-btn-dark-grey u-uppercase" onClick={handleYes}>Yes</div>
                </div>
            </div>
        )
    }
}