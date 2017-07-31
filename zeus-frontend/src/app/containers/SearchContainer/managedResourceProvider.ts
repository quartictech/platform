import * as _ from "underscore";
import { SearchResultEntry } from "./index";
import {
  Dataset,
} from "../../models";
import {
  ManagedResource,
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";

export const managedResourceProvider = <T>(
  selector: (state: any) => ResourceState<Dataset<T>>,
  resource: ManagedResource<Dataset<T>>,
  mapper: (id: string, item: T) => SearchResultEntry,
) => (reduxState: any, dispatch: Redux.Dispatch<any>) => {
  const resourceState = selector(reduxState);
  return {
    required: (query: string) => (dispatch((query.length > 0)
      ? resourceActions(resource).requiredFresh(query, 5)
      : resourceActions(resource).clear())),
    result: {
      entries: _.map(resourceState.data.content, (item, id) => mapper(id, item)),
      loaded: resourceState.status !== ResourceStatus.LOADING,
    },
  };
};
