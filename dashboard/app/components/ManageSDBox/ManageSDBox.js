import React from 'react'
import { Component } from 'react'
import { connect } from 'react-redux'
import VaultBrowser from '../VaultBrowser/VaultBrowser'
import EditSDBoxForm from '../EditSDBoxForm/EditSDBoxForm'

import * as sdbMActions from '../../actions/manageSafetyDepositBoxActions'
import * as appActions from '../../actions/appActions'
import * as modalActions from '../../actions/modalActions'

import './ManageSDBox.scss'
import { getLogger } from 'logger'
var log = getLogger('manage-sdb')

// connect to the store for the pieces we care about
@connect((state) => {
    return {
        // user info
        vaultToken: state.auth.vaultToken,

        // Domain data
        hasDomainDataLoaded: state.app.cmsDomainData.hasLoaded,
        categories: state.app.cmsDomainData.categories,
        roles: state.app.cmsDomainData.roles,

        // SDB Data
        hasFetchedSDBData: state.manageSafetyDepositBox.hasFetchedSDBData,
        sdbData: state.manageSafetyDepositBox.data,
        navigatedPath: state.manageSafetyDepositBox.navigatedPath,
        vaultPathKeys: state.manageSafetyDepositBox.vaultPathKeys,
        vaultSecretsData: state.manageSafetyDepositBox.vaultSecretsData,
        hasFetchedKeys: state.manageSafetyDepositBox.hasFetchedKeys,
        showAddSecretForm: state.manageSafetyDepositBox.showAddSecretForm,

        displayPermissions: state.manageSafetyDepositBox.displayPermissions
    }
})
export default class ManageSDBox extends Component {

    componentWillReceiveProps(nextProps) {
        if (nextProps.routeParams.id != this.props.routeParams.id) {
            this.props.dispatch(sdbMActions.fetchSDBDataFromCMS(nextProps.routeParams.id, this.props.vaultToken))
        }
    }

    /**
     * Fetch the data about the SDB from CMS
     */
    componentDidMount() {
        if (! this.props.hasDomainDataLoaded) {
            this.props.dispatch(appActions.fetchCmsDomainData(this.props.vaultToken))
        }
        if (! this.props.hasFetchedSDBData) {
            this.props.dispatch(sdbMActions.fetchSDBDataFromCMS(this.props.routeParams.id, this.props.vaultToken))
        }
    }

    handleTogglePermissionsSectionVisibility() {
        this.props.dispatch(sdbMActions.togglePermVis())
    }

