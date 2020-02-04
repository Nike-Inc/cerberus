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
import * as mSDBActions from '../../actions/manageSafetyDepositBoxActions'
import * as modalActions from '../../actions/modalActions'
import * as messengerActions from '../../actions/messengerActions'
import './SecureFileForm.scss'
import { getLogger } from 'logger'
import FileInput from 'react-simple-file-input';
import ConfirmationBox from '../ConfirmationBox/ConfirmationBox';

var log = getLogger('add-new-secure-file');

const MAX_FILE_SIZE = 250000;

const fields = [
    'path',
    'uploadedFile'
]

// define our client side form validation rules
const validate = values => {
    const errors = {}
    errors.kvMap = {}

    if (!values.path) {
        errors.path = 'Required'
    }

    return errors
}
@connect((state) => {
    return {
        cerberusAuthToken: state.auth.cerberusAuthToken,
        navigatedPath: state.manageSafetyDepositBox.navigatedPath,
        isFileSelected: state.manageSafetyDepositBox.isFileSelected,
        secureFileData: state.manageSafetyDepositBox.secureFileData,
        secureDataKeys: state.manageSafetyDepositBox.keysForSecureDataPath
    }
})
@reduxForm(
    {
        form: 'add-new-secure-file',
        fields: fields,
        validate
    }
)

export default class SecureFileForm extends Component {
    render() {
        const {
            fields: {
                path,
                uploadedFile
            },
            navigatedPath,
            dispatch,
            cerberusAuthToken,
            handleSubmit,
            pathReadOnly,
            isFileSelected,
            secureFileData,
            secureDataKeys,
            isActive,
            formKey
        } = this.props

        return(
            <div id="add-new-secure-file-container">
                <form id="add-new-secure-file-form" onSubmit={handleSubmit( data => {
                    let isNewSecureFilePath = formKey === 'add-new-secure-file';
                    if (data.path in secureDataKeys) {
                        this.confirmFileOverwrite(dispatch, cerberusAuthToken, navigatedPath, data.path, data.uploadedFile, isNewSecureFilePath)
                    } else {
                        dispatch(mSDBActions.uploadFile(cerberusAuthToken, navigatedPath, data.path, data.uploadedFile, isNewSecureFilePath))
                    }
                })}>
                    { pathReadOnly &&
                        <div id="new-secure-file-path">
                            <div id="new-secure-file-path-label">Path:</div>
                            <div id="new-secure-file-path-full">
                                {navigatedPath}
                                <span className="new-secure-file-path-user-value-read-only">{path.value}</span>
                            </div>
                        </div>
                    }
                    { !pathReadOnly && isFileSelected &&
                        <div id="new-secure-file-path">
                            <div id="new-secure-file-path-label">Path:</div>
                            <div id="new-secure-file-path-full">
                                {navigatedPath}
                                <span className="new-secure-file-path-user-value-read-only">{path.value}</span>
                            </div>
                        </div>
                    }
                    <div className="secure-file-operation-buttons-container">
                        { isFileSelected && !isActive &&
                            <div className="secure-file-name-label">
                                <div className="secure-file-name">Filename: {uploadedFile.value.name}</div>
                                <div className="secure-file-size">{this.convertBytesToKilobytes(uploadedFile.value.sizeInBytes, 2, false)} KB</div>
                            </div>
                        }
                        { !isFileSelected && !isActive &&
                            <div className="secure-file-name-label">
                                <div className="no-file-selected">No File Selected</div>
                                <div className="max-file-size-label">Maximum Size: {this.convertBytesToKilobytes(MAX_FILE_SIZE, 0, false)} KB</div>
                            </div>
                        }
                        { isActive &&
                            <div className="secure-file-name-label">
                                <div className="secure-file-name">Filename: {path.value}</div>
                                <div className="secure-file-size">
                                    {this.convertBytesToKilobytes(secureFileData[`${navigatedPath}${path.value}`].data.sizeInBytes, 2, false)} KB
                                </div>
                            </div>
                        }
                        {!pathReadOnly &&
                            /* this div/label structure is required by the react-simple-file-input plugin */
                            <div><label>
                                <FileInput readAs='buffer'
                                           style={{display: 'none'}}
                                           onLoad={(event, file) => {
                                               let fileContents = event.target.result;
                                               let fileData = {
                                                   name: file.name,
                                                   sizeInBytes: file.size,
                                                   type: file.type,
                                                   contents: fileContents
                                               };
                                               uploadedFile.onChange(fileData);
                                               path.onChange(file.name);
                                               dispatch(mSDBActions.secureFileSelected());
                                           }}
                                           cancelIf={ (file) => this.fileIsTooLarge(dispatch, file) }/>
                                <div className="ncss-btn-dark-grey btn ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt3-lg pb3-lg u-uppercase un-selectable"
                                     id="new-file-form-browse-btn">
                                    Browse
                                </div>
                            </label></div>
                        }
                        {isActive &&
                            <div className="ncss-btn-dark-grey btn ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt3-lg pb3-lg u-uppercase un-selectable"
                                 onClick={() => {mSDBActions.downloadFile(cerberusAuthToken, `${navigatedPath}${path.value}`, `${path.value}`)}}>
                                Download
                            </div>
                        }
                        { isActive &&
                            deleteButton(dispatch, navigatedPath, path.value, cerberusAuthToken)
                        }
                    </div>
                    {!pathReadOnly &&
                        <div className="secure-file-button-container">
                            <div id="submit-btn-container">
                                <div className="btn-wrapper">
                                    <button className="ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt3-lg pb3-lg u-uppercase un-selectable"
                                            disabled={secureFileData[navigatedPath + path.value] ? secureFileData[navigatedPath + path.value].isUpdating : false}>
                                        Submit
                                    </button>
                                </div>
                                <div className="btn-wrapper">
                                    <div id='cancel-btn'
                                         className="ncss-btn-accent ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase un-selectable"
                                         onClick={() => {dispatch(mSDBActions.hideAddNewSecureFile())}}>
                                        Cancel
                                    </div>
                                </div>
                            </div>
                        </div>
                    }
                </form>
            </div>
        )
    }

