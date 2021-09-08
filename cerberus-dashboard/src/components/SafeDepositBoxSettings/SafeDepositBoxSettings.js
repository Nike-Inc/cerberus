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

import PropTypes from 'prop-types';
import React, { Component } from 'react';
import ReactTooltip from 'react-tooltip';
import * as modalActions from '../../actions/modalActions';
import { validateADGroup } from '../CreateSDBoxForm/validator';
import DeleteSafeDepositBoxForm from '../DeleteSafeDepositBoxForm/DeleteSafeDepositBoxForm';
import EditSDBoxForm from '../EditSDBoxForm/EditSDBoxForm';
import './SafeDepositBoxSettings.scss';


export default class SafeDepositBoxSettings extends Component {

    static propTypes = {
        categories: PropTypes.array.isRequired,
        roles: PropTypes.array.isRequired,
        sdbData: PropTypes.object.isRequired,
        dispatch: PropTypes.func.isRequired,
    };

    render() {

        const {
            categories,
            roles,
            sdbData,
            dispatch,
        } = this.props;

        let category = categories.find(category => category.id === sdbData.categoryId);

        return (
            <div className="safe-deposit-box-settings">
                <ReactTooltip />
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
                        {!validateADGroup(sdbData.owner) && <div className="warning-icon" data-tip={process.env.REACT_APP_SDB_WARNING_MESSAGE ? process.env.REACT_APP_SDB_WARNING_MESSAGE : ""}></div>}
                    </div>
                </div>
                <div className="sdb-settings-description">
                    <div className="sdb-settings-label">Description:</div>
                    <div className="sdb-settings-value">{sdbData.description}</div>
                </div>
                <div className="manage-sdb-box-permissions-container">
                    <div className="read-only-permissions">
                        {readOnlyUserGroupPermissions(sdbData.userGroupPermissions, roles)}
                        {readOnlyIamPrincipalPermissions(sdbData.iamPrincipalPermissions, roles)}
                    </div>
                </div>
                <div className="sdb-settings-buttons">
                    <div
                        className='btn ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                        onClick={() => {
                            dispatch(modalActions.pushModal(<EditSDBoxForm />));
                        }}
                    >Edit Safe Deposit Box Settings</div>
                    <div
                        className='btn ncss-btn-accent ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                        onClick={() => {
                            dispatch(modalActions.pushModal(<DeleteSafeDepositBoxForm />));
                        }}
                    >Delete this Safe Deposit Box</div>
                </div>
            </div>
        );
    }
}


const readOnlyUserGroupPermissions = (userGroupPermissions, roles) => {
    if (userGroupPermissions === null || userGroupPermissions.length < 1) {
        return (<div>No User Group Permissions Defined</div>);
    } else {
        return (
            <div className=".">
                <div className="read-only-permissions-label">User Group Permissions</div>
                <table className="user-group-read-only-permission-group">
                    <tr>
                        <th className="iam-read-label">User Group</th>
                        <th className="iam-read-label">Role</th>
                    </tr>

                    {userGroupPermissions.map((perm, index) => {
                        return (
                            <tr key={perm.id} className={(index + 1) % 2 === 0 ? "iam-read-only-perm even-row" : "iam-read-only-perm odd-row"}>
                                <td>
                                    {!validateADGroup(perm.name) && <div className="warning-icon" data-tip={process.env.REACT_APP_SDB_WARNING_MESSAGE ? process.env.REACT_APP_SDB_WARNING_MESSAGE : ""}></div>}
                                    {perm.name}
                                </td>
                                <td>{roleNameFromId(perm.roleId, roles)}</td>
                            </tr>
                        );
                    })}
                </table>
            </div>
        );
    }
};

const readOnlyIamPrincipalPermissions = (iamPrincipalPermissions, roles) => {
    if (iamPrincipalPermissions === null || iamPrincipalPermissions.length < 1) {
        return (<div>No IAM Principal Permissions Defined</div>);
    } else {
        return (
            <div className="perm-block">
                <div className="read-only-permissions-label">IAM Principal Permissions</div>
                <table className="iam-read-only-permission-group">
                    <tr>
                        <th className="iam-read-label">IAM Principal ARN</th>
                        <th className="iam-read-label">Role</th>
                    </tr>

                    {iamPrincipalPermissions.map((perm, index) => {
                        return (
                            <tr key={perm.id} className={(index + 1) % 2 === 0 ? "iam-read-only-perm even-row" : "iam-read-only-perm odd-row"}>
                                <td className="iam-read-only-perm-item iam-read-only-perm-principal-arn">{perm.iamPrincipalArn}</td>
                                <td className="iam-read-only-perm-item iam-read-only-perm-role">{roleNameFromId(perm.roleId, roles)}</td>
                            </tr>
                        );
                    })}
                </table>
            </div>



        );
    }
};

const roleNameFromId = (roleId, roles) => {
    let role = roles.filter(role => role.id === roleId);
    return role.length > 0 ? role[0].name : 'unknown';
};