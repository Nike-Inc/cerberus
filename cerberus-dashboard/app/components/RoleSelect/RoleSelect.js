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

export default class RoleSelect extends Component {

    render() {
        const {roles, value, onChange, handleBeingTouched, touched, error} = this.props

        var options = roles.map(function(permission) {
            let option = {label: permission.name, value: permission.id}
            return option
        })

        return (
            <div className='role-select'>
                <Select
                    className={((touched && error) ? 'category-select select-container-error' : 'category-select select-container')}
                    onChange = {(v) => { handleBeingTouched(); onChange(v)} }
                    onBlur={() => { handleBeingTouched() }}
                    placeholder="Select a permission"
                    value={value}
                    options={options} />
                {touched && error && <div className='select-error-msg'>{error}</div>}
            </div>
        )
    }
}