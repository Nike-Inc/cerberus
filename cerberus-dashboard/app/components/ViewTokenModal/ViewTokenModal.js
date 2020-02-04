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
import { connect } from 'react-redux'
import { reduxForm, touch } from 'redux-form'
import * as modalActions from '../../actions/modalActions'
import * as appActions from '../../actions/appActions'
import * as authActions from '../../actions/authenticationActions'
import CopyToClipboard from 'react-copy-to-clipboard';

import './ViewTokenModal.scss'

// connect to the store in order to obtain the current cerberus auth token
@connect((state) => {
  return {
    clientToken: state.auth.cerberusAuthToken,
    isAuthenticating: state.auth.isAuthenticating
  }
})

export default class ViewTokenModal extends Component {

  constructor(props) {
    super(props)

    this.state = {
      isTokenRevealed: false
    }

    this.handleHideButtonClicked = function() {
      this.setState({ isTokenRevealed: !this.state.isTokenRevealed });
    }.bind(this)

    this.handleRenewTokenClicked = function() {
      this.props.dispatch(authActions.refreshAuth(this.props.clientToken, '', false))
    }.bind(this)
  }

  render() {
    const {clientToken, dispatch, isAuthenticating} = this.props
    let tokenExpiresDate = sessionStorage.getItem('tokenExpiresDate');

    return (
      <div id='view-token-modal'>
        <div id="view-token-modal-description" className="ncss-brand">
          <h1>View and Renew Client Token</h1>
          <h4>Below is your current client token which can be used for debugging purposes or for local development, e.g. "export CERBERUS_TOKEN=..."</h4>
        </div>

        <div className="view-token-modal-token-wrapper">
          <div className="view-token-modal-token">
            <div className="view-token-modal-data-label">Client Token:</div>
            <div className="view-token-modal-data-token-value"
              style={{display: this.state.isTokenRevealed ? 'block' : 'none' }}
              >{clientToken}
            </div>
            <div className="secret-value-placeHolder"
              style={{display: ! this.state.isTokenRevealed? 'block' : 'none' }}
              >Hidden, click the reveal button
            </div>
          </div>
          <div className='row-buttons'>
            <div className="btn-wrapper btn-wrapper-left">
              <input type="checkbox" className={! this.state.isTokenRevealed ? 'row-btn row-btn-reveal' : 'row-btn row-btn-revealed'} {...this.state.isTokenRevealed}
                onClick={this.handleHideButtonClicked}/>
              </div>
              <CopyToClipboard text={clientToken}>
                <div className={clientToken.length <= 1 ? 'btn-wrapper btn-wrapper-right' : 'btn-wrapper'}>
                  <div className='row-btn row-btn-copy'></div>
                </div>
              </CopyToClipboard>
            </div>
          </div>

          <div className="view-token-modal-date-wrapper">
            <div className="view-token-modal-date">
              <div className="view-token-modal-data-label">Client Token Expiration Date:</div>
              <div className="view-token-modal-data-date-value">{tokenExpiresDate}</div>
            </div>
          </div>
          <div className="view-token-modal-time-left-wrapper">
            <div className="view-token-modal-time-left">
              <div className="view-token-modal-data-label">Client Token Time Remaining:</div>
              <div className="view-token-modal-data-time-left-value">{dateDiffinMin(tokenExpiresDate)} minutes</div>
            </div>
          </div>

          <div id="renew-btn-container">
            <div id='fountainG' className={isAuthenticating ? 'show-me' : 'hide-me'}>
              <div id='fountainG_1' className='fountainG'></div>
              <div id='fountainG_2' className='fountainG'></div>
              <div id='fountainG_3' className='fountainG'></div>
              <div id='fountainG_4' className='fountainG'></div>
              <div id='fountainG_5' className='fountainG'></div>
              <div id='fountainG_6' className='fountainG'></div>
              <div id='fountainG_7' className='fountainG'></div>
              <div id='fountainG_8' className='fountainG'></div>
            </div>
            <div id='close-btn'
              className='btn ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
              onClick={ () => {
                dispatch(modalActions.popModal())
              }}>Close
            </div>
            <button id='renew-btn'
              className='btn ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
              onClick={this.handleRenewTokenClicked}
              disabled={isAuthenticating}
              >Renew Token
            </button>
          </div>
        </div>
      )
    }
  }

  export function dateDiffinMin(expiresDate) {
    var expDate = new Date(expiresDate)
    var currentDate = new Date()
    var t2 = expDate.getTime();
    var t1 = currentDate.getTime();
    var diff = parseInt((t2-t1)/(60*1000));
    return diff
  }
