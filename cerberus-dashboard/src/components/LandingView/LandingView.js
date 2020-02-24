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
import * as appActions from '../../actions/appActions'
import EnvironmentService from '../../service/EnvironmentService'
import './LandingView.scss'

@connect((state) => {
    return {
        hasDashboardMetadataLoaded: state.app.metadata.hasLoaded,
        dashboardVersion: state.app.metadata.version
    }
})
export default class LandingView extends Component {

    componentDidMount() {
        if (! this.props.hasDashboardMetadataLoaded) {
            this.props.dispatch(appActions.loadDashboardMetadata())
        }
    }

    render() {
        const {dashboardVersion, hasDashboardMetadataLoaded} = this.props

        return (
            <div id='landing-view' className='ncss-brand'>
                <h2>Welcome to the Cerberus Management Dashboard</h2>
                <h3>Environment: {EnvironmentService.getEnvironment()}</h3>
                <h3>API Domain: {EnvironmentService.getDomain()}</h3>

                <div id='loader' className={hasDashboardMetadataLoaded ? 'hide-me' : 'show-me'}>
                    <div id='fountainG'>
                        <div id='fountainG_1' className='fountainG'></div>
                        <div id='fountainG_2' className='fountainG'></div>
                        <div id='fountainG_3' className='fountainG'></div>
                        <div id='fountainG_4' className='fountainG'></div>
                        <div id='fountainG_5' className='fountainG'></div>
                        <div id='fountainG_6' className='fountainG'></div>
                        <div id='fountainG_7' className='fountainG'></div>
                        <div id='fountainG_8' className='fountainG'></div>
                    </div>
                </div>
                <h3 className={hasDashboardMetadataLoaded ? '' : 'hide-me'}>Version: {dashboardVersion}</h3>
                <h4>For help please visit the <a target="_blank" href="/dashboard/help/index.html">help page</a></h4>
            </div>
        )
    }
}