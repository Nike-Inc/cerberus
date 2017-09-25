import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'
import { connect } from 'react-redux'
import { reduxForm } from 'redux-form'
import { loginUser } from '../../actions/authenticationActions'

const formName = 'login-user-form'

/**
 * Component used to authenticate users before  to the dashboard.
 */
export const fields = ['username', 'password']

const isValidEmailRegex = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i

// define our client side form validation rules
const validate = values => {
    const errors = {}

    if (! values.username) {
        errors.username = 'Required'
    } else if (!isValidEmailRegex.test(values.username)) {
        errors.username = 'Invalid email address'
    }

    if (! values.password) {
        errors.password = 'Required'
    }

    return errors
}

@connect((state) => {
    return {
        isAuthenticating: state.auth.isAuthenticating,
        statusText: state.auth.statusText,
        initialValues: {
            redirectTo: state.routing.locationBeforeTransitions.query.next || '/'
        }
    }
})

// wire up the redux form
@reduxForm(
    {
        form: formName,
        fields: fields,
        validate
    }
)

export default class LoginUserForm extends Component {
    static propTypes = {
        fields: PropTypes.object.isRequired,
        handleSubmit: PropTypes.func.isRequired,
        isAuthenticating: PropTypes.bool.isRequired,
        dispatch: PropTypes.func.isRequired,
    }

    render() {
        const {fields: {username, password}, handleSubmit, isAuthenticating, dispatch} = this.props

        return (
            <form id={formName} onSubmit={handleSubmit( data => {
                        dispatch(loginUser(data.username, data.password))
                    })}>
                <div id='email-div' className='ncss-form-group'>
                    <div className={((username.touched && username.error) ? 'ncss-input-container error' : 'ncss-input-container')}>
                        <label className='ncss-label'>Email</label>
                        <input type='text'
                               className='ncss-input pt2-sm pr4-sm pb2-sm pl4-sm'
                               placeholder='Please enter your email address'
                               {...username}/>
                        {username.touched && username.error && <div className='ncss-error-msg'>{username.error}</div>}
                    </div>
                </div>
                <div id='pass-div' className='ncss-form-group'>
                    <div className={((password.touched && password.error) ? 'ncss-input-container error' : 'ncss-input-container')}>
                        <label className='ncss-label'>Password</label>
                        <input type='password'
                               className='ncss-input pt2-sm pr4-sm pb2-sm pl4-sm r'
                               placeholder='Please enter your password'
                               {...password}/>
                        {password.touched && password.error && <div className='ncss-error-msg'>{password.error}</div>}
                    </div>
                </div>
                <div id='login-form-submit-container'>
                    <div id='fountainG' className={isAuthenticating ? 'show-me' : 'hide-me'}>
                        <div id='fountainG_1' className='fountainG'></div>
                        <div id='fountainG_2' className='fountainG'></div>
                        <div id='fountainG_3' className='fountainG'></div>
                        <div id='fountainG_4' className='fountainG'></div>
                        <div id='fountainG_5' className='fountainG'></div>
                        <div id='fountainG_6' className='fountainG'></div>
                        <div id='fountainG_7' className='fountainG'></div>
                        <div id='fountainG_8' className='fountainG'></div>
                    </div>
                    <div id="login-help">
                        <a target="_blank" href="/dashboard/help/index.html">Need help?</a>
                    </div>
                    <button id='login-btn'
                            className='ncss-btn-offwhite ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                            disabled={isAuthenticating}>Login</button>
                </div>
            </form>
        )
    }
}









