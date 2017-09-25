import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'
import { connect } from 'react-redux'
import { reduxForm, touch } from 'redux-form'
import { finalizeMfaLogin } from '../../actions/authenticationActions'
import MfaDeviceSelect from '../MfaDeviceSelect/MfaDeviceSelect'
import './LoginFormMfa.scss'

const formName = 'login-mfa-form'

/**
 * This is the smart component for the create new bucket page,
 * This component contains the actual for and handle passing needed things into the dumb component views
 */
export const fields = ['otpToken', 'mfaDeviceId']

// define our client side form validation rules
const validate = values => {
    const errors = {}

    if (! values.otpToken) {
        errors.otpToken = 'Required'
    }
    if (! values.mfaDeviceId) {
        errors.mfaDeviceId = 'Required'
    }

    return errors
}

@connect((state) => {
    return {
        mfaDevices: state.auth.mfaDevices,
        stateToken: state.auth.stateToken,
        isAuthenticating: state.auth.isAuthenticating,
        statusText: state.auth.statusText,
        initialValues: {
            redirectTo: state.routing.locationBeforeTransitions.query.next || '/',
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

export default class LoginMfaForm extends Component {
    static propTypes = {
        fields: PropTypes.object.isRequired,
        stateToken: PropTypes.string.isRequired,
        isAuthenticating: PropTypes.bool.isRequired,
        dispatch: PropTypes.func.isRequired,
    }

    render() {
        const {fields: {otpToken, mfaDeviceId}, stateToken, handleSubmit, isAuthenticating, mfaDevices, dispatch} = this.props

        return (
            <div >
                <h3 id="mfa-required-header" className="text-color-">MFA is required.</h3>
                <form id={formName} onSubmit={handleSubmit( data => {
                            dispatch(finalizeMfaLogin(data.otpToken, data.mfaDeviceId, stateToken))
                        })}>
                    <div>

                        <label className='ncss-label'>MFA Devices:</label>
                        <div id='top-section'>
                            <MfaDeviceSelect {...mfaDeviceId}
                                             mfaDevices={mfaDevices}
                                             handleBeingTouched={() => {dispatch(touch(formName, 'mfaDeviceId'))}} />
                        </div>

                        <div id='security-code-div' className='ncss-form-group'>
                            <div className={((otpToken.touched && otpToken.error) ? 'ncss-input-container error' : 'ncss-input-container')}>
                                <label className='ncss-label'>Security Code</label>
                                <input type='text'
                                       className='ncss-input pt2-sm pr4-sm pb2-sm pl4-sm r'
                                       placeholder='Please enter your OTP token'
                                       autoFocus={true}
                                       {...otpToken}/>
                                {otpToken.touched && otpToken.error && <div className='ncss-error-msg'>{otpToken.error}</div>}
                            </div>
                        </div>

                    </div>
                    <div id='login-form-submit-container'>
                        <div id="login-help">
                            <a target="_blank" href="/dashboard/help/index.html">Need help?</a>
                        </div>
                        <button id='login-mfa-btn'
                                type='submit'
                                className='ncss-btn-offwhite ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                                disabled={isAuthenticating}>Login</button>
                    </div>
                </form>
            </div>
        )
    }
}









