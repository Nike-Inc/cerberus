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
import './GenericError.scss'

/**
 * A component to use to make API messages sent to the messenger look pretty and be html and stylable
 *
 * @prop message The Message for this app to provide context to the user for what action failed
 * @prop response The Axios response
 */
export default class GenericError extends Component {
    static propTypes = {
        errorHeader: PropTypes.string.isRequired,
        message: PropTypes.string.isRequired
    }

    render() {
        const {message, errorHeader} = this.props
        return (
            <div className="generic-error-wrapper">
                <div className="generic-error-header">{errorHeader}</div>
                <div className="generic-error-message-wrapper">
                    <div className="generic-error-message-label">Message:</div>
                    <div className="generic-error-message">{message}</div>
                </div>
            </div>
        )
    }
}