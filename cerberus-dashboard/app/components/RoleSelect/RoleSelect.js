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