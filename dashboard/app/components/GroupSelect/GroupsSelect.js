import React from 'react'
import { Component } from 'react'
import Select from 'react-select'
import log from 'logger'

export default class GroupsSelect extends Component {

    render() {
        const {userGroups, value, onChange, handleBeingTouched, touched, error} = this.props

        var options = userGroups.map(function(group) {
            return {label: group, value: group}
        })

        var containsValue = false
        options.map(option => {
            if (option.value == value) {
                containsValue = true
            }
        })

        if (! containsValue) {
            options.unshift({
                label: value,
                value: value
            })
        }

        return (
            <div className='group-select ncss-form-group'>
                <Select.Creatable
                    className={((touched && error) ? 'category-select select-container-error' : 'category-select select-container')}
                    onChange = {(v) => { handleBeingTouched(); onChange(v)}}
                    onBlur={() => { handleBeingTouched() }}
                    value={value}
                    placeholder="Select a user group"
                    options={options} />
                {touched && error && <div className='select-error-msg'>{error}</div>}
            </div>
        )
    }
}