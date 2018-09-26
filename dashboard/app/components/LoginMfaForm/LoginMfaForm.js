import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'
import { connect } from 'react-redux'
import { reduxForm, touch } from 'redux-form'
import {finalizeMfaLogin, triggerCodeChallenge} from '../../actions/authenticationActions'
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
        shouldDisplaySendCodeButton: state.auth.shouldDisplaySendCodeButton,
        selectedDeviceId: state.auth.selectedDeviceId,
        mfaDevices: state.auth.mfaDevices,
        stateToken: state.auth.stateToken,
        isAuthenticating: state.auth.isAuthenticating,
        isChallengeSent: state.auth.isChallengeSent,
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
        isChallengeSent: PropTypes.bool.isRequired,
        dispatch: PropTypes.func.isRequired,
    }

    constructor(props) {
        super(props)

        this.handleMfaDeviceTouch = function(formName) {
            this.props.dispatch(touch(formName, 'mfaDeviceId'))
        }
    }

    render() {
        const {fields: {otpToken, mfaDeviceId}, stateToken, handleSubmit, isAuthenticating, isChallengeSent, mfaDevices,
                dispatch, selectedDeviceId, shouldDisplaySendCodeButton} = this.props

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
                                             dispatch={dispatch}
                                             mfaDevices={mfaDevices}
                                             handleBeingTouched={() => {this.handleMfaDeviceTouch(formName)}} />
                        </div>

                        <div id='otp-div' className='ncss-form-group'>
                            <div className={((otpToken.touched && otpToken.error) ? 'ncss-input-container error' : 'ncss-input-container')}>

                                <label className='ncss-label'>One-Time Passcode:</label>

                                <div id='otp-input-row'>
                                    {shouldDisplaySendCodeButton &&
                                        <button id={isChallengeSent ? 'sent-mfa-btn' : 'send-code-mfa-btn'} type='button' className='ncss-btn-offwhite ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'

                                               onClick={function () {
                                                dispatch(triggerCodeChallenge(selectedDeviceId, stateToken));
                                                }
                                            }
                                            disabled={isChallengeSent}
                                        >{isChallengeSent ? "Sent" : "Send Code"}</button>
                                    }

                                    <input type='text'
                                           className='ncss-input pt2-sm pr4-sm pb2-sm pl4-sm r'
                                           placeholder='Please enter your OTP token'
                                           autoComplete="off"
                                           autoFocus={true}
                                           {...otpToken}/>
                                </div>
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









