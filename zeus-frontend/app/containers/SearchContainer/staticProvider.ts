import * as _ from "underscore";
import { SearchResultEntry } from "./index";
import { stringInString } from "../../helpers/Utils";

const MIN_ALIAS_MATCH_LENGTH = 3;

const matchesGloablAlias = (query: string, globalAliases: string[]) =>
  (query.length >= MIN_ALIAS_MATCH_LENGTH) && _.any(globalAliases, alias => stringInString(query, alias));

export const staticProviderEngine = (globalAliases: string[]) => {
  let myQuery = ""; // State
  return (entries: SearchResultEntry[], onResultChange: () => void) => ({
    required: (query: string) => {
      myQuery = query;
      onResultChange();
    },
    result: {
      entries: matchesGloablAlias(myQuery, globalAliases)
        ? entries
        : ((myQuery.length > 0) ? _.filter(entries, e => stringInString(myQuery, e.name)) : []),
      loaded: true,
    },
  });
};

export const staticProvider = (globalAliases: string[], entries: SearchResultEntry[]) => {
  const engine = staticProviderEngine(globalAliases);  // Create upfront so that its state isn't lost every time Redux updates
  return (_reduxState, _dispatch, onResultChange: () => void) => {
    return engine(entries, onResultChange);
  };
};