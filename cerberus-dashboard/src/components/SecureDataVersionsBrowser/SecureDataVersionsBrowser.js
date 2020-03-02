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
import Loader from '../Loader/Loader';
import ReactPaginate from 'react-paginate';
import Select from 'react-select';
import * as vActions from '../../actions/versionHistoryBrowserActions.js';
import JSONPretty from 'react-json-pretty';
import './SecureDataVersionsBrowser.scss';

class SecureDataVersionsBrowser extends Component {

    dispatch = this.props.dispatch;

    // When the component mounts fetch the initial history data for the Safe Deposit Box
    componentDidMount() {
        if (!this.props.hasFetchedPathsWithHistory) {
            this.dispatch(vActions.fetchVersionDataForSdb(this.props.safeDepositBoxId, this.props.cerberusAuthToken));
        }
    }

    handlePageClick = (data) => {
        let pageNumber = data.selected;

        this.dispatch(vActions.updatePageNumber(pageNumber));
        this.dispatch(vActions.fetchPathVersionData(this.props.versionPathSelected, this.props.cerberusAuthToken, pageNumber, this.props.versionPathPerPage));
    };

    handlePerPageSelect = (selected) => {
        let perPage = selected.value;
        let pageNumber = 0; // default back to the first page

        this.dispatch(vActions.updatePerPage(perPage));
        this.dispatch(vActions.updatePageNumber(pageNumber));
        this.dispatch(vActions.fetchPathVersionData(this.props.versionPathSelected, this.props.cerberusAuthToken, pageNumber, perPage));
    };

    handleBreadCrumbHomeClick = () => {
        this.dispatch(vActions.handleBreadCrumbHomeClick());
    };

    handleFetchVersion = (versionId) => {
        this.dispatch(vActions.fetchVersionedSecureDataForPath(this.props.versionPathSelected, versionId, this.props.cerberusAuthToken));
    };

    handleDownloadVersion = (versionId) => {
        this.dispatch(vActions.downloadSecureFileVersion(this.props.versionPathSelected, versionId, this.props.cerberusAuthToken));
    };

    handlePathWithHistoryClick = (path) => {
        this.dispatch(vActions.fetchPathVersionData(path, this.props.cerberusAuthToken, this.props.versionPathPageNumber, this.props.versionPathPerPage));
    };

    render() {
        const {
            cerberusAuthToken,
            hasFetchedPathsWithHistory,
            pathsWithHistory,
            hasFetchedVersionPathData,
            versionPathSelected,
            versionPathData,
            versionPathPerPage,
            versionPathPageNumber,
            versionPathSecureDataMap
        } = this.props;

        const {
            handlePerPageSelect,
            handlePageClick,
            handleBreadCrumbHomeClick,
            handleFetchVersion,
            handleDownloadVersion,
            handlePathWithHistoryClick
        } = this;

        if (!hasFetchedPathsWithHistory) {
            return (
                <div className="secure-data-versions-browser">
                    <Loader />
                </div>
            );
        }

        return (
            <div className="secure-data-versions-browser">
                {!versionPathSelected &&
                    pathsWithHistoryList(pathsWithHistory, cerberusAuthToken, versionPathPageNumber, versionPathPerPage, handlePathWithHistoryClick)
                }

                {versionPathSelected &&
                    pathVersionsBrowser(versionPathSelected, hasFetchedVersionPathData, versionPathData, versionPathPerPage,
                        versionPathPageNumber, versionPathSecureDataMap, handlePerPageSelect, handlePageClick, handleBreadCrumbHomeClick, handleFetchVersion, handleDownloadVersion)
                }
            </div>
        );
    }
}

const pathsWithHistoryList = (pathsWithHistory, cerberusAuthToken, versionPathPageNumber, versionPathPerPage, handlePathWithHistoryClick) => {
    return (
        <div>
            <h3 className="ncss-brand">Paths with version history</h3>
            <div className="paths-with-history">
                {pathsWithHistory && pathsWithHistory.map((path) =>
                    <div key={path}
                        className="path clickable ncss-brand"
                        onClick={() => { handlePathWithHistoryClick(path); }}>{path}</div>
                )}
            </div>
        </div>
    );
};

const pathVersionsBrowser = (versionPathSelected,
    hasFetchedVersionPathData,
    data,
    perPage,
    pageNumber,
    versionPathSecureDataMap,
    handlePerPageSelect,
    handlePageClick,
    handleBreadCrumbHomeClick,
    handleFetchVersion,
    handleDownloadVersion) => {

    if (!hasFetchedVersionPathData) {
        return (<Loader />);
    }

    return (
        <div className="version-list-container">
            <h3 className="ncss-brand">Version Summaries for Path: {versionPathSelected}</h3>
            <div onClick={() => { handleBreadCrumbHomeClick(); }} className="breadcrumb clickable">Back to path list</div>
            {pathVersionsBrowserPaginationMenu(data, perPage, pageNumber, handlePerPageSelect, handlePageClick)}
            {summaries(data['secure_data_version_summaries'], handleFetchVersion, handleDownloadVersion, versionPathSecureDataMap)}
            {pathVersionsBrowserPaginationMenu(data, perPage, pageNumber, handlePerPageSelect, handlePageClick)}
        </div>
    );
};

const summaries = (summaries, handleFetchVersion, handleDownloadVersion, versionPathSecureDataMap) => {
    return (
        <div className="path-version-summaries">
            {summaries.map((summary, index) =>
                generateVersionSummary(summary, index, handleFetchVersion, handleDownloadVersion, versionPathSecureDataMap).map(it => it)
            )}
        </div>
    );
};

