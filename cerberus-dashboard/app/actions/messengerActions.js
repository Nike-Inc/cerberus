import * as constants from '../constants/actions'

/**
 * adds a message to the messenger
 * @param message A string or JSX element to be rendered in the messenger component
 */
export function addNewMessage(message, id) {
    window.scrollTo(0, 0)
    return {
        type: constants.ADD_MESSAGE,
        payload: {
            message: message,
            id: id ? id : guid()
        }
    }
}

export function addNewMessageWithTimeout(message, timeout) {
    return (dispatch => {
        let id = guid()

        dispatch(addNewMessage(message, id))
        setTimeout(() => {
            dispatch(removeMessage(id))
        }, timeout)
    })
}

export function removeMessage(id) {
    return {
        type: constants.REMOVE_MESSAGE,
        payload: id
    }
}

export function clearAllMessages() {
    return {
        type: constants.CLEAR_ALL_MESSAGES
    }
}

function guid() {
    function s4() {
        return Math.floor((1 + Math.random()) * 0x10000)
          .toString(16)
          .substring(1);
    }
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
      s4() + '-' + s4() + s4() + s4();
}