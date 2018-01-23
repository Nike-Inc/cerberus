import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'
import './SecureDataBrowser.scss'
import AddButton from '../AddButton/AddButton'
import SecureDataForm from '../SecureDataForm/SecureDataForm'
import SecureData from '../SecureData/SecureData'
import * as mSDBActions from '../../actions/manageSafetyDepositBoxActions'
import { getLogger } from 'logger'
var log = getLogger('secure-data-browser-component')

export default class SecureDataBrowser extends Component {
    static propTypes = {
        hasFetchedKeys: PropTypes.bool.isRequired,
        cerberusAuthToken: PropTypes.string.isRequired,
        navigatedPath: PropTypes.string.isRequired,
        keysForSecureDataPath: PropTypes.array,
        secureData: PropTypes.object.isRequired,
        dispatch: PropTypes.func.isRequired,
        showAddSecretForm: PropTypes.bool.isRequired
    }

    componentDidMount() {
        if (!this.props.hasFetchedKeys) {
            this.props.dispatch(mSDBActions.fetchSecureDataPathKeys(this.props.navigatedPath, this.props.cerberusAuthToken))
        }
    }

    render() {
        const {cerberusAuthToken, navigatedPath, keysForSecureDataPath, secureData, dispatch, hasFetchedKeys, showAddSecretForm} = this.props

        if (!hasFetchedKeys) {
            return(<div></div>)
        }

        log.debug(JSON.stringify(keysForSecureDataPath))

        return (
            <div id='secure-data-browser'>
                <SecureDataBreadcrumb path={navigatedPath} dispatch={dispatch} cerberusAuthToken={cerberusAuthToken} />
                <div id='secure-data-keys'>
                    {keysForSecureDataPath && keysForSecureDataPath.map((key) =>
                        <div key={key} className='secure-data-key' id={`secure-data-key-${key}`}>
                            <SecureData label={key}
                                         navigatedPath={navigatedPath}
                                         secureData={secureData}
                                         cerberusAuthToken={cerberusAuthToken}
                                         dispatch={dispatch}
                                         isActive={`${navigatedPath}${key}` in secureData ? secureData[`${navigatedPath}${key}`].isActive : false}
                                         isFetching={`${navigatedPath}${key}` in secureData ? secureData[`${navigatedPath}${key}`].isFetching : false}
                                         sdbData={`${navigatedPath}${key}` in secureData ? secureData[`${navigatedPath}${key}`].data : {}} />
                        </div>
                    )}
                </div>
                <div id="add-container">
                    {! showAddSecretForm &&
                        <AddButton handleClick={() => {
                            dispatch(mSDBActions.showAddNewSecureData())
                        }} message="Add New Secret Path" />
                    }
                    {showAddSecretForm &&
                        <div id="add-form-container">
                            <div className="secure-data-form-label">Add a new Secret Path</div>
                            <SecureDataForm initialValues={{
                                                           kvMap: [{key: null, value: null, revealed: true}],
                                                           path: null
                                                         }}
                                             pathReadOnly={false}
                                             formKey={`add-new-secure-data`} />
                        </div>
                    }
                </div>
            </div>
        )
    }
}

class SecureDataBreadcrumb extends Component {
    static propTypes = {
        path: PropTypes.string.isRequired,
        dispatch: PropTypes.func.isRequired,
        cerberusAuthToken: PropTypes.string.isRequired
    }

    handlePathPieceClick(indexClicked) {
        let pathClicked = this.props.path.split('/').filter(Boolean).slice(0, indexClicked + 1).join('/')+'/'
        this.props.dispatch(mSDBActions.updateNavigatedPath(pathClicked, this.props.cerberusAuthToken))
    }

    render() {
        let pathPieces = this.props.path.split('/').filter(Boolean)

        return(
            <div id='secure-data-path'>
                <div id='secure-data-path-label'>Current Location:</div>
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