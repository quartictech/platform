import { Action, combineReducers } from "redux";
import { SagaIterator } from "redux-saga";
import {
  call,
  put,
  race,
  select,
  take,
  takeLatest,
} from "redux-saga/effects";
import { fromJS } from "immutable";
import { Intent } from "@blueprintjs/core";
import * as _ from "underscore";
import * as selectors from "../redux/selectors";
import { toaster } from "../containers/App/toaster";

interface ApiConstants {
  clear: string;
  required: string;
  beganLoading: string;
  loaded: string;
  failedToLoad: string;
}

const constants = (resource: ManagedResource<any>) => <ApiConstants> {
  clear: `API/${resource.shortName}/CLEAR`,
  required: `API/${resource.shortName}/REQUIRED`,
  beganLoading: `API/${resource.shortName}/BEGAN_LOADING`,
  loaded: `API/${resource.shortName}/LOADED`,
  failedToLoad: `API/${resource.shortName}/FAILED_TO_LOAD`,
};

interface ApiAction<T> extends Action {
  args: any[];
  data: T;
}

interface ApiActionCreators<T> {
  clear: () => ApiAction<T>;
  required: (...args: any[]) => ApiAction<T>;
  beganLoading: () => ApiAction<T>;
  loaded: (data: T) => ApiAction<T>;
  failedToLoad: (key: string) => any;
}

export const resourceActions = <T>(resource: ManagedResource<T>) => {
  const c = constants(resource);
  return <ApiActionCreators<T>> {
    clear: () => ({ type: c.clear }),
    required: (...args) => ({ type: c.required, args }),
    beganLoading: () => ({ type: c.beganLoading }),
    loaded: (data) => ({ type: c.loaded, data }),
    failedToLoad: (key: string) => ({ type: c.failedToLoad, key }),
  };
};

const showError = (key, message) => {
  // Avoids duplicates
  if (_.some(toaster.getToasts(), t => t.key === key )) {
    return key;
  }

  return toaster.show({
    iconName: "warning-sign",
    intent: Intent.DANGER,
    message,
  });
};

function* fetch<T>(resource: ManagedResource<T>, action: any): SagaIterator {
  yield put(resourceActions(resource).beganLoading());
  try {
    const result = yield call(resource.endpoint, ...action.args);
    yield put(resourceActions(resource).loaded(result));
  } catch (e) {
    console.warn(e);
    const myState = (yield select(selectors.selectManaged))[resource.shortName].toJS() as ResourceState<T>;
    const toasterKey = showError(myState.toasterKey, `Error loading ${resource.name}.`);
    yield put(resourceActions(resource).failedToLoad(toasterKey));
  }
}

function* fetchAndWatchForClear<T>(resource: ManagedResource<T>, action: any): SagaIterator {
  yield race({
    fetch: call(fetch, resource, action),
    cancel: take(constants(resource).clear),
  });
}

export function* watchAndFetch(resources: ManagedResource<any>[]): SagaIterator {
  for (let i = 0; i < resources.length; i++) {
    yield takeLatest(constants(resources[i]).required, fetchAndWatchForClear, resources[i]);
  }
}

export const enum ResourceStatus {
  NOT_LOADED,
  LOADING,
  ERROR,
  LOADED,
}

export interface ResourceState<T> {
  data: T;
  status: ResourceStatus;
  toasterKey: string;
}

export const singleReducer = <T>(resource: ManagedResource<T>) => (
  state = fromJS(<ResourceState<T>>{
    data: {},
    status: ResourceStatus.NOT_LOADED,
    toasterKey: null,
  }),
  action: any) => {
    switch (action.type) {
      case constants(resource).clear:
        return state
          .set("data", {})
          .set("status", ResourceStatus.NOT_LOADED);

      case constants(resource).beganLoading:
        return state
          .set("data", {})
          .set("status", ResourceStatus.LOADING);

      case constants(resource).failedToLoad:
        return state
          .set("data", {})
          .set("status", ResourceStatus.ERROR)
          .set("toasterKey", action.key);

      case constants(resource).loaded:
        return state
          .set("data", action.data)
          .set("status", ResourceStatus.LOADED);

      default:
        return state;
    }
  };

export const reducer = (resources: ManagedResource<any>[]) =>
  combineReducers(_.object(_.map(resources, r => [r.shortName, singleReducer(r)])) as {});

export const selector = <T>(resource: ManagedResource<T>) =>
  (state) => state.get("managed")[resource.shortName].toJS() as ResourceState<T>;

export class ManagedResource<T> {
  name: string;
  shortName: string;
  endpoint: (...args: any[]) => Promise<T>;
}
