import { Action, combineReducers } from "redux";
import { SagaIterator } from "redux-saga";
import {
  call,
  put,
  race,
  take,
  takeLatest,
} from "redux-saga/effects";
import { fromJS } from "immutable";
import { Intent } from "@blueprintjs/core";
import * as _ from "underscore";
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
  failedToLoad: () => ApiAction<T>;
}

export const resourceActions = <T>(resource: ManagedResource<T>) => {
  const c = constants(resource);
  return <ApiActionCreators<T>> {
    clear: () => ({ type: c.clear }),
    required: (...args) => ({ type: c.required, args }),
    beganLoading: () => ({ type: c.beganLoading }),
    loaded: (data) => ({ type: c.loaded, data }),
    failedToLoad: () => ({ type: c.failedToLoad }),
  };
};

const showError = (message) => toaster.show({
  iconName: "warning-sign",
  intent: Intent.DANGER,
  message
});

function* fetch<T>(resource: ManagedResource<T>, action: any): SagaIterator {
  yield put(resourceActions(resource).beganLoading());
  try {
    const result = yield call(resource.endpoint, ...action.args);
    yield put(resourceActions(resource).loaded(result));
  } catch (e) {
    console.warn(e);
    showError(`Error loading ${resource.name}.`);
    yield put(resourceActions(resource).failedToLoad());
  }
}

function* fetchAndWatchForClear<T>(resource: ManagedResource<T>, action: any): SagaIterator {
  yield race({
    fetch: call(fetch, resource, action),
    cancel: take(constants(resource).clear)
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
}

export const singleReducer = <T>(resource: ManagedResource<T>) => (
  state = fromJS(<ResourceState<T>>{
    data: {},
    status: ResourceStatus.NOT_LOADED,
  }),
  action: any) => {
    switch (action.type) {
      case constants(resource).clear:
        return fromJS(<ResourceState<T>>{
          data: {},
          status: ResourceStatus.NOT_LOADED,
        });

      case constants(resource).beganLoading:
        return fromJS(<ResourceState<T>>{
          data: {},
          status: ResourceStatus.LOADING,
        });

      case constants(resource).failedToLoad:
        return fromJS(<ResourceState<T>>{
          data: {},
          status: ResourceStatus.ERROR,
        });

      case constants(resource).loaded:
        return fromJS(<ResourceState<T>>{
          data: action.data,
          status: ResourceStatus.LOADED,
        });

      default:
        return state;
    }
  };

export const reducer = (resources: ManagedResource<any>[]) =>
  combineReducers(_.object(_.map(resources, r => [r.shortName, singleReducer(r)])) as {});

export interface ManagedResource<T> {
  name: string;
  shortName: string;
  endpoint: (...args: any[]) => Promise<T>;
}
