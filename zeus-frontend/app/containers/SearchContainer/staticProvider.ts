import * as _ from "underscore";
import { SearchResultEntry } from "./index";
import { stringInString } from "../../helpers/Utils";

export const staticProviderEngine = () => {
  let myQuery = ""; // State
  return (entries: SearchResultEntry[], onResultChange: () => void) => ({
    required: (query: string) => {
      myQuery = query;
      onResultChange();
    },
    result: {
      entries: (myQuery.length > 0) ? _.filter(entries, e => stringInString(myQuery, e.name)) : [],
      loaded: true,
    },
  });
};

export const staticProvider = (entries: SearchResultEntry[]) => {
  const engine = staticProviderEngine();  // Create upfront so that its state isn't lost every time Redux updates
  return (_reduxState, _dispatch, onResultChange: () => void) => {
    return engine(entries, onResultChange);
  };
};