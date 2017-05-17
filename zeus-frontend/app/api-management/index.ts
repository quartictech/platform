import { Action } from "redux";
import { SagaIterator } from "redux-saga";
import {
  call,
  put,
  takeLatest,
} from "redux-saga/effects";
import { fromJS } from "immutable";
import { Intent } from "@blueprintjs/core";
import { toaster } from "../containers/App/toaster";

interface ApiConstants {
  required: string;
  beganLoading: string;
  loaded: string;
  failedToLoad: string;
}

const constants = (resource: ManagedResource<any>) => <ApiConstants> {
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
  required: (...args: any[]) => ApiAction<T>;
  beganLoading: () => ApiAction<T>;
  loaded: (data: T) => ApiAction<T>;
  failedToLoad: () => ApiAction<T>;
}

export const resourceActions = <T>(resource: ManagedResource<T>) => {
  const c = constants(resource);
  return <ApiActionCreators<T>> {
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

export function* watchAndFetch<T>(resource: ManagedResource<T>): SagaIterator {
    yield takeLatest(constants(resource).required, fetch, resource);
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

export const reducer = <T>(resource: ManagedResource<T>) => (
  state = fromJS(<ResourceState<T>>{
    data: {},
    status: ResourceStatus.NOT_LOADED,
  }),
  action: any) => {
    switch (action.type) {
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

export interface ManagedResource<T> {
  name: string;
  shortName: string;
  endpoint: (...args: any[]) => Promise<T>;
}
