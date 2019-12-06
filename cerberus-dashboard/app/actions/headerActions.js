import * as constants from '../constants/actions'

/**
 * Event to dispatch when user mouses over username to trigger context menu to be shown
 */
export function mouseOverUsername() {
    return {
        type: constants.USERNAME_CLICKED
    }
}

/**
 * Event to dispatch when user mouses over username to trigger context menu to be hidden
 */
export function mouseOutUsername() {
    return {
        type: constants.MOUSE_OUT_USERNAME
    }
}