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
import PropTypes from 'prop-types'
import SecureDataForm from '../SecureDataForm/SecureDataForm'
import * as mSDBActions from '../../actions/manageSafetyDepositBoxActions'
import './SecureData.scss'
import log from 'logger'

const fields = [
    'path',
    'kvMap[].key',
    'kvMap[].value'
]

export default class SecureData extends Component {
    static propTypes = {
        label: PropTypes.string.isRequired,
        navigatedPath: PropTypes.string.isRequired,
        secureObjectData: PropTypes.object.isRequired,
        isActive: PropTypes.bool.isRequired,
        isFetching: PropTypes.bool.isRequired,
        sdbData: PropTypes.object.isRequired,
        dispatch: PropTypes.func.isRequired,
        cerberusAuthToken: PropTypes.string.isRequired
    }

    handleLabelClicked(key) {
        if (this.isFolder()) {
            this.props.dispatch(mSDBActions.updateNavigatedPath(`${this.props.navigatedPath}${key}`, this.props.cerberusAuthToken))
        } else {
            let fullKey = `${this.props.navigatedPath}${key}`
            if (fullKey in this.props.secureObjectData) {
                log.debug(`deleting from local cache Key: ${fullKey}`)
                this.props.dispatch(mSDBActions.removeSecureDataFromLocalStore(fullKey))
            } else {
                log.debug(`fetching Key: ${fullKey}`)
                this.props.dispatch(mSDBActions.getSecureData(fullKey, this.props.cerberusAuthToken))
            }
        }
    }

    //noinspection JSMethodCanBeStatic
    assembleFormData(sdbData) {
        var data = []
        for (let key in sdbData) {
            if (sdbData.hasOwnProperty(key)) {
                data.push({key: key, value: sdbData[key]})
            }
        }
        return data
    }

    isFolder() {
        return this.props.label.endsWith('/')
    }

    render() {
        const {label, isActive, isFetching, sdbData, navigatedPath, secureObjectData, dispatch, cerberusAuthToken} = this.props

        return (
            <div className="secure-data-container">
                <div className="secure-data-collapsed-container" id={`secure-data-container-${label}`} onClick={() => {
                    this.handleLabelClicked(label)
                }}>
                    <div className={`secure-data-collapsed-icon ${this.isFolder() ? 'folder' : (isActive ? 'key-active' : 'key')}`}></div>
                    <div className={`secure-data-collapsed-label ${this.isFolder() ? 'folder' : `key${isActive ? '-active' : ''}`}-label`}>{label}</div>
                </div>
                {isActive && !isFetching &&
                <div className="secure-data-data-container">
                    <SecureDataForm initialValues={ (secureObjectData[`${navigatedPath}${label}`] && secureObjectData[`${navigatedPath}${label}`].hasFormInit) ? undefined : {kvMap: this.assembleFormData(sdbData), path: label}}
                                    pathReadOnly={true}
                                    formKey={`${navigatedPath}${label}`} />
                    { deleteButton(dispatch, navigatedPath, label, cerberusAuthToken) }
                </div>
                }
            </div>
        )
    }
}

const deleteButton = (dispatch, navigatedPath, label, cerberusAuthToken) => {
    return (
        <div className="secure-data-delete-btn btn ncss-btn-accent ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase" onClick={() => {
            dispatch(mSDBActions.deleteSecureDataPathConfirm(`${navigatedPath}`,`${label}`, cerberusAuthToken))
        }}>
            <div className="ncss-glyph-plus-lg secure-data-delete-btn-icon"></div>
            <div className="secure-data-delete-btn-label">Delete this Secure Data Path</div>
        </div>
    )
}