    convertBytesToKilobytes(sizeInBytes, numDecimalPlaces, divideByPowerOfTwo) {
        if (divideByPowerOfTwo) {
            return (sizeInBytes / 1024).toFixed(numDecimalPlaces);
        } else {
            return (sizeInBytes / 1000).toFixed(numDecimalPlaces);
        }
    }

    fileIsTooLarge(dispatch, file) {
        if (file.size > MAX_FILE_SIZE) {
            dispatch(messengerActions.addNewMessageWithTimeout(
                <div className="file-validation-error-msg-container">
                    <div className="file-validation-error-msg-header">
                        <h4>{`File: '${file.name}' is too large`}</h4>
                    </div>
                </div>, 15000));
            return true;
        }
        return false;
    }

    confirmFileOverwrite(dispatch, cerberusAuthToken, navigatedPath, secureFileName, fileContents, isNewSecureFilePath) {
        let yes = () => {
            dispatch(mSDBActions.uploadFile(cerberusAuthToken, navigatedPath, secureFileName, fileContents, isNewSecureFilePath));
            dispatch(modalActions.popModal());
        };

        let no = () => {
            dispatch(modalActions.popModal());
        };

        let comp = <ConfirmationBox handleYes={yes}
                                    handleNo={no}
                                    message={`Are you sure you want to overwrite file: "${secureFileName}"?`}/>
        dispatch(modalActions.pushModal(comp));
    }
}

const deleteButton = (dispatch, navigatedPath, label, cerberusAuthToken) => {
    return (
        <div className="secure-file-delete-btn btn ncss-btn-accent ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt3-lg pb3-lg u-uppercase un-selectable" onClick={() => {
            dispatch(mSDBActions.deleteSecureFilePathConfirm(`${navigatedPath}`,`${label}`, cerberusAuthToken));
        }}>
            <div className="secure-file-delete-btn-label">Delete</div>
        </div>
    )
}