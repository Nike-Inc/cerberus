import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'
import RoleSelect from '../RoleSelect/RoleSelect'
import Buttons from '../Buttons/Buttons'
import AddButton from '../AddButton/AddButton'
import { touch } from 'redux-form'
import './IamPrincipalPermissionsFieldSet.scss'

/**
 * Component for displaying User Group Permissions form field set
 * @prop iamPrincipalPermissions is the Redux form field for the array of IAM role permission objects (Group -> Role)
 * @prop dispatch from the store to dispatch touch events for the drop downs
 * @prop formName The redux form name for touch events in the drop downs
 * @props roles The list of roles that a user can select for the permission
 */
export default class IamPrincipalPermissionsFieldSet extends Component {
    static propTypes = {
        iamPrincipalPermissions: PropTypes.array.isRequired,
        dispatch: PropTypes.func.isRequired,
        formName: PropTypes.string.isRequired,
        roles: PropTypes.array.isRequired
    }

    render() {
        const {iamPrincipalPermissions, dispatch, formName, roles} = this.props

        return (
            <div className='iam-principal-permissions'>
                <div className='iam-principal-permissions-label ncss-label'>IAM Principal Permissions</div>
                <div className="iam-principal-permissions-perms-container">
                    <div className="iam-principal-permissions-perms-container">
                    {iamPrincipalPermissions.map((permission, index) =>
                        <div key={index}>
                            <div className='iam-principal-permissions-permission'>

                                <div className='role-perm-principal-arn'>
                                    <div className={((permission.iamPrincipalArn.touched && permission.iamPrincipalArn.error) ? 'ncss-input-container error' : 'ncss-input-container')}>
                                        <input {...permission.iamPrincipalArn}
                                            type='text'
                                            className='ncss-input pt2-sm pr4-sm pb2-sm pl4-sm'
                                            placeholder='IAM Principal ARN' />
                                        {permission.iamPrincipalArn.touched && permission.iamPrincipalArn.error && <div className='ncss-error-msg'>{permission.iamPrincipalArn.error}</div>}
                                    </div>
                                </div>

                                <RoleSelect {...permission.roleId}
                                    roles={roles.filter((role => role.name.toLowerCase() != 'owner'))}
                                    handleBeingTouched={() => {
                                                    dispatch(touch(formName, permission.roleId.name))
                                            }} />

                                <Buttons handleRemoveClicked={() => {
                                                     iamPrincipalPermissions.removeField(index)
                                                 }} />
                            </div>
                        </div>
                    )}
                    </div>
                </div>
                <AddButton handleClick={iamPrincipalPermissions.addField} message="Add new permission" />
            </div>
        )
    }
}