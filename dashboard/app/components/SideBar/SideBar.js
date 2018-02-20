import React from 'react'
import { Component } from 'react'
import { connect } from 'react-redux'
import * as appActions from '../../actions/appActions'
import * as mSDBActions from '../../actions/manageSafetyDepositBoxActions'
import * as vActions from '../../actions/versionHistoryBrowserActions'
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
        return (
            <div className='sidebar'>
                <div className="sidebar-header ncss-brand u-uppercase un-selectable">Safe Deposit Boxes</div>
                { sideBarContent(this.handleMouseClickAddNewBucket, this.handleSDBClicked, this.props) }
            </div>
        )
    }
}

const sideBarContent = (handleMouseClickAddNewBucket, handleSDBClicked, props) => {
    if (props.isFetching) {
        return (
            <div className='loader'>
                <Loader/>
            </div>
        )
    }

    return (
        <div className='sidebar-categories'>
            {categories(handleMouseClickAddNewBucket, handleSDBClicked, props.data)}
        </div>
    )
}

const categories = (handleMouseClickAddNewBucket, handleSDBClicked, data) => {
    var categories = []
    for (let key in data) {
        if (data.hasOwnProperty(key)) {
            let category = data[key]
            categories.push(
                <div key={category.id}>
                    <div className='category'>
                        <div className='category-label ncss-brand u-uppercase un-selectable'>{category.name}</div>
                    </div>
                    <div className="add-new-sdb ncss-brand u-uppercase un-selectable sidebar-button"
                         onClick={() => {
                             handleMouseClickAddNewBucket(category.id)
                         }}>
                        <div className="ncss-glyph-plus-lg icon"></div>
                        <div className="txt">Create a New SDB</div>
                    </div>
                    {bucketComponents(category.boxes, handleSDBClicked)}
                </div>
            )
        }
    }
    return categories
}

const bucketComponents = (boxes, handleSDBClicked) => {
    let bucketComponents = []
    for (let box of boxes) {
        bucketComponents.push(
            <div className='border-bottom-light-grey sidebar-button un-selectable'
                 key={box.id}
                 onClick={handleSDBClicked.bind(this, box.id, box.path)} >
                <div className='ncss-brand u-uppercase'>{box.name}</div>
            </div>
        )
    }
    return bucketComponents
}