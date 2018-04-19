import React from 'react'
import { Component } from 'react'
import { connect } from 'react-redux'

import * as sdbMActions from '../../actions/manageSafetyDepositBoxActions'
import * as appActions from '../../actions/appActions'
import * as vActions from '../../actions/versionHistoryBrowserActions'
import SecureDataBrowser from '../SecureDataBrowser/SecureDataBrowser'
import SecureDataVersionsBrowser from '../SecureDataVersionsBrowser/SecureDataVersionsBrowser'
import SafeDepositBoxSettings from '../SafeDepositBoxSettings/SafeDepositBoxSettings'

import './ManageSafeDepositBox.scss'

// connect to the store for the pieces we care about
@connect((state) => {
    return {
        // user info
        cerberusAuthToken: state.auth.cerberusAuthToken,

        // Domain data
        hasDomainDataLoaded: state.app.cmsDomainData.hasLoaded,
        categories: state.app.cmsDomainData.categories,
        roles: state.app.cmsDomainData.roles,

        // SDB Data
        hasFetchedSDBData: state.manageSafetyDepositBox.hasFetchedSDBData,
        sdbData: state.manageSafetyDepositBox.data,
        navigatedPath: state.manageSafetyDepositBox.navigatedPath,
        keysForSecureDataPath: state.manageSafetyDepositBox.keysForSecureDataPath,
        secureObjectData: state.manageSafetyDepositBox.secureData,
        secureFileData: state.manageSafetyDepositBox.secureFileData,
        hasFetchedObjectKeys: state.manageSafetyDepositBox.hasFetchedObjectKeys,
        hasFetchedFileKeys: state.manageSafetyDepositBox.hasFetchedFileKeys,
        showAddSecretForm: state.manageSafetyDepositBox.showAddSecretForm,
        showAddFileForm: state.manageSafetyDepositBox.showAddFileForm,
        showDeleteDialog: state.manageSafetyDepositBox.showDeleteDialog,
        nav: state.manageSafetyDepositBox.nav
    }
})
export default class ManageSafeDepositBox extends Component {

    componentWillReceiveProps(nextProps) {
        // Fetch and load SDB details based on id in uri
        if (nextProps.routeParams.id !== this.props.routeParams.id) {
            this.props.dispatch(sdbMActions.fetchSDBDataFromCMS(nextProps.routeParams.id, this.props.cerberusAuthToken))
        }
    }

    /**
     * Fetch the data about the SDB from CMS
     */
    componentDidMount() {
        if (! this.props.hasDomainDataLoaded) {
            this.props.dispatch(appActions.fetchCmsDomainData(this.props.cerberusAuthToken))
        }
        if (! this.props.hasFetchedSDBData) {
            this.props.dispatch(sdbMActions.fetchSDBDataFromCMS(this.props.routeParams.id, this.props.cerberusAuthToken))
        }
    }

    handleNavItemClicked(navItem) {
        if (navItem === 'secureDataVersions') {
            this.props.dispatch(vActions.resetVersionBrowserState())
        }

        this.props.dispatch(sdbMActions.navItemClicked(navItem))
    }

    render() {
        const {
            hasDomainDataLoaded,
            hasFetchedSDBData,
            sdbData,
            navigatedPath,
            keysForSecureDataPath,
            dispatch,
            cerberusAuthToken,
            hasFetchedObjectKeys,
            hasFetchedFileKeys,
            secureObjectData,
            secureFileData,
            showAddSecretForm,
            showAddFileForm,
            roles,
            categories,
            nav,
            showDeleteDialog
        } = this.props

        if (!hasDomainDataLoaded || !hasFetchedSDBData) {
            return(<div></div>)
        }

        return(
            <div className="safe-deposit-box-content-wrapper">
                <div className="safe-deposit-box-content-section safe-deposit-box-header">
                    <div className="safe-deposit-box-header-item safe-deposit-box-header-name ncss-brand u-uppercase">{sdbData.name}</div>
                    <div className="safe-deposit-box-header-item safe-deposit-box-header-description">{sdbData.description}</div>
                </div>
                <div className="safe-deposit-box-content-section safe-deposit-box-nav">
                    <div
                        className={"un-selectable nav-item secure-data ncss-brand u-uppercase" + (nav.secureDataSelected ? ' nav-item-selected' : '')}
                        onClick={() => { this.handleNavItemClicked('secureData')}}
                    >Secure Data</div>
                    <div
                        className={"un-selectable nav-item versions ncss-brand u-uppercase" + (nav.secureDataVersionsSelected ? ' nav-item-selected' : '')}
                        onClick={() => {
                            dispatch(vActions.fetchVersionDataForSdb(sdbData.id, cerberusAuthToken))
                            this.handleNavItemClicked('secureDataVersions')
                        }}
                    >Secure Data Version History</div>
                    <div
                        className={"un-selectable nav-item setting ncss-brand u-uppercase" + (nav.sdbSettingsSelected ? ' nav-item-selected' : '')}
                        onClick={() => { this.handleNavItemClicked('sdbSettings')}}
                    >Settings</div>
                </div>
                <div className="safe-deposit-box-content-section safe-deposit-box-sub-component">
                    {nav.secureDataSelected &&
                        <SecureDataBrowser cerberusAuthToken={cerberusAuthToken}
                                           hasFetchedObjectKeys={hasFetchedObjectKeys}
                                           hasFetchedFileKeys={hasFetchedFileKeys}
                                           navigatedPath={navigatedPath}
                                           keysForSecureDataPath={keysForSecureDataPath}
                                           secureObjectData={secureObjectData}
                                           secureFileData={secureFileData}
                                           showAddSecretForm={showAddSecretForm}
                                           showAddFileForm={showAddFileForm}
                                           dispatch={dispatch} />
                    }

                    {nav.secureDataVersionsSelected &&
                        <SecureDataVersionsBrowser safeDepositBoxId={sdbData.id}/>
                    }

                    {nav.sdbSettingsSelected &&
                        <SafeDepositBoxSettings categories={categories}
                                                roles={roles}
                                                sdbData={sdbData}
                                                dispatch={dispatch}
                                                cerberusAuthToken={cerberusAuthToken}
                                                showDeleteDialog={showDeleteDialog}/>
                    }
                </div>
            </div>
        )
    }

}