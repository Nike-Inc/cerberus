import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'
import VaultSecretForm from '../VaultSecretForm/VaultSecretForm'
import * as mSDBActions from '../../actions/manageSafetyDepositBoxActions'
import './VaultSecret.scss'
import log from 'logger'

const fields = [
    'path',
    'kvMap[].key',
    'kvMap[].value'
]

export default class VaultSecret extends Component {
    static propTypes = {
        label: PropTypes.string.isRequired,
        navigatedPath: PropTypes.string.isRequired,
        vaultSecretsData: PropTypes.object.isRequired,
        isActive: PropTypes.bool.isRequired,
        isFetching: PropTypes.bool.isRequired,
        sdbData: PropTypes.object.isRequired,
        dispatch: PropTypes.func.isRequired,
        vaultToken: PropTypes.string.isRequired
    }

    handleLabelClicked(key) {
        if (this.isFolder()) {
            this.props.dispatch(mSDBActions.updateNavigatedPath(`${this.props.navigatedPath}${key}`, this.props.vaultToken))
        } else {
            let fullKey = `${this.props.navigatedPath}${key}`
            if (fullKey in this.props.vaultSecretsData) {
                log.debug(`deleting from local cache Key: ${fullKey}`)
                this.props.dispatch(mSDBActions.removeVaultSecretFromLocalStore(fullKey))
            } else {
                log.debug(`fetching Key: ${fullKey}`)
                this.props.dispatch(mSDBActions.getVaultSecret(fullKey, this.props.vaultToken))
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
        const {label, isActive, isFetching, sdbData, navigatedPath, vaultSecretsData, dispatch, vaultToken} = this.props

        return (
            <div className="vault-secret-container">
                <div className="vault-secret-collapsed-container" id={`vault-secret-container-${label}`} onClick={() => {
                    this.handleLabelClicked(label)
                }}>
                    <div className={`vault-secret-collapsed-icon ${this.isFolder() ? 'folder' : (isActive ? 'key-active' : 'key')}`}></div>
                    <div className={`vault-secret-collapsed-label ${this.isFolder() ? 'folder' : `key${isActive ? '-active' : ''}`}-label`}>{label}</div>
                </div>
                {isActive && !isFetching &&
                <div className="vault-secret-data-container">
                    <VaultSecretForm initialValues={ (vaultSecretsData[`${navigatedPath}${label}`] && vaultSecretsData[`${navigatedPath}${label}`].hasFormInit) ? undefined : {kvMap: this.assembleFormData(sdbData), path: label}}
                                     pathReadOnly={true}
                                     formKey={`${navigatedPath}${label}`} />
                    <div className="vault-secret-delete-btn" onClick={() => {
                                dispatch(mSDBActions.deleteVaultPathConfirm(`${navigatedPath}`,`${label}`, vaultToken))
                            }}>
                        <div className="vault-secret-delete-btn-icon"></div>
                        <div className="vault-secret-delete-btn-label">Delete this Vault Path</div>
                    </div>
                </div>
                }
            </div>
        )
    }
}