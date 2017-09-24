import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'
import './VaultBrowser.scss'
import AddButton from '../AddButton/AddButton'
import VaultSecretForm from '../VaultSecretForm/VaultSecretForm'
import VaultSecret from '../VaultSecret/VaultSecret'
import * as mSDBActions from '../../actions/manageSafetyDepositBoxActions'
import { getLogger } from 'logger'
var log = getLogger('vault-browser-component')

export default class VaultBrowser extends Component {
    static propTypes = {
        hasFetchedKeys: PropTypes.bool.isRequired,
        vaultToken: PropTypes.string.isRequired,
        navigatedPath: PropTypes.string.isRequired,
        vaultPathKeys: PropTypes.array,
        vaultSecretsData: PropTypes.object.isRequired,
        dispatch: PropTypes.func.isRequired,
        showAddSecretForm: PropTypes.bool.isRequired
    }

    componentDidMount() {
        if (!this.props.hasFetchedKeys) {
            this.props.dispatch(mSDBActions.fetchVaultPathKeys(this.props.navigatedPath, this.props.vaultToken))
        }
    }

    render() {
        const {vaultToken, navigatedPath, vaultPathKeys, vaultSecretsData, dispatch, hasFetchedKeys, showAddSecretForm} = this.props

        if (!hasFetchedKeys) {
            return(<div></div>)
        }

        log.debug(JSON.stringify(vaultPathKeys))

        return (
            <div id='vault-browser'>
                <VaultBreadcrumb path={navigatedPath} dispatch={dispatch} vaultToken={vaultToken} />
                <div id='vault-keys'>
                    {vaultPathKeys && vaultPathKeys.map((key) =>
                        <div key={key} className='vault-key' id={`vault-key-${key}`}>
                            <VaultSecret label={key}
                                         navigatedPath={navigatedPath}
                                         vaultSecretsData={vaultSecretsData}
                                         vaultToken={vaultToken}
                                         dispatch={dispatch}
                                         isActive={`${navigatedPath}${key}` in vaultSecretsData ? vaultSecretsData[`${navigatedPath}${key}`].isActive : false}
                                         isFetching={`${navigatedPath}${key}` in vaultSecretsData ? vaultSecretsData[`${navigatedPath}${key}`].isFetching : false}
                                         sdbData={`${navigatedPath}${key}` in vaultSecretsData ? vaultSecretsData[`${navigatedPath}${key}`].data : {}} />
                        </div>
                    )}
                </div>
                <div id="add-container">
                    {! showAddSecretForm &&
                        <AddButton handleClick={() => {
                            dispatch(mSDBActions.showAddNewVaultSecret())
                        }} message="Add New Vault Path" />
                    }
                    {showAddSecretForm &&
                        <div id="add-form-container">
                            <div className="vault-secret-form-label">Add a new Vault Path</div>
                            <VaultSecretForm initialValues={{
                                                           kvMap: [{key: null, value: null, revealed: true}],
                                                           path: null
                                                         }}
                                             pathReadOnly={false}
                                             formKey={`add-new-secret`} />
                        </div>
                    }
                </div>
            </div>
        )
    }
}

class VaultBreadcrumb extends Component {
    static propTypes = {
        path: PropTypes.string.isRequired,
        dispatch: PropTypes.func.isRequired,
        vaultToken: PropTypes.string.isRequired
    }

    handlePathPieceClick(indexClicked) {
        let pathClicked = this.props.path.split('/').filter(Boolean).slice(0, indexClicked + 1).join('/')+'/'
        this.props.dispatch(mSDBActions.updateNavigatedPath(pathClicked, this.props.vaultToken))
    }

    render() {
        let pathPieces = this.props.path.split('/').filter(Boolean)

        return(
            <div id='vault-path'>
                <div id='vault-path-label'>Current Location:</div>
                {pathPieces.map((breadCrumbPiece, i) =>
                    <div key={i} className={i == 0 ? 'breadcrumb-piece' : 'breadcrumb-piece clickable'} onClick={() => {
                        if (i != 0) {
                            this.handlePathPieceClick(i)
                        }
                    }}>
                        {breadCrumbPiece}
                        <div className='breadcrumb-separator'>/</div>
                    </div>
                )}
            </div>
        )
    }
}