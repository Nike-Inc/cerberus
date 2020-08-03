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

import * as cms from '../../constants/cms';
import { getLogger } from '../../utils/logger';
var log = getLogger('create-new-sdb-validator');

const doesContainNonAlphaNumericSpaceCharsRegex = /[^a-z\d\s]+/i;

// define our client side form validation rules
const validate = values => {
    const errors = {};
    errors.userGroupPermissions = {};
    errors.iamPrincipalPermissions = {};
    errors.foo = {};

    // Validate the Name field
    if (!values.name) {
        errors.name = 'You must enter a name for your SDB';
    } else if (doesContainNonAlphaNumericSpaceCharsRegex.test(values.name)) {
        errors.name = 'Name can only contain alpha numeric chars and spaces.';
    } else if (values.name.length > cms.SDB_NAME_MAX_LENGTH) {
        errors.name = `Name cannot be longer than ${cms.SDB_NAME_MAX_LENGTH} chars`;
    }

    if (values.description && values.description.length > cms.SDB_DESC_MAX_LENGTH) {
        errors.description = `description cannot be longer than ${cms.SDB_DESC_MAX_LENGTH} chars`;
    }

    if (!values.categoryId) {
        errors.categoryId = 'You must select a category';
    }

    if (!values.owner) {
        errors.owner = 'You must select an owning user group';
    }

    if (values.userGroupPermissions) {
        values.userGroupPermissions.map((permission, index) => validateUserGroupPermissions(permission, index, errors));
    }

    if (values.userGroupPermissions) {
        values.iamPrincipalPermissions.map((permission, index) => validateIamPrincipalPermissions(permission, index, errors));
    }

    log.debug('Completed validation returning Error:\n' + JSON.stringify(errors, null, 2));
    return errors;
};

const validateUserGroupPermissions = (permission, index, errors) => {
    errors.userGroupPermissions[`${index}`] = {};
    if (!permission.name) {
        errors.userGroupPermissions[`${index}`].name = 'You must select a user group for this permission';
    }

    if (!permission.roleId) {
        errors.userGroupPermissions[`${index}`].roleId = 'You must select a role for this permission';
    }
};

const validateIamPrincipalPermissions = (permission, index, errors) => {
    errors.iamPrincipalPermissions[`${index}`] = {};

    if (!permission.iamPrincipalArn) {
        errors.iamPrincipalPermissions[`${index}`].iamPrincipalArn = 'You must enter a IAM principal ARN for this permission';
    } else if (! /^arn:(aws|aws-cn):(iam|sts)::.+$/.test(permission.iamPrincipalArn)) {
        errors.iamPrincipalPermissions[`${index}`].iamPrincipalArn = 'Invalid ARN';
    }

    if (!permission.roleId) {
        errors.iamPrincipalPermissions[`${index}`].roleId = 'Required';
    }
};

export default validate;