    render() {
        const {
            hasDomainDataLoaded,
            hasFetchedSDBData,
            categories,
            roles,
            sdbData,
            navigatedPath,
            vaultPathKeys,
            dispatch,
            vaultToken,
            displayPermissions,
            hasFetchedKeys,
            vaultSecretsData,
            showAddSecretForm
        } = this.props

        if (!hasDomainDataLoaded || !hasFetchedSDBData) {
            return(<div></div>)
        }

        let category = categories.find(category => category.id == sdbData.categoryId)
        
        return (
            <div className="manage-sdb-box-content-wrapper">
                <div className="manage-sdb-box-header">
                    <div className="manage-sdb-box-header-button-name-wrapper">
                        <div className="manage-sdb-box-header-name">
                            <div className="manage-sdb-box-header-label">Name:</div>
                            <div className="manage-sdb-box-header-value">{sdbData.name}</div>
                        </div>
                        <div className="manage-sdb-box-header-buttons">
                            <div className="manage-sdb-box-edit" onClick={() => {
                                dispatch(modalActions.pushModal(<EditSDBoxForm />))
                            }}></div>
                            <div className="manage-sdb-box-delete" onClick={() => {
                                dispatch(sdbMActions.deleteSDBConfirm(sdbData.id, vaultToken))
                            }}></div>
                        </div>
                    </div>
                    <div className="manage-sdb-box-header-category-owner-wrapper">
                        <div className="manage-sdb-box-header-category">
                            <div className="manage-sdb-box-header-label">Category:</div>
                            <div className="manage-sdb-box-header-value">{category.display_name}</div>
                        </div>
                        <div className="manage-sdb-box-header-owner">
                            <div className="manage-sdb-box-header-label">Owner:</div>
                            <div className="manage-sdb-box-header-value">{sdbData.owner}</div>
                        </div>
                    </div>
                    <div className="manage-sdb-box-header-description">
                        <div className="manage-sdb-box-header-label">Description:</div>
                        <div className="manage-sdb-box-header-value">{sdbData.description}</div>
                    </div>
                    <div className="manage-sdb-box-permissions-container">
                        <div className="manage-sdb-box-permissions-label" onClick={() => {
                            this.handleTogglePermissionsSectionVisibility()
                        }}>
                            <div className={displayPermissions ? "clickable perm-ico perm-ico-open" : "clickable perm-ico perm-ico-closed"}></div>
                            <div className="manage-sdb-box-header-label perm-label clickable">Permissions</div>
                        </div>

                        { displayPermissions &&
                            <div className="read-only-permissions">
                                { readOnlyUserGroupPermissions(sdbData.userGroupPermissions, roles) }
                                { readOnlyIamPrincipalPermissions(sdbData.iamPrincipalPermissions, roles) }
                            </div>
                        }

                    </div>
                </div>
                <div id="manage-sdb-box-vault-browser-container">
                    <VaultBrowser vaultToken={vaultToken}
                                  hasFetchedKeys={hasFetchedKeys}
                                  navigatedPath={navigatedPath}
                                  vaultPathKeys={vaultPathKeys}
                                  vaultSecretsData={vaultSecretsData}
                                  showAddSecretForm={showAddSecretForm}
                                  dispatch={dispatch} />
                </div>
            </div>
        )
    }
}

const readOnlyUserGroupPermissions = (userGroupPermissions, roles) => {
    if (userGroupPermissions == null || userGroupPermissions.length < 1) {
        return(<div>No User Group Permissions Defined</div>)
    } else {
        return(
            <div className=".">
                <div className="read-only-permissions-label">User Group Permissions</div>
                <table className="user-group-read-only-permission-group">
                    <tr>
                        <th className="iam-read-label">User Group</th>
                        <th className="iam-read-label">Role</th>
                    </tr>

                    {userGroupPermissions.map((perm, index) => {
                        return (
                            <tr key={perm.id} className={(index + 1) % 2 == 0 ? "iam-read-only-perm even-row" : "iam-read-only-perm odd-row"}>
                                <td>{perm.name}</td>
                                <td>{roleNameFromId(perm.roleId, roles)}</td>
                            </tr>
                        )
                    })}
                </table>
            </div>
        )
    }
}

const readOnlyIamPrincipalPermissions = (iamPrincipalPermissions, roles) => {
    if (iamPrincipalPermissions == null || iamPrincipalPermissions.length < 1) {
        return(<div>No IAM Principal Permissions Defined</div>)
    } else {
        return(
            <div className="perm-block">
                <div className="read-only-permissions-label">IAM Principal Permissions</div>
                <table className="iam-read-only-permission-group">
                    <tr>
                        <th className="iam-read-label">IAM Principal ARN</th>
                        <th className="iam-read-label">Role</th>
                    </tr>

                    {iamPrincipalPermissions.map((perm, index) => {
                        return (
                            <tr key={perm.id} className={(index + 1) % 2 == 0 ? "iam-read-only-perm even-row" : "iam-read-only-perm odd-row"}>
                                <td className="iam-read-only-perm-item iam-read-only-perm-principal-arn">{perm.iamPrincipalArn}</td>
                                <td className="iam-read-only-perm-item iam-read-only-perm-role">{roleNameFromId(perm.roleId, roles)}</td>
                            </tr>
                        )
                    })}
                </table>
            </div>



        )
    }
}

const roleNameFromId = (roleId, roles) => {
    let role = roles.filter(role => role.id == roleId)
    return role.length > 0 ? role[0].name : 'unknown'
}