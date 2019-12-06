import React from 'react'
import { Component } from 'react'
import EditSDBoxForm from '../EditSDBoxForm/EditSDBoxForm'
import DeleteSafeDepositBoxForm from '../DeleteSafeDepositBoxForm/DeleteSafeDepositBoxForm'
import PropTypes from 'prop-types'
import * as modalActions from '../../actions/modalActions'
import './SafeDepositBoxSettings.scss'

export default class SafeDepositBoxSettings extends Component {

    static propTypes = {
        categories: PropTypes.array.isRequired,
        roles: PropTypes.array.isRequired,
        sdbData: PropTypes.object.isRequired,
        dispatch: PropTypes.func.isRequired,
    }

    render() {

        const {
            categories,
            roles,
            sdbData,
            dispatch,
        } = this.props

        let category = categories.find(category => category.id === sdbData.categoryId)

        return(
            <div className="safe-deposit-box-settings">
                <div className="sdb-settings-name">
                    <div className="sdb-settings-label">Name:</div>
                    <div className="sdb-settings-value">{sdbData.name}</div>
                </div>
                <div className="sdb-settings-category-owner-wrapper">
                    <div className="sdb-settings-category">
                        <div className="sdb-settings-label">Category:</div>
                        <div className="sdb-settings-value">{category.display_name}</div>
                    </div>
                    <div className="sdb-settings-owner">
                        <div className="sdb-settings-label">Owner:</div>
                        <div className="sdb-settings-value">{sdbData.owner}</div>
                    </div>
                </div>
                <div className="sdb-settings-description">
                    <div className="sdb-settings-label">Description:</div>
                    <div className="sdb-settings-value">{sdbData.description}</div>
                </div>
                <div className="manage-sdb-box-permissions-container">
                    <div className="read-only-permissions">
                        { readOnlyUserGroupPermissions(sdbData.userGroupPermissions, roles) }
                        { readOnlyIamPrincipalPermissions(sdbData.iamPrincipalPermissions, roles) }
                    </div>
                </div>
                <div className="sdb-settings-buttons">
                    <div
                        className='btn ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                        onClick={() => {
                            dispatch(modalActions.pushModal(<EditSDBoxForm />))
                        }}
                    >Edit Safe Deposit Box Settings</div>
                    <div
                        className='btn ncss-btn-accent ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                        onClick={() => {
                            dispatch(modalActions.pushModal(<DeleteSafeDepositBoxForm />))
                        }}
                    >Delete this Safe Deposit Box</div>
                </div>
            </div>
        )
    }
}


const readOnlyUserGroupPermissions = (userGroupPermissions, roles) => {
    if (userGroupPermissions === null || userGroupPermissions.length < 1) {
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
    if (iamPrincipalPermissions === null || iamPrincipalPermissions.length < 1) {
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
    let role = roles.filter(role => role.id === roleId)
    return role.length > 0 ? role[0].name : 'unknown'
}