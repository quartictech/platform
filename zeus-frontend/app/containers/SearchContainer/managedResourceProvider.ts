import * as _ from "underscore";
import { SearchResultEntry } from "./index";
import {
  ManagedResource,
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";

export const managedResourceProvider = <T>(
  selector: (state: any) => ResourceState<{ [id: string] : T }>,
  resource: ManagedResource<{ [id: string] : T }>,
  mapper: (id: string, item: T) => SearchResultEntry,
) => (reduxState: any, dispatch: Redux.Dispatch<any>, _onResultChange: () => void) => {
  const resourceState = selector(reduxState);
  return {
    required: (query: string) => (dispatch((query.length > 0)
      ? resourceActions(resource).required(query, 5)
      : resourceActions(resource).clear())),
    result: {
      entries: _.map(resourceState.data, (item, id) => mapper(id, item)),
      loaded: resourceState.status !== ResourceStatus.LOADING,
    },
  };
};
