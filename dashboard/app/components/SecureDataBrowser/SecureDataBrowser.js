import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'
import './SecureDataBrowser.scss'
import AddButton from '../AddButton/AddButton'
import SecureDataForm from '../SecureDataForm/SecureDataForm'
import SecureData from '../SecureData/SecureData'
import * as mSDBActions from '../../actions/manageSafetyDepositBoxActions'
import { getLogger } from 'logger'
import SecureFileForm from '../SecureFileForm/SecureFileForm';
import FileButton from '../FileButton/FileButton';
import SecureFile from '../SecureFile/SecureFile';

var log = getLogger('secure-data-browser-component')

export default class SecureDataBrowser extends Component {
    static propTypes = {
        hasFetchedObjectKeys: PropTypes.bool.isRequired,
        hasFetchedFileKeys: PropTypes.bool.isRequired,
        cerberusAuthToken: PropTypes.string.isRequired,
        navigatedPath: PropTypes.string.isRequired,
        keysForSecureDataPath: PropTypes.object,
        secureObjectData: PropTypes.object.isRequired,
        secureFileData: PropTypes.object.isRequired,
        dispatch: PropTypes.func.isRequired,
        showAddSecretForm: PropTypes.bool.isRequired,
        showAddFileForm: PropTypes.bool.isRequired
    }

    componentDidMount() {
        if (!this.props.hasFetchedObjectKeys) {
            this.props.dispatch(mSDBActions.fetchSecureDataPathKeys(this.props.navigatedPath, this.props.cerberusAuthToken));
        }
        if (!this.props.hasFetchedFileKeys) {
            this.props.dispatch(mSDBActions.fetchSecureFilePathKeys(this.props.navigatedPath, this.props.cerberusAuthToken));
        }
    }

    render() {
        const {
            cerberusAuthToken, navigatedPath, keysForSecureDataPath, secureObjectData,
            secureFileData, dispatch, hasFetchedObjectKeys, hasFetchedFileKeys,
            showAddSecretForm, showAddFileForm
        } = this.props

        if (!hasFetchedObjectKeys || !hasFetchedFileKeys) {
            return(<div></div>)
        }

        log.debug(JSON.stringify(keysForSecureDataPath))

        return (
            <div id='secure-data-browser'>
                <SecureDataBreadcrumb path={navigatedPath} dispatch={dispatch} cerberusAuthToken={cerberusAuthToken} />
                <div id='secure-data-keys'>
                    {keysForSecureDataPath && Object.keys(keysForSecureDataPath).sort().map((key) => {
                        return(
                            <div key={key} className='secure-data-key' id={`secure-data-key-${key}`}>
                                { keysForSecureDataPath[key].type === 'file' ?
                                    (<SecureFile label={key}
                                                 dispatch={dispatch}
                                                 navigatedPath={navigatedPath}
                                                 cerberusAuthToken={cerberusAuthToken}
                                                 secureFileData={secureFileData}
                                                 isActive={`${navigatedPath}${key}` in secureFileData ? secureFileData[`${navigatedPath}${key}`].isActive : false}
                                                 isFetching={`${navigatedPath}${key}` in secureFileData ? secureFileData[`${navigatedPath}${key}`].isFetching : false}
                                                 sdbData={`${navigatedPath}${key}` in secureFileData ? secureFileData[`${navigatedPath}${key}`].data : {}} />
                                    ) :
                                    (<SecureData label={key}
                                                 navigatedPath={navigatedPath}
                                                 secureObjectData={secureObjectData}
                                                 cerberusAuthToken={cerberusAuthToken}
                                                 dispatch={dispatch}
                                                 isActive={`${navigatedPath}${key}` in secureObjectData ? secureObjectData[`${navigatedPath}${key}`].isActive : false}
                                                 isFetching={`${navigatedPath}${key}` in secureObjectData ? secureObjectData[`${navigatedPath}${key}`].isFetching : false}
                                                 sdbData={`${navigatedPath}${key}` in secureObjectData ? secureObjectData[`${navigatedPath}${key}`].data : {}}/>
                                    )
                                }
                            </div>
                        )
                    })}
                </div>
                <div id="add-container">
                    {! showAddSecretForm && ! showAddFileForm &&
                        <div id={"add-buttons-container"}>
                            <AddButton handleClick={() => {
                                dispatch(mSDBActions.showAddNewSecureData())
                            }} message="Add New Secure Data Path" />
                            <FileButton handleClick={() => {
                                dispatch(mSDBActions.showAddNewSecureFile())
                            }} message="Upload a File" />

                        </div>
                    }
                    {showAddSecretForm &&
                        <div className="add-form-container">
                            <div className="secure-data-form-label ncss-brand u-uppercase">Add a new Secure Data Path</div>
                            <SecureDataForm initialValues={{
                                                             kvMap: [{key: null, value: null, revealed: true}],
                                                             path: null
                                                           }}
                                            pathReadOnly={false}
                                            formKey={"add-new-secure-data"} />
                        </div>
                    }
                    {showAddFileForm &&
                        <div className="add-form-container">
                            <div className="secure-data-form-label ncss-brand u-uppercase">Upload a New File</div>
                            <SecureFileForm initialValues={{
                                                              path: null,
                                                              uploadedFileData: {}
                                                          }}
                                            pathReadOnly={false}
                                            formKey={"add-new-secure-file"} />
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