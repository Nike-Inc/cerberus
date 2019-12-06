import React from 'react'
import { Component } from 'react'
import Select from 'react-select'
import './CategorySelect.scss'

export default class CategorySelect extends Component {

    render() {
        const {categories, value, onChange, handleBeingTouched, touched, error} = this.props

        var options = categories.map(function(category) {
            return {label: category.display_name, value: category.id}
        })

        return (
            <div className='category-select-container'>
                <label className='category-select-label ncss-label'>Category</label>
                <Select
                    className={((touched && error) ? 'category-select select-container-error' : 'category-select select-container')}
                    onChange = {(v) => { handleBeingTouched(); onChange(v)} }
                    onBlur={() => { handleBeingTouched() }}
                    placeholder="Choose a Category"
                    value={value}
                    options={options} />
                {touched && error && <div className='select-error-msg'>{error}</div>}
            </div>
        )
    }
}
