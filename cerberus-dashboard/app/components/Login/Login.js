import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'
import { connect } from 'react-redux'
import * as messengerActions from '../../actions/messengerActions'
import Messenger from '../Messenger/Messenger'
import './Login.scss'
import LoginUserForm from '../LoginUserForm/LoginUserForm'
import LoginMfaForm from '../LoginMfaForm/LoginMfaForm'

// connect to the store for the pieces we care about
@connect((state) => {
    return {
        isSessionExpired: state.auth.isSessionExpired,
        isMfaRequired: state.auth.isMfaRequired,
        statusText: state.auth.statusText,
        initialValues: {
            redirectTo: state.routing.locationBeforeTransitions.query.next || '/'
        }
    }
})

export default class LoginForm extends Component {
    static propTypes = {
        dispatch: PropTypes.func.isRequired,
        isMfaRequired: PropTypes.bool.isRequired,
    }

    componentDidMount() {
        this.props.dispatch(messengerActions.clearAllMessages())
    }
    render() {
        const {isSessionExpired, isMfaRequired} = this.props

        return (
            <div id='login-container' className=''>
                <div id='login-form-div'>
                    <header>
                        <div id='logo-container'>
                            <div className='cerberus-logo'></div>
                        </div>
                        <h1 className='ncss-brand'>CERBERUS MANAGEMENT DASHBOARD TEST</h1>

                    </header>
                    { isSessionExpired &&
                        <div id="session-expired-message">
                            Your session has expired and you are required to re-authenticate
                        </div>
                    }
                    <Messenger />
                    { !isMfaRequired && <LoginUserForm /> }
                    { isMfaRequired && <LoginMfaForm /> }
                </div>
            </div>
        )
    }
}
