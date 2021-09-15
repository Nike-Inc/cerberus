/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        });

        let selected = options.find(option => option.value === value);

        return (
            <div className='mfa-device-select'>
                <Select
                    className={((touched && error) ? 'category-select select-container-error' : 'category-select select-container')}
                    onChange = {(selectedFactor) => {
                        this.props.dispatch(setSelectedDeviceId(selectedFactor.value))
                        handleBeingTouched(); onChange(selectedFactor)
                    }}
                    onBlur={() => { handleBeingTouched() }}
                    value={selected}
                    placeholder="Select a MFA device"
                    options={options} />
                {touched && error && <div className='select-error-msg'>{error}</div>}
            </div>
        )
    }
}