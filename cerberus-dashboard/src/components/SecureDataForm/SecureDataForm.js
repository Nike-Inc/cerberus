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
import { connect } from 'react-redux'
import { reduxForm } from 'redux-form'
import CopyToClipboard from 'react-copy-to-clipboard';
import AddButton from '../AddButton/AddButton'
import * as mSDBActions from '../../actions/manageSafetyDepositBoxActions'
import './SecureDataForm.scss'
import { getLogger } from 'logger'
const  { DOM: { textarea } } = React

var log = getLogger('add-new-secure-data')

const fields = [
    'path',
    'kvMap[].key',
    'kvMap[].value',
    'kvMap[].revealed'
]

// define our client side form validation rules
const validate = values => {
    const errors = {}
    errors.kvMap = {}

    if (!values.path) {
        errors.path = 'Required'
    }

    values.kvMap.map((entry, index) => {
        errors.kvMap[`${index}`] = {}

        if (! entry.key) {
            errors.kvMap[`${index}`].key = 'Required'
        }

        if (! entry.value) {
            errors.kvMap[`${index}`].value = 'Required'
        }
    })

    return errors
}
@connect((state) => {
    return {
        cerberusAuthToken: state.auth.cerberusAuthToken,
        navigatedPath: state.manageSafetyDepositBox.navigatedPath,
        secureData: state.manageSafetyDepositBox.secureData
    }
})
@reduxForm(
    {
        form: 'add-new-secure-data',
        fields: fields,
        validate
    }
)

export default class SecureDataForm extends Component {
    render() {
        const {
            fields: {
                path,
                kvMap
            },
            navigatedPath,
            dispatch,
            cerberusAuthToken,
            handleSubmit,
            pathReadOnly,
            secureData,
            formKey
        } = this.props

        return(
            <div id="add-new-secure-data-container">
                <form id='add-new-secure-data-form' onSubmit={handleSubmit( data => {
                    let isNewSecureDataPath = formKey === 'add-new-secure-data';
                    dispatch(mSDBActions.commitSecret(navigatedPath, data, cerberusAuthToken, isNewSecureDataPath))
                })}>
                    <div id='new-secure-data-path'>
                        <div id='new-secure-data-path-label'>Path:</div>
                        <div id='new-secure-data-path-full'>
                            {navigatedPath}
                            {pathReadOnly && <span className="new-secure-data-path-user-value-read-only">{path.value}</span>}
                        </div>
                        {! pathReadOnly &&
                            <div id='new-secure-data-path-user-value'>
                                <div className='secure-data-path'>
                                    <div
                                        className={((path.touched && path.error) ? 'ncss-input-container error' : 'ncss-input-container')}>
                                        <input {...path}
                                            type='text'
                                            className='ncss-input pt2-sm pr4-sm pb2-sm pl4-sm'
                                            placeholder='Path'/>
                                        {path.touched && path.error && <div className='ncss-error-msg'>{path.error}</div>}
                                    </div>
                                </div>
                            </div>
                        }
                    </div>
                    <div id="new-secure-data-kv-map">
                        {kvMap.map((entry, index) =>
                            <div className="new-secure-data-kv-entry" key={index}>
                                <div className='secure-data-key'>
                                    <div className={((entry.key.touched && entry.key.error) ? 'ncss-input-container error' : 'ncss-input-container')}>
                                        <input {...entry.key}
                                            type='text'
                                            className='ncss-input pt2-sm pr4-sm pb2-sm pl4-sm'
                                            placeholder='Key' />
                                        {entry.key.touched && entry.key.error && <div className='ncss-error-msg'>{entry.key.error}</div>}
                                    </div>
                                </div>
                                <div className='secure-data-value'>
                                    <div className={((entry.value.touched && entry.value.error) ? 'ncss-input-container error' : 'ncss-input-container')}>
                                        <textarea {...entry.value}
                                                  type="text"
                                                  className='ncss-input pt2-sm pr4-sm pb2-sm pl4-sm'
                                                  placeholder='Value'
                                                  style={{display: entry.revealed.value ? 'block' : 'none' }}/>

                                        <div className="ncss-input pt2-sm pr4-sm pb2-sm pl4-sm secret-value-placeHolder"
                                             style={{display: !entry.revealed.value ? 'block' : 'none' }}
                                        >
                                            Hidden, click the reveal button
                                        </div>

                                        {entry.value.touched && entry.value.error && <div className='ncss-error-msg'>{entry.value.error}</div>}
                                    </div>
                                </div>

                                <div className='row-buttons'>
                                    <div className="btn-wrapper btn-wrapper-left">
                                        <input type="checkbox" className={! entry.revealed.value ? 'row-btn row-btn-reveal' : 'row-btn row-btn-revealed'} {...entry.revealed}/>
                                    </div>
                                    <CopyToClipboard text={entry.value.value}>
                                        <div className={kvMap.length <= 1 ? 'btn-wrapper btn-wrapper-right' : 'btn-wrapper'}>
                                            <div className='row-btn row-btn-copy'></div>
                                        </div>
                                    </CopyToClipboard>
                                    {kvMap.length > 1 &&
                                        <div className="btn-wrapper btn-wrapper-right">
                                            <div className='row-btn row-btn-remove' onClick={() => {kvMap.removeField(index)}}></div>
                                        </div>
                                    }
                                </div>
                            </div>
                        )}
                    </div>
                    <div className="secure-data-button-container">
                        <AddButton handleClick={() => {kvMap.addField({'revealed': true})}} message="Add Key Value Pair" />
                        <div id="submit-btn-container">
                            <div className="btn-wrapper">
                                <div id='cancel-btn'
                                        className='ncss-btn-accent ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase un-selectable'
                                        onClick={() => {
                                            dispatch(mSDBActions.hideAddNewSecureData())
                                        }}>Cancel</div>
                            </div>
                            <div className="btn-wrapper">
                                <button id='submit-btn'
                                        className='ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase un-selectable'
                                        disabled={secureData[navigatedPath + path.value] ? secureData[navigatedPath + path.value].isUpdating : false}>Save
                                </button>
                            </div>
                        </div>      
                    </div>
                </form>
            </div>
        )
    }
}