import React from 'react'
import { Component } from 'react'
import PropTypes from 'prop-types'
import log from 'logger'

/**
 * Dumb component for the Bucket Category and its associated buckets
 *
 * @param handleMouseClickAddNewBucket The callback for when a user selects the plus button
 * @param handleSDBClicked The callback for when a user clicks a SDB
 * @param id The category id
 * @param name the name of the category
 * @param boxes Safe Deposit Boxes for a category

 */
export default class Category extends Component {
    static propTypes = {
        handleSDBClicked: PropTypes.func.isRequired,
        handleMouseClickAddNewBucket: PropTypes.func.isRequired,
        id: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired,
        boxes: PropTypes.array.isRequired
    }

    render() {
        const {handleMouseClickAddNewBucket, handleSDBClicked, id, name, boxes} = this.props
        var bucketComponents = []
        for (var box of boxes) {
            bucketComponents.push(
                <div className='border-bottom-light-grey bucket-button'
                     key={box.id}
                     onClick={handleSDBClicked.bind(this, box.id, box.path)} >
                    <h3 className='ncss-brand'>{box.name}</h3>
                </div>
            )
        }

        return (
            <div>
                <div className='category' key={id}>
                    <div className='category-label ncss-brand h2'>{name}</div>
                    <div className='category-buttons'>
                        <div className='category-button ncss-glyph-plus-lg'
                             onClick={handleMouseClickAddNewBucket.bind(this, id)}>
                        </div>
                    </div>
                </div>
                {bucketComponents}
            </div>
        )
    }
}