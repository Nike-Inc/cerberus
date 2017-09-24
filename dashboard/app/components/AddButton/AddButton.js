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
            <div className='permissions-add-new-permission-button-container clickable'
                 onClick={() => {
                     handleClick()
                 }}>
                <div className='permissions-add-new-permission-add-icon'></div>
                <div className='permissions-add-new-permission-add-label'>{message}</div>
            </div>
        )
    }
}
