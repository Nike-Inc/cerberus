import { createReducer } from '../utils'
import * as action from '../constants/actions'
import { getLogger } from 'logger'
var log = getLogger('manage-sdb')

const initialState = {
    hasFetchedSDBData: false,
    hasFetchedKeys: false,
    data: {},
    navigatedPath: null,
    vaultPathKeys: [],
    vaultSecretsData: {},
    displayPermissions: false,
    showAddSecretForm: false,
    isEditSubmitting: false
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
    [action.FETCHED_VAULT_KEYS]: (state, payload) => {
        return Object.assign({}, state, {
            vaultPathKeys: payload,
            hasFetchedKeys: true
        })
    },
    [action.ADD_VAULT_KEY_IF_NOT_PRESET]: (state, payload) => {
        let existingList = state.vaultPathKeys
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
            vaultPathKeys: newList
        })
    },
    [action.REMOVE_VAULT_KEY_FROM_LOCAL_STORE]: (state, payload) => {
        let existingList = state.vaultPathKeys
        let newList = []
        let keyToRemove = payload

        for (let key in existingList) {
            let value = existingList[key]
            if (keyToRemove != value) {
                newList.push(value)
            }
        }

        return Object.assign({}, state, {
            vaultPathKeys: newList
        })
    },
    [action.FETCHING_VAULT_KEYS]: (state) => {
        return Object.assign({}, state, {
            hasFetchedKeys: false
        })
    },
    [action.UPDATE_NAVIGATED_PATH]: (state, payload) => {
        return Object.assign({}, state, {
            navigatedPath: payload,
            hasFetchedKeys: false,
            vaultPathKeys: []
        })
    },
    [action.FETCHING_VAULT_SECRET]: (state, payload) => {
        let existingMap = state.vaultSecretsData
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
            vaultSecretsData: newMap
        })
    },
    [action.FETCHED_VAULT_SECRET]: (state, payload) => {
        let existingMap = state.vaultSecretsData
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
            vaultSecretsData: newMap
        })
    },
    [action.REMOVE_SECRET_FROM_LOCAL_STORE]: (state, payload) => {
        let existingMap = state.vaultSecretsData
        let newMap = new Map()

        for (let key in existingMap) {
            if (existingMap.hasOwnProperty(key) && key != payload) {
                newMap[key] = existingMap[key]
            }
        }

        return Object.assign({}, state, {
            vaultSecretsData: newMap
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
    [action.CLEAR_VAULT_DATA]: (state) => {
        return Object.assign({}, state, {
            vaultPathKeys: [],
            vaultSecretsData: {}
        })
    },
    [action.SAVING_VAULT_SECRET]: (state, payload) => {
        let existingMap = state.vaultSecretsData
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
            vaultSecretsData: newMap
        })
    },

    [action.RESET_SDB_DATA]: () => {
        return initialState
    }
})
