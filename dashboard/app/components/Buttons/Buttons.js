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