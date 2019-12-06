import * as constants from '../constants/actions'

/**
 * Event to dispatch when user mouses over username to trigger context menu to be shown
 */
export function newDropDownCreated(key, value) {
    return {
        type: constants.NEW_DROP_DOWN_CREATED,
        payload: {
            key: key,
            value: value
        }
    }
}

export function selectedItemClicked(key) {
    return {
        type: constants.SELECTED_ITEM_CLICKED,
        payload: {
            key: key
        }
    }
}