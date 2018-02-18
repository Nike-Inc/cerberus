import { combineReducers } from 'redux'
import { routerReducer } from 'react-router-redux'
import { reducer as formReducer } from 'redux-form'
import auth from './authenticationReducer'
import header from './headerStateReducer'
import app from './appReducer'
import nSDB from './createSDBoxReducer'
import messenger from './messengerReducer'
import manageSafetyDepositBox from './manageSafetyDepositBoxReducer'
import metadata from './metadataReducer'
import modal from './modalReducer'
import versionHistoryBrowser from './versionHistoryBrowserReducer'

export default combineReducers({
    auth,
    header,
    app,
    nSDB,
    manageSafetyDepositBox,
    modal,
    routing: routerReducer,
    form: formReducer,
    messenger,
    metadata,
    versionHistoryBrowser
})