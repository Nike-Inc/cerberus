import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'

import './Modal.scss'

import { getLogger } from 'logger'
var log = getLogger('modal')

export default class Modal extends Component {

    static propTypes = {
        modalStack: PropTypes.array.isRequired
    }

    render() {
        const {modalStack} = this.props

        if (modalStack.length < 1) {
            return(<div></div>)
        }

        let modal = modalStack[modalStack.length - 1]

        return (
            <div className="modal-container">
                <div className="modal-component-wrapper">
                    {modal}
                </div>
            </div>
        )
    }
}