import React from 'react'
import { Component } from 'react'
import { connect } from 'react-redux'
import { reduxForm, touch } from 'redux-form'

import GroupsSelect from '../GroupSelect/GroupsSelect'
import UserGroupPermissionsFieldSet from '../UserGroupPermissionsFieldSet/UserGroupPermissionsFieldSet'
import IamPrincipalPermissionsFieldSet from '../IamPrincipalPermissionsFieldSet/IamPrincipalPermissionsFieldSet'
import SDBDescriptionField from '../SDBDescriptionField/SDBDescriptionField'

import * as modalActions from '../../actions/modalActions'
import * as manageSafetyDepositBoxActions from '../../actions/manageSafetyDepositBoxActions'

import './EditSDBoxForm.scss'

import { getLogger } from 'logger'
var log = getLogger('edit-sdb')

const formName = 'edit-sdb'

//state.manageSafetyDepositBox.data
export const fields = [
    'description',
    'owner',
    'userGroupPermissions[].name',
    'userGroupPermissions[].roleId',
    'iamPrincipalPermissions[].iamPrincipalArn',
    'iamPrincipalPermissions[].roleId'
]

class EditSDBoxForm extends Component {


    render() {
        const { fields: { description, owner, userGroupPermissions, iamPrincipalPermissions },
            cerberusAuthToken, sdbId, roles, userGroups, hasDomainDataLoaded, dispatch, handleSubmit, isEditSubmitting } = this.props

        // Lets not attempt to render everything until we have the data we need, when the domain data has loaded we can pass this
        if (! hasDomainDataLoaded) {
            return(<div></div>)
        }

        return (
            <form id="edit-sdb-form" onSubmit={ handleSubmit( data => {
                dispatch(manageSafetyDepositBoxActions.submitEditSDBRequest(sdbId, data, cerberusAuthToken))
            })}>

                <div id="form-description" className="ncss-brand">
                    <h1>Edit Safe Deposit Box</h1>
                    <h4>Edit the below safe deposit box.</h4>
                </div>

                <div id="owner">
                    <label id="category-select-label" className='ncss-label'>Owner</label>
                    <GroupsSelect {...owner}
                        userGroups={userGroups}
                        allowCustomValues={false}
                        handleBeingTouched={() => {
                                  dispatch(touch(formName, owner.name))
                              }} />
                </div>

                <SDBDescriptionField description={description} />

                <UserGroupPermissionsFieldSet userGroupPermissions={userGroupPermissions}
                                              dispatch={dispatch}
                                              formName={formName}
                                              userGroups={userGroups}
                                              roles={roles} />

                <IamPrincipalPermissionsFieldSet iamPrincipalPermissions={iamPrincipalPermissions}
                                                 dispatch={dispatch}
                                                 formName={formName}
                                                 roles={roles} />

                <div id="submit-btn-container">
                    <div id='cancel-btn'
                         className='btn ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                         onClick={ () => {
                                dispatch(modalActions.popModal())

                            }}>Cancel
                    </div>
                    <button id='submit-btn'
                            className='btn ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                            disabled={isEditSubmitting}>Submit
                    </button>
                </div>

            </form>
        )
    }


}

let form = reduxForm({
    form: formName,
    fields
})(EditSDBoxForm)

export default connect((state) => {
    const { id, description, owner, userGroupPermissions, iamPrincipalPermissions } = state.manageSafetyDepositBox.data

    return {
        cerberusAuthToken: state.auth.cerberusAuthToken,
        hasDomainDataLoaded: state.app.cmsDomainData.hasLoaded,
        roles: state.app.cmsDomainData.roles,
        userGroups: state.auth.groups,
        isEditSubmitting: state.manageSafetyDepositBox.isEditSubmitting,
        sdbId: id,

        initialValues: {
            description: description,
            owner: owner,
            userGroupPermissions: userGroupPermissions,
            iamPrincipalPermissions: iamPrincipalPermissions
        }
    }
})(form)