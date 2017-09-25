import React from 'react'
import { Component } from 'react'
import Select from 'react-select'

export default class MfaDeviceSelect extends Component {

    componentDidMount() {
        var mfaDevices = this.props.mfaDevices;
        if (!this.props.value && mfaDevices.length > 0) {
            // choose the first value by default
            this.props.onChange(mfaDevices[0].id);
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
                    onChange = {(v) => { handleBeingTouched(); onChange(v)} }
                    onBlur={() => { handleBeingTouched() }}
                    value={value}
                    placeholder="Select a MFA device"
                    options={options} />
                {touched && error && <div className='select-error-msg'>{error}</div>}
            </div>
        )
    }
}