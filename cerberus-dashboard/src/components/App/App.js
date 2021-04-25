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
import { Route, Switch } from 'react-router';
import axios from 'axios';
import * as appActions from '../../actions/appActions';
import Login from '../Login/Login';
import Modal from '../Modal/Modal';
import Header from '../Header/Header';
import Messenger from '../Messenger/Messenger';
import SideBar from '../SideBar/SideBar';
import Footer from '../Footer/Footer';
import './App.scss';
import LandingView from "../LandingView/LandingView";
import ManageSafeDepositBox from "../ManageSafeDepositBox/ManageSafeDepositBox";
import SDBMetadataList from "../SDBMetadataList/SDBMetadataList";
import NotFound from "../NotFound/NotFound";

/**
 * This is the main Component that loads the header, content div and footer
 */
class App extends Component {

    componentDidMount() {
        if (!this.props.hasDashboardMetadataLoaded) {
            this.props.dispatch(appActions.loadDashboardMetadata());
        }
    }

    render() {
        const { isAdmin, userName, displayUserContextMenu, dispatch, cerberusAuthToken, modalStack, children, isSessionExpired, isAuthenticated, dashboardVersion } = this.props;


        axios.defaults.headers.common['X-Cerberus-Client'] = `Dashboard/${dashboardVersion}`;

        return (
            <div id='main-wrapper'>
                <Modal modalStack={modalStack} />
                {!isAuthenticated && <Login />}
                {(isAuthenticated || isSessionExpired) &&
                    <div id='content-wrapper'>
                        <Header userName={userName}
                            displayUserContextMenu={displayUserContextMenu}
                            dispatch={dispatch}
                            cerberusAuthToken={cerberusAuthToken}
                            isAdmin={isAdmin} />
                        {isAuthenticated &&
                            <div id="app-messenger-wrapper">
                                <Messenger />
                            </div>
                        }
                        <div id='content'>
                            <SideBar />
                            <div id='workspace'>
                                <div id='workspace-wrapper'>
                                    <Switch>
                                        <Route
                                            path="/manage-safe-deposit-box/:id"
                                            component={ManageSafeDepositBox}
                                        />
                                        <Route path="/admin/sdb-metadata" component={SDBMetadataList} />
                                        <Route path="/" exact component={LandingView} />
                                        <Route path="*" component={NotFound} />
                                    </Switch>
                                </div>
                            </div>
                        </div>
                        <Footer />
                    </div>
                }
            </div>
        );
    }
}

const mapStateToProps = state => ({
    isAuthenticated: state.auth.isAuthenticated,
    isAdmin: state.auth.isAdmin,
    isSessionExpired: state.auth.isSessionExpired,
    userName: state.auth.userName,
    displayUserContextMenu: state.header.displayUserContextMenu,
    cerberusAuthToken: state.auth.cerberusAuthToken,
    modalStack: state.modal.modalStack,
    hasDashboardMetadataLoaded: state.app.metadata.hasLoaded,
    dashboardVersion: state.app.metadata.version
});

export default connect(mapStateToProps)(App);