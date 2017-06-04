import { Action, combineReducers } from "redux";
import { SagaIterator } from "redux-saga";
import {
  call,
  cancel,
  fork,
  put,
  race,
  select,
  take,
} from "redux-saga/effects";
import { fromJS } from "immutable";
import { Intent } from "@blueprintjs/core";
import * as _ from "underscore";
import { toaster } from "../containers/App/toaster";

interface ApiConstants {
  clear: string;
  required: string;
  requiredFresh: string;
  beganLoading: string;
  loaded: string;
  failedToLoad: string;
}

const constants = (resource: ManagedResource<any>) => <ApiConstants> {
  clear: `API/${resource.shortName}/CLEAR`,
  required: `API/${resource.shortName}/REQUIRED`,
  requiredFresh: `API/${resource.shortName}/REQUIRED_FRESH`,
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
  requiredFresh: (...args: any[]) => ApiAction<T>;
  beganLoading: () => ApiAction<T>;
  loaded: (data: T) => ApiAction<T>;
  failedToLoad: (key: string) => any;
}

export const resourceActions = <T>(resource: ManagedResource<T>) => {
  const c = constants(resource);
  return <ApiActionCreators<T>> {
    clear: () => ({ type: c.clear }),
    required: (...args) => ({ type: c.required, args }),
    requiredFresh: (...args) => ({ type: c.requiredFresh, args }),
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
    const myState = yield select(selector(resource));
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

function* shouldElide<T>(resource: ManagedResource<T>, action: any): SagaIterator {
    if (action.type === constants(resource).required) {
      const myState = (yield select(selector(resource))) as ResourceState<T>;
      if (myState.status !== ResourceStatus.NOT_LOADED) {
        return true;
      }
    }
    return false;
}

export function* watchAndFetch(resources: ManagedResource<any>[]): SagaIterator {
  for (let i = 0; i < resources.length; i++) {
    const c = constants(resources[i]);

    // Modified version of takeLatest (see https://redux-saga.js.org/docs/advanced/Concurrency.html)
    yield fork(function* () {
      let lastTask;
      while (true) {
        const action = yield take([c.required, c.requiredFresh]);
        const skip = yield call(shouldElide, resources[i], action);
        if (!skip) {
          if (lastTask) {
            yield cancel(lastTask);
          }
          lastTask = yield fork(fetchAndWatchForClear, resources[i], action);
        }
      }
    });
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

export const ifLoaded = <T, R>(resource: ResourceState<T>, func: (T) => R, defaultValue: R) =>
  ((resource.status === ResourceStatus.LOADED) ? func(resource.data) : defaultValue);

export class ManagedResource<T> {
  name: string;
  shortName: string;
  endpoint: (...args: any[]) => Promise<T>;
}
