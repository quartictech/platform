import { combineReducers } from 'redux';
import { routerReducer } from 'react-router-redux';
//import { nodesReducer } from './modules/nodes';
//import { routesReducer } from './modules/routes';

// TODO: Fix type!
const rootReducer: Redux.Reducer<any> = combineReducers({
  routing: routerReducer,
});

export { rootReducer }
