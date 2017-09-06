import { SagaIterator } from "redux-saga";

import { manageDatasets } from "./datasets";
import { managePipeline } from "./pipeline";
import { manageFeed } from "./feed";
import { manageUser } from "./user";

export function* sagas(): SagaIterator {
  yield* manageUser();
  yield* manageDatasets();
  yield* managePipeline();
  yield* manageFeed();
}
