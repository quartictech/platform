import { SagaIterator } from "redux-saga";

import { manageDatasets } from "./datasets";
import { managePipeline } from "./pipeline";
import { manageBuilds } from "./builds";
import { manageUser } from "./user";

export function* sagas(): SagaIterator {
  yield* manageUser();
  yield* manageDatasets();
  yield* managePipeline();
  yield* manageBuilds();
}
