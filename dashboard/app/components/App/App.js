import React from 'react'
import { Component } from 'react'
import { connect } from 'react-redux'
import axios from 'axios'
import * as appActions from '../../actions/appActions'
import Login from '../Login/Login'
import Modal from '../Modal/Modal'
import Header from '../Header/Header'
import Messenger from '../Messenger/Messenger'
import SideBar from '../SideBar/SideBar'
import Footer from '../Footer/Footer'
import './App.scss'

/**
 * This is the main Component that loads the header, content div and footer
 */
@connect((state) => {
    return {
        isAuthenticated: state.auth.isAuthenticated,
        isAdmin: state.auth.isAdmin,
        isSessionExpired: state.auth.isSessionExpired,
        userName: state.auth.userName,
        displayUserContextMenu: state.header.displayUserContextMenu,
        vaultToken: state.auth.vaultToken,
        modalStack: state.modal.modalStack,
        hasDashboardMetadataLoaded: state.app.metadata.hasLoaded,
        dashboardVersion: state.app.metadata.version
    }
})


export default class App extends Component {

    componentDidMount() {
        if (! this.props.hasDashboardMetadataLoaded) {
            this.props.dispatch(appActions.loadDashboardMetadata())
        }
    }

    render() {
        const {isAdmin, userName, displayUserContextMenu, dispatch, vaultToken, modalStack, children, isSessionExpired, isAuthenticated, dashboardVersion} = this.props

        axios.defaults.headers.common['X-Cerberus-Client'] = `Dashboard/${dashboardVersion}`

        return (
            <div id='main-wrapper'>
                <Modal modalStack={modalStack} />

                { ! isAuthenticated && <Login />}
                { (isAuthenticated || isSessionExpired) &&
                <div id='content-wrapper'>
                    <Header userName={userName}
                            displayUserContextMenu={displayUserContextMenu}
                            dispatch={dispatch}
                            vaultToken={vaultToken}
                            isAdmin={isAdmin}/>
                    { isAuthenticated &&
                        <div id="app-messenger-wrapper">
                            <Messenger />
                        </div>
                    }
                    <div id='content'>
                        <SideBar />
                        <div id='workspace'>
                            <div id='workspace-wrapper'>
                                {children}
                            </div>
                        </div>
                    </div>
                    <Footer />
                </div>
                }
            </div>
        )
    }
}
