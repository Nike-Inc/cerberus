import rootReducer from '../reducers/rootReducer'
import { applyMiddleware, createStore, compose } from 'redux'
import { routerMiddleware } from 'react-router-redux'
import { browserHistory } from 'react-router'
import thunk from 'redux-thunk'
import createLogger from 'redux-logger'

export default function configureStore() {

    // Apply the middleware to the store
    const middleware = routerMiddleware(browserHistory)

    let store
    if (localStorage.getItem('redux-logger-enabled') == 'true') {
        const logger = createLogger()
        store = createStore(
            rootReducer,
            compose(
                applyMiddleware(middleware, thunk, logger),
                window.devToolsExtension ? window.devToolsExtension() : f => f
            )
        )
    } else {
        store = createStore(
            rootReducer,
            compose(
                applyMiddleware(middleware, thunk),
                window.devToolsExtension ? window.devToolsExtension() : f => f
            )
        )
    }

    if (module.hot) {
        module.hot
            .accept('../reducers/rootReducer', () => {
                const nextRootReducer = require('../reducers/rootReducer')
                store.replaceReducer(nextRootReducer)
            })
    }

    return store
}
