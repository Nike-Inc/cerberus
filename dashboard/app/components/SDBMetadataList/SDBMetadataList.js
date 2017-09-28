import React from 'react'
import { Component } from 'react'
import { connect } from 'react-redux'
import * as metadataActions from '../../actions/metadataActions'
import ReactPaginate from 'react-paginate'
import Select from 'react-select'
import SDBMetadata from '../SDBMetadata/SDBMetadata'

import './SDBMetadataList.scss'

@connect((state) => {
    return {
        vaultToken: state.auth.vaultToken,
        metadata: state.metadata.metadata,
        perPage: state.metadata.perPage,
        pageNumber: state.metadata.pageNumber
    }
})

export default class SDBMetadataList extends Component {

    options = [
        { value: 10, label: '10' },
        { value: 50, label: '50' },
        { value: 100, label: '100' },
        { value: 1000, label: '1000' }
    ]

    componentDidMount() {
        this.props.dispatch(metadataActions.fetchMetadata(this.props.vaultToken, this.props.pageNumber, this.props.perPage))
    }

    handlePageClick = (data) => {
        let pageNumber = data.selected;

        this.props.dispatch(metadataActions.updatePageNumber(pageNumber));
        this.props.dispatch(metadataActions.fetchMetadata(this.props.vaultToken, pageNumber, this.props.perPage));
    };

    handlePerPageSelect = (selected) => {
        let perPage = selected.value;
        let pageNumber = 0; // default back to the first page

        this.props.dispatch(metadataActions.updatePerPage(perPage));
        this.props.dispatch(metadataActions.updatePageNumber(pageNumber))
        this.props.dispatch(metadataActions.fetchMetadata(this.props.vaultToken, pageNumber, perPage));
    };

    render() {
        const {metadata, perPage} = this.props

        if (metadata['safe_deposit_box_metadata'] == undefined) {
            return(
                <div>
                    NO METADATA
                </div>
            )
        }

        return (
            <div className="metadata-list-container">
                <div className="ncss h3">SDB Metadata</div>
                <div className="ncss h4">Total SDBs: {metadata.total_sdbcount}</div>
                { paginationMenu(metadata, this.options, perPage, this.props.pageNumber, this.handlePerPageSelect, this.handlePageClick) }
                <div className="matadata-listings">
                    {metadata['safe_deposit_box_metadata'].map((sdb, index) =>
                        <SDBMetadata sdbMetadata={sdb}
                                     key={index}/>
                    )}
                </div>
                { paginationMenu(metadata, this.options, perPage, this.props.pageNumber, this.handlePerPageSelect, this.handlePageClick) }
            </div>
        )
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
            onChange = { handlePerPageSelect }
            value={ perPage }
            placeholder="Show Per Page"
            options={options}
            searchable={false}
            clearable={false} />
      </div>
    )
}

