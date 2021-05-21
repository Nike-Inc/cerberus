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

import React from 'react'
import { Component } from 'react'
import * as headerActions from '../../actions/headerActions'
import * as authActions from '../../actions/authenticationActions'
import './Header.scss'
import '../../assets/images/cerberus-logo-narrow-off-white.svg'
import * as modalActions from '../../actions/modalActions'
import ViewTokenModal from '../ViewTokenModal/ViewTokenModal'
import { push } from 'connected-react-router';

export default class Header extends Component {

    render() {
        return (
            <header id='header'>
                <div id='bottom-header'>
                    <div id='header-logo' onClick={ function(dispatch) {
                        push('/')
                    }.bind(this, this.props.dispatch)
                    }></div>
                    <div id='header-title' className='ncss-brand u-uppercase un-selectable'>Cerberus</div>
                    <UserBox userName={this.props.userName}
                             displayUserContextMenu={this.props.displayUserContextMenu}
                             dispatch={this.props.dispatch}
                             cerberusAuthToken={this.props.cerberusAuthToken}
                             isAdmin={this.props.isAdmin}/>
                </div>
            </header>
        )
    }
}

class UserBox extends Component {

    constructor(props) {
        super(props)

        this.handleMouseClickUserName = function() {
            this.props.dispatch(headerActions.mouseOverUsername())
        }.bind(this)

        this.handleMouseLeaveUserMenuContext = function() {
            this.props.dispatch(headerActions.mouseOutUsername())
        }.bind(this)

        this.handleMouseClickSdbSummary = function() {
            this.props.dispatch(push('/admin/sdb-metadata'))
        }.bind(this)

        this.handleMouseClickViewToken = function() {
            this.props.dispatch(modalActions.pushModal(<ViewTokenModal />))
        }.bind(this)


        this.handleMouseClickLogout = function() {
            this.props.dispatch(authActions.logoutUser(this.props.cerberusAuthToken))
        }.bind(this)
    }

    render() {
        let isAdmin = this.props.isAdmin

        return (
            <div id='user-box' className='ncss-brand u-uppercase'>
                <div id='u-b-container'
                     onClick={this.handleMouseClickUserName}
                     onMouseLeave={this.handleMouseLeaveUserMenuContext} >
                <div id='u-b-name'>{this.props.userName}</div>
                    <div id='u-b-ico' className={this.props.displayUserContextMenu ? 'ncss-glyph-arrow-down' : 'ncss-glyph-arrow-down rot-90'}></div>
                </div>
                <div id='u-b-context-menu' className={this.props.displayUserContextMenu ? 'show-me-block' : 'hide-me'}
                     onMouseEnter={this.handleMouseClickUserName}
                     onMouseLeave={this.handleMouseLeaveUserMenuContext} >
                    {isAdmin && <div className='context-menu-button' onClick={
                        this.handleMouseClickSdbSummary
                    }>SDB Summary</div>}
                    <div className='context-menu-button' onClick={
                        this.handleMouseClickViewToken
                    }>View Token</div>
                    <div className='context-menu-button' onClick={this.handleMouseClickLogout}>Logout</div>
                </div>
            </div>
        )
    }
}
