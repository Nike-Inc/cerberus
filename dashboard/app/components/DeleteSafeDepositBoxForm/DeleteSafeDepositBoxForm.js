import React from 'react'
import { Component } from 'react'
import { connect } from 'react-redux'
import { reduxForm } from 'redux-form'
import * as modalActions from '../../actions/modalActions'
import * as sdbActions from '../../actions/manageSafetyDepositBoxActions'
import validate from './validator'
import './DeleteSafeDepositBoxForm.scss'

const formName = 'delete-sdb-form'
export const fields = [
    'verifiedSdbName',
    'sdbName'
]

// connect to the store for the pieces we care about
@connect((state) => {
    return {
        cerberusAuthToken: state.auth.cerberusAuthToken,
        sdbId: state.manageSafetyDepositBox.data.id,
        sdbName: state.manageSafetyDepositBox.data.name,

        initialValues: {
            verifiedSdbName: '',
            sdbName: state.manageSafetyDepositBox.data.name
        }
    }
})
// wire up the redux form
@reduxForm(
    {
        form: formName,
        fields: fields,
        validate
    }
)
export default class DeleteSafeDepositBoxForm extends Component {
    render() {
        const {
            fields: { verifiedSdbName },
            handleSubmit,
            dispatch,
            cerberusAuthToken,
            sdbId,
            sdbName
        } = this.props

        return (
            <div className="delete-sdb-wrapper">
                <form id="delete-safe-deposit-box" onSubmit={handleSubmit( () => {
                        dispatch(sdbActions.deleteSDB(sdbId, cerberusAuthToken))
                        dispatch(modalActions.popModal())
                })}>
                    <div id="form-description" className="ncss-brand">
                        <h3><span className="attention">ATTENTION:</span>Are you sure you want to delete this safe deposit box?</h3>
                        <h4>You are attempting to delete SDB with name: '{sdbName}'</h4>
                        <h4>Deleting a Safe Deposit Box is irreversible and will delete all data associated with this SDB including past versions of secure data.</h4>
                    </div>

                    <div id='verifiedSdbName' className='ncss-form-group'>
                        <div className={((verifiedSdbName.touched && verifiedSdbName.error) ? 'ncss-input-container error' : 'ncss-input-container')}>
                            <input type='text'
                                   className='ncss-input pt2-sm pr4-sm pb2-sm pl4-sm'
                                   placeholder='Enter the Safe Deposit Box name and confirm deletion'
                                   autoComplete="off"
                                   {...verifiedSdbName}/>
                            {verifiedSdbName.touched && verifiedSdbName.error && <div className='ncss-error-msg'>{verifiedSdbName.error}</div>}
                        </div>
                    </div>

                    <div id="submit-btn-container">
                        <div id='cancel-btn'
                             className='btn ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                             onClick={ () => {
                                 dispatch(modalActions.popModal())
                             }}>Cancel
                        </div>
                        <button id='submit-btn'
                                className='btn ncss-btn-accent ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
                                >Yes, Delete my SDB and all of its data.
                        </button>
                    </div>
                </form>
            </div>
        )
    }
}