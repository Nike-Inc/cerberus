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

import React from 'react';
import { Component } from 'react';
import Select from 'react-select';

import { validateADGroup } from '../CreateSDBoxForm/validator';

class GroupsSelect extends Component {

    render() {
        const { userGroups, allowCustomValues, value, onChange, handleBeingTouched, touched, error } = this.props;

        var options = [];
        userGroups.forEach(group => {
            if (validateADGroup(group)) {
                options.push({ label: group, value: group })
            }
        });

        var containsValue = false;
        options.forEach(option => {
            if (option.value === value) {
                containsValue = true;
            }
        });

        if (!containsValue) {
            options.unshift({
                label: value,
                value: value
            });
        }

        return (
            <div className='group-select ncss-form-group'>
                {allowCustomValues &&
                    <Select.Creatable
                        className={((touched && error) ? 'category-select select-container-error' : 'category-select select-container')}
                        onChange={(v) => { handleBeingTouched(); onChange(v); }}
                        onBlur={() => { handleBeingTouched(); }}
                        value={value}
                        placeholder="Select a user group"
                        options={options} />
                }
                {!allowCustomValues &&
                    <Select
                        className={((touched && error) ? 'category-select select-container-error' : 'category-select select-container')}
                        onChange={(v) => { handleBeingTouched(); onChange(v); }}
                        onBlur={() => { handleBeingTouched(); }}
                        value={value}
                        placeholder="Select a user group"
                        options={options} />
                }
                {touched && error && <div className='select-error-msg'>{error}</div>}
            </div>
        );
    }
}

export default GroupsSelect