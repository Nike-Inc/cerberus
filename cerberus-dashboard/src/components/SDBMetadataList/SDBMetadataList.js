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
import * as metadataActions from '../../actions/metadataActions';
import ReactPaginate from 'react-paginate';
import Select from 'react-select';
import SDBMetadata from '../SDBMetadata/SDBMetadata';

import './SDBMetadataList.scss';

class SDBMetadataList extends Component {

    options = [
        { value: 10, label: '10' },
        { value: 50, label: '50' },
        { value: 100, label: '100' },
        { value: 1000, label: '1000' }
    ];

    componentDidMount() {
        this.props.dispatch(metadataActions.fetchMetadata(this.props.cerberusAuthToken, this.props.pageNumber, this.props.perPage));
    }

    handlePageClick = (data) => {
        let pageNumber = data.selected;

        this.props.dispatch(metadataActions.updatePageNumber(pageNumber));
        this.props.dispatch(metadataActions.fetchMetadata(this.props.cerberusAuthToken, pageNumber, this.props.perPage));
    };

    handlePerPageSelect = (selected) => {
        let perPage = selected.value;
        let pageNumber = 0; // default back to the first page

        this.props.dispatch(metadataActions.updatePerPage(perPage));
        this.props.dispatch(metadataActions.updatePageNumber(pageNumber));
        this.props.dispatch(metadataActions.fetchMetadata(this.props.cerberusAuthToken, pageNumber, perPage));
    };

    render() {
        const { metadata, perPage } = this.props;

        if (metadata['safe_deposit_box_metadata'] === undefined) {
            return (
                <div>
                    NO METADATA
                </div>
            );
        }

        return (
            <div className="metadata-list-container">
                <div className="ncss h3">SDB Metadata</div>
                <div className="ncss h4">Total SDBs: {metadata.total_sdbcount}</div>
                {paginationMenu(metadata, this.options, perPage, this.props.pageNumber, this.handlePerPageSelect, this.handlePageClick)}
                <div className="metadata-listings">
                    {metadata['safe_deposit_box_metadata'].map((sdb, index) =>
                        <SDBMetadata sdbMetadata={sdb}
                            key={index} />
                    )}
                </div>
                {paginationMenu(metadata, this.options, perPage, this.props.pageNumber, this.handlePerPageSelect, this.handlePageClick)}
            </div>
        );
    }
}

const paginationMenu = (metadata, options, perPage, pageNumber, handlePerPageSelect, handlePageClick) => {
    return (
        <div className="metadata-pagination-menu ncss-brand">
            <ReactPaginate pageCount={Math.ceil(metadata.total_sdbcount / perPage)}
                pageRangeDisplayed={3}
                marginPagesDisplayed={1}
                previousLabel={"Prev"}
                nextLabel={"Next"}
                onPageChange={handlePageClick}
                forcePage={pageNumber}
                containerClassName={"metadata-pagination"}
                previousClassName={"metadata-previous-btn"}
                nextClassName={"metadata-next-btn"}
                previousLinkClassName={"ncss-btn-black ncss-brand pt2-sm pr5-sm pb2-sm pl5-sm"}
                nextLinkClassName={"ncss-btn-black ncss-brand pt2-sm pr5-sm pb2-sm pl5-sm "}
                pageClassName={"page-btn"}
                breakClassName={"page-btn ncss-btn-light-grey disabled ncss-brand pt2-sm pr5-sm pb2-sm pl5-sm"}
                pageLinkClassName={"ncss-btn-light-grey ncss-brand pt2-sm pr5-sm pb2-sm pl5-sm"}
                activeClassName={"metadata-active"}
            />
            <Select
                className={'metadata-pagination-per-page-selector'}
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
    cerberusAuthToken: state.auth.cerberusAuthToken,
    metadata: state.metadata.metadata,
    perPage: state.metadata.perPage,
    pageNumber: state.metadata.pageNumber
});

export default connect(mapStateToProps)(SDBMetadataList);