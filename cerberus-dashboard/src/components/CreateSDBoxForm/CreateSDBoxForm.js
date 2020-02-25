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

import React from 'react';
import { Component } from 'react';
import { connect } from 'react-redux';
import { reduxForm, touch } from 'redux-form';
import * as modalActions from '../../actions/modalActions';
import * as appActions from '../../actions/appActions';
import * as cNSDBActions from '../../actions/createSDBoxActions';
import CategorySelect from '../CategorySelect/CategorySelect';
import GroupsSelect from '../GroupSelect/GroupsSelect';
import validate from './validator';
import * as cms from '../../constants/cms';
import UserGroupPermissionsFieldSet from '../UserGroupPermissionsFieldSet/UserGroupPermissionsFieldSet';
import IamPrincipalPermissionsFieldSet from '../IamPrincipalPermissionsFieldSet/IamPrincipalPermissionsFieldSet';
import SDBDescriptionField from '../SDBDescriptionField/SDBDescriptionField';

import './CreateSDBoxForm.scss';

import { getLogger } from 'logger';
var log = getLogger('create-new-sdb');

const formName = 'create-new-sdb-form';

/**
 * This is the smart component for the create new bucket page,
 * This component contains the actual for and handle passing needed things into the dumb component views
 */
export const fields = [
    'name',
    'categoryId',
    'description',
    'owner',
    'userGroupPermissions[].name',
    'userGroupPermissions[].roleId',
    'iamPrincipalPermissions[].iamPrincipalArn',
    'iamPrincipalPermissions[].roleId'
];

// connect to the store for the pieces we care about
export default 
@connect((state) => {
    return {
        // user info
        cerberusAuthToken: state.auth.cerberusAuthToken,
        userGroups: state.auth.groups,

        // domain data for the drop downs
        hasDomainDataLoaded: state.app.cmsDomainData.hasLoaded,
        categories: state.app.cmsDomainData.categories,
        roles: state.app.cmsDomainData.roles,

        // data for the form
        isSubmitting: state.nSDB.isSubmitting,
        initialValues: {
            categoryId: state.nSDB.selectedCategoryId
        }
    };
})
// wire up the redux form
@reduxForm(
    {
        form: formName,
        fields: fields,
        validate
    }
)
class CreateSDBoxForm extends Component {

    /**
     * Force the domain data to load if it hasn't
     */
    componentDidMount() {
        if (!this.props.hasDomainDataLoaded) {
            this.props.dispatch(appActions.fetchCmsDomainData(this.props.cerberusAuthToken));
        }
    }

    render() {
        const {
            fields: {
                name,
                description,
                categoryId,
                owner,
                userGroupPermissions,
                iamPrincipalPermissions
            },
            categories,
            handleSubmit,
            isSubmitting,
            hasDomainDataLoaded,
            dispatch,
            roles,
            userGroups,
            cerberusAuthToken
        } = this.props;

        // Lets not attempt to render everything until we have the data we need, when the domain data has loaded we can pass this
        if (!hasDomainDataLoaded) {
            return (<div></div>);
        }

        log.debug('Form props:\n' + JSON.stringify(this.props.fields, null, 2));

        return (
            <form id='new-sdbox-form' onSubmit={handleSubmit(data => {
                dispatch(cNSDBActions.submitCreateNewSDB(data, cerberusAuthToken));
            })}>
                <div id="form-description" className="ncss-brand">
                    <h1>Create a New Safe Deposit Box</h1>
                    <h4>Fill out the below form to create a safe deposit box, where you can store properties securely.</h4>
                </div>

                <div id='name' className='ncss-form-group'>
                    <div className={((name.touched && name.error) ? 'ncss-input-container error' : 'ncss-input-container')}>
                        <label className='ncss-label'>Name</label>
                        <input type='text'
                            className='ncss-input pt2-sm pr4-sm pb2-sm pl4-sm'
                            placeholder='Enter an immutable name for your new Safe Deposit Box'
                            maxLength={`${cms.SDB_NAME_MAX_LENGTH}`}
                            {...name} />
                        {name.touched && name.error && <div className='ncss-error-msg'>{name.error}</div>}
                    </div>
                </div>

                <div id='top-section'>
                    <CategorySelect {...categoryId} categories={categories} handleBeingTouched={() => { dispatch(touch(formName, 'categoryId')); }} />
                    <div id="owner">
                        <label id="category-select-label" className='ncss-label'>Owner</label>
                        <GroupsSelect {...owner}
                            userGroups={userGroups}
                            allowCustomValues={false}
                            handleBeingTouched={() => {
                                dispatch(touch(formName, owner.name));
                            }} />
                    </div>
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
                        className='btn ncss-btn-accent ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                        onClick={() => {
                            dispatch(modalActions.popModal());
                        }}>Cancel
                    </div>
                    <button id='submit-btn'
                        className='btn ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                        disabled={isSubmitting}>Submit
                    </button>
                </div>
            </form>
        );
    }
}