const generateVersionSummary = (summary, index, handleFetchVersion, handleDownloadVersion, versionPathSecureDataMap) => {
    if (summary.action === 'DELETE') {
        return [
            <div className="version-summary" key={`${index}-deleted`}>
                <div className="type">Type: {summary['type']}</div>
                <div className="id">Version: <span className="deleted">DELETED</span></div>
                <div className="principal-wrapper">
                    Deleted by <span className="principal">{summary['action_principal']}</span> on <span className="date">{new Date(summary['action_ts']).toLocaleString()}</span>
                </div>
            </div>,
            versionSummary(summary, index, handleFetchVersion, handleDownloadVersion, versionPathSecureDataMap)
        ];
    }
    return [versionSummary(summary, index, handleFetchVersion, handleDownloadVersion, versionPathSecureDataMap)];
};

const versionSummary = (summary, index, handleFetchVersion, handleDownloadVersion, versionPathSecureDataMap) => {
    let versionId = summary.id;
    let dataForVersion = versionPathSecureDataMap.hasOwnProperty(versionId) ? versionPathSecureDataMap[versionId] : false;
    return (
        <div className="version-summary" key={index}>
            <div className="id">Version: <span className={versionId === 'CURRENT' ? 'current' : ''}>{summary.id}</span></div>
            <div className="type">Type: {summary['type']}</div>
            {summary.type === 'FILE' &&
                <div className="size-in-bytes">Size: {(summary['size_in_bytes'] / 1024).toFixed(2)} KB</div>
            }
            <div className="principal-wrapper">
                Created by <span className="principal">{summary['version_created_by']}</span> on <span className="date">{new Date(summary['version_created_ts']).toLocaleString()}</span>
            </div>
            {summary.type === 'FILE' ?
                (versionDownloadButton(handleDownloadVersion, versionId))
                :
                (dataForVersion ? secureDataForVersion(dataForVersion) : fetchVersionButton(handleFetchVersion, versionId))
            }
        </div>
    );
};

const secureDataForVersion = (dataForVersion) => {
    return (
        <div className="secure-data-for-version-wrapper">
            <div className="secure-data-for-version">
                <JSONPretty json={dataForVersion} space="4"></JSONPretty>
            </div>
        </div>
    );
};

const fetchVersionButton = (handleFetchVersion, versionId) => {
    return (
        <div
            className='btn ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
            onClick={() => { handleFetchVersion(versionId); }}
        >Show this version</div>
    );
};

const versionDownloadButton = (handleDownloadVersion, versionId) => {
    return (
        <div
            className='btn ncss-btn-dark-grey ncss-brand pt3-sm pr5-sm pb3-sm pl5-sm pt2-lg pb2-lg u-uppercase'
            onClick={() => { handleDownloadVersion(versionId); }}
        >Download</div>
    );
};

const pathVersionsBrowserPaginationMenu = (pathData, perPage, pageNumber, handlePerPageSelect, handlePageClick) => {

    const options = [
        { value: 5, label: '5' },
        { value: 10, label: '10' },
        { value: 25, label: '25' },
        { value: 50, label: '50' },
        { value: 100, label: '100' }
    ];

    if (pageNumber === 0 && pathData.has_next === false) {
        return (<div></div>);
    }

    return (
        <div className="version-pagination-menu paths-with-history-pagination-menu ncss-brand">
            <ReactPaginate pageCount={Math.ceil(pathData.total_version_count / perPage)}
                pageRangeDisplayed={3}
                marginPagesDisplayed={1}
                previousLabel={"Prev"}
                nextLabel={"Next"}
                onPageChange={handlePageClick}
                forcePage={pageNumber}
                containerClassName={"version-pagination"}
                previousClassName={"version-previous-btn"}
                nextClassName={"version-next-btn"}
                previousLinkClassName={"ncss-btn-black ncss-brand pt2-sm pr5-sm pb2-sm pl5-sm"}
                nextLinkClassName={"ncss-btn-black ncss-brand pt2-sm pr5-sm pb2-sm pl5-sm "}
                pageClassName={"page-btn"}
                breakClassName={"page-btn ncss-btn-light-grey disabled ncss-brand pt2-sm pr5-sm pb2-sm pl5-sm"}
                pageLinkClassName={"ncss-btn-light-grey ncss-brand pt2-sm pr5-sm pb2-sm pl5-sm"}
                activeClassName={"version-active"}
            />
            <Select
                className={'version-pagination-per-page-selector'}
                onChange={handlePerPageSelect}
                value={perPage}
                placeholder="Show Per Page"
                options={options}
                searchable={false}
                clearable={false} />
        </div>
    );
};

const mapStateToProps = state => ({
    // current sdb
    safeDepositBoxId: state.manageSafetyDepositBox.data.id,

    // user info
    cerberusAuthToken: state.auth.cerberusAuthToken,

    // version state
    hasFetchedPathsWithHistory: state.versionHistoryBrowser.hasFetchedPathsWithHistory,
    pathsWithHistory: state.versionHistoryBrowser.pathsWithHistory,

    hasFetchedVersionPathData: state.versionHistoryBrowser.hasFetchedVersionPathData,
    versionPathSelected: state.versionHistoryBrowser.versionPathSelected,
    versionPathData: state.versionHistoryBrowser.versionPathData,
    versionPathPerPage: state.versionHistoryBrowser.versionPathPerPage,
    versionPathPageNumber: state.versionHistoryBrowser.versionPathPageNumber,
    versionPathSecureDataMap: state.versionHistoryBrowser.versionPathSecureDataMap
});

export default connect(mapStateToProps)(SecureDataVersionsBrowser);