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