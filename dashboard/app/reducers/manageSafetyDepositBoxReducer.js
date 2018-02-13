import { createReducer } from '../utils'
import * as action from '../constants/actions'
import { getLogger } from 'logger'
var log = getLogger('manage-sdb')

const initialState = {
    hasFetchedSDBData: false,
    hasFetchedKeys: false,
    data: {},
    navigatedPath: null,
    keysForSecureDataPath: [],
    secureData: {},
    displayPermissions: false,
    showAddSecretForm: false,
    isEditSubmitting: false,
    nav: {
        secureDataSelected: true,
        secureDataVersionsSelected: false,
        sdbSettingsSelected: false
    }
}

export default createReducer(initialState, {
    [action.STORE_SDB_DATA]: (state, payload) => {
        return Object.assign({}, state, {
            data: payload,
            hasFetchedSDBData: true,
            navigatedPath: payload.path
        })
    },
    [action.TOGGLE_PERM_VIS]: (state) => {
        return Object.assign({}, state, {
            displayPermissions: ! state.displayPermissions
        })
    },
    [action.FETCHED_SECURE_DATA_KEYS]: (state, payload) => {
        return Object.assign({}, state, {
            keysForSecureDataPath: payload,
            hasFetchedKeys: true
        })
    },
    [action.ADD_SECURE_DATA_KEY_IF_NOT_PRESET]: (state, payload) => {
        let existingList = state.keysForSecureDataPath
        let keyToAddIfMissing = payload
        let newList = []
        let isKeyPreset = false

        for (let key in existingList) {
            let value = existingList[key]
            if (value == keyToAddIfMissing) {
                isKeyPreset = true
            }
            newList.push(value)
        }

        if (! isKeyPreset) {
            newList.push(keyToAddIfMissing)
        }

        return Object.assign({}, state, {
            keysForSecureDataPath: newList
        })
    },
    [action.REMOVE_KEY_FOR_SECURE_DATA_FROM_LOCAL_STORE]: (state, payload) => {
        let existingList = state.keysForSecureDataPath
        let newList = []
        let keyToRemove = payload

        for (let key in existingList) {
            let value = existingList[key]
            if (keyToRemove != value) {
                newList.push(value)
            }
        }

        return Object.assign({}, state, {
            keysForSecureDataPath: newList
        })
    },
    [action.FETCHING_SECURE_DATA_KEYS]: (state) => {
        return Object.assign({}, state, {
            hasFetchedKeys: false
        })
    },
    [action.UPDATE_NAVIGATED_PATH]: (state, payload) => {
        return Object.assign({}, state, {
            navigatedPath: payload,
            hasFetchedKeys: false,
            keysForSecureDataPath: []
        })
    },
    [action.FETCHING_SECURE_DATA]: (state, payload) => {
        let existingMap = state.secureData
        let newMap = {}
        let fetchingKey = payload

        for (let key in existingMap) {
            if (existingMap.hasOwnProperty(key)) {
                newMap[key] = existingMap[key]
            }
        }
        newMap[fetchingKey] = {
            isFetching: true,
            isUpdating: false,
            isActive: true,
            data: {}
        }

        return Object.assign({}, state, {
            secureData: newMap
        })
    },
    [action.FETCHED_SECURE_DATA]: (state, payload) => {
        let existingMap = state.secureData
        let newMap = {}
        let fetchedKey = payload.key

        for (let key in existingMap) {
            if (existingMap.hasOwnProperty(key)) {
                newMap[key] = existingMap[key]
            }
        }

        newMap[fetchedKey] = {
            isFetching: false,
            isUpdating: false,
            isActive: true,
            data: payload.data
        }

        return Object.assign({}, state, {
            secureData: newMap
        })
    },
    [action.REMOVE_SECRET_FROM_LOCAL_STORE]: (state, payload) => {
        let existingMap = state.secureData
        let newMap = new Map()

        for (let key in existingMap) {
            if (existingMap.hasOwnProperty(key) && key != payload) {
                newMap[key] = existingMap[key]
            }
        }

        return Object.assign({}, state, {
            secureData: newMap
        })
    },
    [action.SHOW_ADD_SECRET_FORM]: (state) => {
        return Object.assign({}, state, {
            showAddSecretForm: true
        })
    },
    [action.HIDE_ADD_SECRET_FORM]: (state) => {
        return Object.assign({}, state, {
            showAddSecretForm: false
        })
    },
    [action.SUBMITTING_EDIT_SDB_REQUEST]: (state) => {
        return Object.assign({}, state, {
            isEditSubmitting: true
        })
    },
    [action.RESET_SUBMITTING_EDIT_SDB_REQUEST]: (state) => {
        return Object.assign({}, state, {
            isEditSubmitting: false
        })
    },
    [action.CLEAR_SECURE_DATA]: (state) => {
        return Object.assign({}, state, {
            keysForSecureDataPath: [],
            secureData: {}
        })
    },
    [action.SAVING_SECURE_DATA]: (state, payload) => {
        let existingMap = state.secureData
        let newMap = {}
        let fetchingKey = payload

        for (let key in existingMap) {
            if (existingMap.hasOwnProperty(key)) {
                newMap[key] = existingMap[key]
            }
        }
        newMap[fetchingKey] = {
            isFetching: false,
            isUpdating: true,
            isActive: true,
            data: existingMap[fetchingKey] ? existingMap[fetchingKey]['data'] : {}
        }

        return Object.assign({}, state, {
            secureData: newMap
        })
    },
    [action.RESET_SDB_DATA]: () => {
        return initialState
    },
    [action.SDB_NAV_ITEM_SELECT]: (state, payload) => {
        let navMap = {
            secureDataSelected: false,
            secureDataVersionsSelected: false,
            sdbSettingsSelected: false
        }
        navMap[payload+'Selected'] = true
        return Object.assign({}, state, {
            nav: navMap
        })
    }
})
