import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'
import * as cms from '../../constants/cms'

/**
 * Component for an Add Button (a button with a plus and a message)
 */
export default class SDBDescriptionField extends Component {
    static propTypes = {
        description: PropTypes.object.isRequired
    }

    render() {
        const {description} = this.props
        return (
            <div id='description' className='ncss-form-group'>
                <div className={((description.touched && description.error) ? 'ncss-textarea-container error' : 'ncss-textarea-container')}>
                    <label className='ncss-label'>Description</label>
                            <textarea className='ncss-textarea pt2-sm pr4-sm pb2-sm pl4-sm'
                                      placeholder='Enter a description for your Bucket'
                                      maxLength={`${cms.SDB_DESC_MAX_LENGTH}`}
                                {...description} />
                    {description.touched && description.error && <div className='ncss-error-msg'>{description.error}</div>}
                </div>
            </div>
        )
    }
}
