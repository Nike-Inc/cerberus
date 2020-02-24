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
import { connect } from 'react-redux'
import * as messengerActions from '../../actions/messengerActions'
import './Messenger.scss'

@connect((state) => {
    return {
        messages: state.messenger.messages
    }
})
export default class Messenger extends Component {
    render() {
        const {messages, dispatch} = this.props

        if (messages.length == 0) {
            return (<div></div>)
        }

        return (
            <div id='messenger-container'>
                {messages.map((message, index) =>
                    <div className="messenger-message" key={index}>
                        <div className="messenger-message-content">{message.message}</div>
                        <div className="messenger-message-buttons">
                            <div className="messenger-message-buttons-acknowledge" onClick={() => {
                                dispatch(messengerActions.removeMessage(message.id))}
                            }></div>
                        </div>
                    </div>
                )}
            </div>
        )
    }
}
