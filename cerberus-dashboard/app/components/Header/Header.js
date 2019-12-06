import React from 'react'
import { Component } from 'react'
import { hashHistory } from 'react-router'
import * as headerActions from '../../actions/headerActions'
import * as authActions from '../../actions/authenticationActions'
import './Header.scss'
import '../../assets/images/cerberus-logo-narrow-off-white.svg'
import * as modalActions from '../../actions/modalActions'
import ViewTokenModal from '../ViewTokenModal/ViewTokenModal'

export default class Header extends Component {

    render() {
        return (
            <header id='header'>
                <div id='bottom-header'>
                    <div id='header-logo' onClick={ function(dispatch) {
                        hashHistory.push('/')
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
                    {isAdmin && <div className='context-menu-button' onClick={() => {hashHistory.push('/admin/sdb-metadata')}}>SDB Summary</div>}
                    <div className='context-menu-button' onClick={this.handleMouseClickViewToken}>View Token</div>
                    <div className='context-menu-button' onClick={this.handleMouseClickLogout}>Logout</div>
                </div>
            </div>
        )
    }
}
