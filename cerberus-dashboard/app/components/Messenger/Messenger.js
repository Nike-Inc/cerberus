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
