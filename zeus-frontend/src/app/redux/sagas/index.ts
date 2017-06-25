import { SagaIterator } from "redux-saga";
import { watchAndFetch } from "../../api-management";
import * as api from "../../api";

export function* sagas(): SagaIterator {
  yield *watchAndFetch(api.managedResources);
}
