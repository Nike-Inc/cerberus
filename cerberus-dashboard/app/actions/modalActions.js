import * as actions from '../constants/actions'
import { getLogger } from 'logger'
var log = getLogger('modal')


export function pushModal(modalComponent) {
    return {
        type: actions.PUSH_MODAL,
        payload: {
            modalComponent: modalComponent
        }
    }
}

export function popModal() {
    return {
        type: actions.POP_MODAL
    }
}

export function clearAllModals() {
    return {
        type: actions.CLEAR_ALL_MODALS
    }
}