import React from 'react'
import { Component } from 'react'
import { connect } from 'react-redux'
import { reduxForm } from 'redux-form'
import CopyToClipboard from 'react-copy-to-clipboard';
import AddButton from '../AddButton/AddButton'
import * as mSDBActions from '../../actions/manageSafetyDepositBoxActions'
import './VaultSecretForm.scss'
import { getLogger } from 'logger'
const  { DOM: { textarea } } = React

var log = getLogger('create-new-vault-secret')

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
        vaultToken: state.auth.vaultToken,
        navigatedPath: state.manageSafetyDepositBox.navigatedPath,
        vaultSecretsData: state.manageSafetyDepositBox.vaultSecretsData
    }
})
@reduxForm(
    {
        form: 'create-new-vault-secret',
        fields: fields,
        validate
    }
)

export default class VaultSecretForm extends Component {
    render() {
        const {
            fields: {
                path,
                kvMap
            },
            navigatedPath,
            dispatch,
            vaultToken,
            handleSubmit,
            pathReadOnly,
            vaultSecretsData,
            formKey
        } = this.props

        return(
            <div id="vault-add-new-secret-container">
                <form id='vault-add-new-secret-form' onSubmit={handleSubmit( data => {
                    let isNewVaultPath = formKey == 'add-new-secret'
                    dispatch(mSDBActions.commitSecret(navigatedPath, data, vaultToken, isNewVaultPath))
                })}>
                    <div id='new-vault-secret-path'>
                        <div id='new-vault-secret-path-label'>Path:</div>
                        <div id='new-vault-secret-path-full'>
                            {navigatedPath}
                            {pathReadOnly && <span className="new-vault-secret-path-user-value-read-only">{path.value}</span>}
                        </div>
                        {! pathReadOnly &&
                            <div id='new-vault-secret-path-user-value'>
                                <div className='vault-secret-path'>
                                    <div
                                        className={((path.touched && path.error) ? 'ncss-input-container error' : 'ncss-input-container')}>
                                        <input {...path}
                                            type='text'
                                            className='ncss-input pt2-sm pr4-sm pb2-sm pl4-sm'
                                            placeholder='Enter path of secret from current path'/>
                                        {path.touched && path.error && <div className='ncss-error-msg'>{path.error}</div>}
                                    </div>
                                </div>
                            </div>
                        }
                    </div>
                    <div id="new-vault-secret-kv-map">
                        {kvMap.map((entry, index) =>
                            <div className="new-vault-secret-kv-entry" key={index}>
                                <div className='vault-secret-key'>
                                    <div className={((entry.key.touched && entry.key.error) ? 'ncss-input-container error' : 'ncss-input-container')}>
                                        <input {...entry.key}
                                            type='text'
                                            className='ncss-input pt2-sm pr4-sm pb2-sm pl4-sm'
                                            placeholder='Key' />
                                        {entry.key.touched && entry.key.error && <div className='ncss-error-msg'>{entry.key.error}</div>}
                                    </div>
                                </div>
                                <div className='vault-secret-value'>
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
                    <div className="vault-secret-button-container">
                        <AddButton handleClick={() => {kvMap.addField({'revealed': true})}} message="Add Key Value Pair" />
                        <div id="submit-btn-container">
                            <div className="btn-wrapper">
                                <div id='cancel-btn'
                                        className='ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                                        onClick={() => {
                                            dispatch(mSDBActions.hideAddNewVaultSecret())
                                        }}>Cancel</div>
                            </div>
                            <div className="btn-wrapper">
                                <button id='submit-btn'
                                        className='ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                                        disabled={vaultSecretsData[navigatedPath + path.value] ? vaultSecretsData[navigatedPath + path.value].isUpdating : false}>Save
                                </button>
                            </div>
                        </div>      
                    </div>
                </form>
            </div>
        )
    }
}