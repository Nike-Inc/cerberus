import React from 'react'
import { Component } from 'react'
import { connect } from 'react-redux'
import * as appActions from '../../actions/appActions'
import * as mSDBActions from '../../actions/manageSafetyDepositBoxActions'
import Category from './views/Category'
import './SideBar.scss'
import { getLogger } from 'logger'
var log = getLogger('side-bar-component')

@connect((state) => {
    return {
        vaultToken: state.auth.vaultToken,
        data: state.app.sideBar.data,
        isFetching: state.app.sideBar.isFetching,
        hasLoaded: state.app.sideBar.hasLoaded
    }
})
export default class SideBar extends Component {

    constructor(props) {
        super(props)
        if (! this.props.hasLoaded) {
            this.props.dispatch(appActions.fetchSideBarData(this.props.vaultToken))
        }

        this.handleMouseClickAddNewBucket = (id) => {
            this.props.dispatch(appActions.addBucketBtnClicked(id))
        }

        this.handleSDBClicked = (id, path) => {
            this.props.dispatch(mSDBActions.hideAddNewVaultSecret())
            this.props.dispatch(appActions.loadManageSDBPage(id, path, this.props.vaultToken))
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
                <ul id='sidebar-categories' className={this.props.isFetching ? 'hide-me' : ''}>
                    {categories}
                </ul>
            </div>
        )
    }
}