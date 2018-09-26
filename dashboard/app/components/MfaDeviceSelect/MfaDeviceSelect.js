import React from 'react'
import { Component } from 'react'
import Select from 'react-select'
import { setSelectedDeviceId } from '../../actions/authenticationActions'

export default class MfaDeviceSelect extends Component {

    componentDidMount() {
        var mfaDevices = this.props.mfaDevices;
        if (!this.props.value && mfaDevices.length > 0) {

            // choose the first value by default
            const selectedDeviceId = mfaDevices[0].id
            this.props.onChange(selectedDeviceId)
            this.props.dispatch(setSelectedDeviceId(selectedDeviceId))
        }
    }

    render() {
        const {mfaDevices, value, onChange, handleBeingTouched, touched, error} = this.props

        var options = mfaDevices.map(function(mfaDevice) {
            return {label: mfaDevice.name, value: mfaDevice.id}
        })

        return (
            <div className='mfa-device-select'>
                <Select
                    className={((touched && error) ? 'category-select select-container-error' : 'category-select select-container')}
                    onChange = {(selectedFactor) => {
                        this.props.dispatch(setSelectedDeviceId(selectedFactor.value))
                        handleBeingTouched(); onChange(selectedFactor)}
                    }
                    onBlur={() => { handleBeingTouched() }}
                    value={value}
                    placeholder="Select a MFA device"
                    options={options} />
                {touched && error && <div className='select-error-msg'>{error}</div>}
            </div>
        )
    }
}