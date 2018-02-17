import React from 'react'
import { Component } from 'react'
import { connect } from 'react-redux'
import * as appActions from '../../actions/appActions'
import * as mSDBActions from '../../actions/manageSafetyDepositBoxActions'
import * as vActions from '../../actions/versionHistoryBrowserActions'
import Category from './views/Category'
import Loader from '../Loader/Loader'
import './SideBar.scss'
import { getLogger } from 'logger'
var log = getLogger('side-bar-component')

@connect((state) => {
    return {
        cerberusAuthToken: state.auth.cerberusAuthToken,
        data: state.app.sideBar.data,
        isFetching: state.app.sideBar.isFetching,
        hasLoaded: state.app.sideBar.hasLoaded
    }
})
export default class SideBar extends Component {

    constructor(props) {
        super(props)
        if (! this.props.hasLoaded) {
            this.props.dispatch(appActions.fetchSideBarData(this.props.cerberusAuthToken))
        }

        this.handleMouseClickAddNewBucket = (id) => {
            this.props.dispatch(appActions.addBucketBtnClicked(id))
        }

        this.handleSDBClicked = (id, path) => {
            this.props.dispatch(mSDBActions.hideAddNewSecureData())
            this.props.dispatch(vActions.resetVersionBrowserState())
            this.props.dispatch(appActions.loadManageSDBPage(id, path, this.props.cerberusAuthToken))
        }
    }

    render() {
        var categories = []
        for (let key in this.props.data) {
            if (this.props.data.hasOwnProperty(key)) {
                var category = this.props.data[key]
                categories.push(<Category key={category.id}
                                          id={category.id}
                                          name={category.name}
                                          boxes={category.boxes}
                                          handleMouseClickAddNewBucket={this.handleMouseClickAddNewBucket}
                                          handleSDBClicked={this.handleSDBClicked}/>)
            }
        }

        return (
            <div id='side-bar'>
                <div id='loader' className={this.props.isFetching ? 'show-me' : 'hide-me'}>
                    <Loader/>
                </div>
                <ul id='sidebar-categories' className={this.props.isFetching ? 'hide-me' : ''}>
                    {categories}
                </ul>
            </div>
        )
    }
}