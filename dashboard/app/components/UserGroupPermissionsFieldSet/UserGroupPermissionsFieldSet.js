import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'
import GroupsSelect from '../GroupSelect/GroupsSelect'
import RoleSelect from '../RoleSelect/RoleSelect'
import Buttons from '../Buttons/Buttons'
import AddButton from '../AddButton/AddButton'
import { touch } from 'redux-form'
import './UserGroupPermissionsFieldSet.scss'

/**
 * Component for displaying User Group Permissions form field set
 * @prop userGroupPermissions is the Redux form field for the array of user group permission objects (Group -> Role)
 * @prop dispatch from the store to dispatch touch events for the drop downs
 * @prop formName The redux form name for touch events in the drop downs
 * @props userGroups The list of groups that a user can select for the permission
 * @props roles The list of roles that a user can select for the permission
 */
export default class UserGroupPermissionsFieldSet extends Component {
    static propTypes = {
        userGroupPermissions: PropTypes.array.isRequired,
        dispatch: PropTypes.func.isRequired,
        formName: PropTypes.string.isRequired,
        userGroups: PropTypes.array.isRequired,
        roles: PropTypes.array.isRequired
    }

    render() {
        const {userGroupPermissions, dispatch, formName, userGroups, roles} = this.props

        return (
            <div className='user-group-permissions'>
                <div className='user-group-permissions-label ncss-label'>User Group Permissions</div>
                <div className="user-group-permissions-perms-container">
                    {userGroupPermissions.map((permission, index) =>
                        <div key={index}>
                            <div className='user-group-permissions-permission'>

                                <GroupsSelect {...permission.name}
                                    userGroups={userGroups}
                                    allowCustomValues={true}
                                    handleBeingTouched={() => {
                                                          dispatch(touch(formName, permission.name.name))
                                                      }} />

                                <RoleSelect {...permission.roleId}
                                    roles={roles.filter((role => role.name.toLowerCase() != 'owner'))}
                                    handleBeingTouched={() => {
                                                        dispatch(touch(formName, permission.roleId.name))
                                                    }} />

                                <Buttons handleRemoveClicked={() => {
                                                     userGroupPermissions.removeField(index)
                                                 }} />
                            </div>
                        </div>
                    )}
                </div>
                <AddButton handleClick={userGroupPermissions.addField} message="Add new permission" />
            </div>
        )
    }
}