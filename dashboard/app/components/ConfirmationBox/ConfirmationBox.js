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