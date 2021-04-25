/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import rootReducer from '../reducers/rootReducer';
import { createBrowserHistory, createHashHistory } from 'history'
import { applyMiddleware, createStore, compose } from 'redux';
import { routerMiddleware } from 'connected-react-router'
import thunk from 'redux-thunk';
import createLogger from 'redux-logger';

// export const history = createHashHistory()
export const history = createBrowserHistory();

export default function configureStore() {

    // Apply the middleware to the store
    const middleware = routerMiddleware(history);

    let store;
    if (localStorage.getItem('redux-logger-enabled') === 'true') {
        const logger = createLogger();
        store = createStore(
            rootReducer(history),
            compose(
                applyMiddleware(middleware, thunk, logger),
                window.devToolsExtension ? window.__REDUX_DEVTOOLS_EXTENSION__() : f => f
            )
        );
    } else {
        store = createStore(
            rootReducer(history),
            compose(
                applyMiddleware(middleware, thunk),
                window.devToolsExtension ? window.__REDUX_DEVTOOLS_EXTENSION__() : f => f
            )
        );
    }

    if (module.hot) {
        module.hot
            .accept('../reducers/rootReducer', () => {
                const nextRootReducer = require('../reducers/rootReducer');
                store.replaceReducer(nextRootReducer);
            });
    }

    return store;
}
