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
import PropTypes from 'prop-types';
import * as mSDBActions from '../../actions/manageSafetyDepositBoxActions';
import './SecureFile.scss';
import log from '../../utils/logger';

import SecureFileForm from '../SecureFileForm/SecureFileForm';

export default class SecureFile extends Component {
    static propTypes = {
        label: PropTypes.string.isRequired,
        navigatedPath: PropTypes.string.isRequired,
        isActive: PropTypes.bool.isRequired,
        dispatch: PropTypes.func.isRequired,
        cerberusAuthToken: PropTypes.string.isRequired,
        isFetching: PropTypes.bool.isRequired,
        secureFileData: PropTypes.object.isRequired,
    };

    handleLabelClicked(key) {
        if (this.isFolder()) {
            this.props.dispatch(mSDBActions.updateNavigatedPath(`${this.props.navigatedPath}${key}`, this.props.cerberusAuthToken));
        } else {
            let fullKey = `${this.props.navigatedPath}${key}`;
            if (fullKey in this.props.secureFileData) {
                log.debug(`deleting from local cache Key: ${fullKey}`);
                this.props.dispatch(mSDBActions.removeSecureFileFromLocalStore(fullKey));
            } else {
                log.debug(`fetching Key: ${fullKey}`);
                this.props.dispatch(mSDBActions.getSecureFileMetadata(fullKey, this.props.cerberusAuthToken));
            }
        }
    }

    isFolder() {
        return this.props.label.endsWith('/');
    }

    render() {
        const { label, isActive, isFetching, navigatedPath, secureFileData } = this.props;

        return (
            <div className="secure-file-container">
                <div className="secure-file-collapsed-container" id={`secure-file-container-${label}`} onClick={() => {
                    this.handleLabelClicked(label);
                }}>
                    <div className={`secure-file-collapsed-icon ${this.isFolder() ? 'folder' : (isActive ? 'key-active' : 'key')}`}></div>
                    <div className={`secure-file-collapsed-label ${this.isFolder() ? 'folder' : `key${isActive ? '-active' : ''}`}-label`}>{label}</div>
                </div>
                {isActive && !isFetching &&
                    <SecureFileForm initialValues={{
                        path: label,
                        fileContents: {}
                    }}
                        secureFileData={secureFileData}
                        pathReadOnly={true}
                        isActive={isActive}
                        formKey={`${navigatedPath}${label}`} />
                }
            </div>
        );
    }

}

