import * as React from "react";
import * as Redux from "redux";
import { connect } from "react-redux";
import * as _ from "underscore";
import Picker, { PickerEntry } from "../../components/Picker";

/*-----------------------------------------------------*
  TODO

  - Appearance
    - boldness
    - scrolling (see https://github.com/palantir/blueprint/pull/1049)

  - Behaviour
    - debouncing
    - order by relevance
    - handle backend errors nicely
    - controlled selection

  - Correctness
    - need to handle non-unique keys across categories
 *-----------------------------------------------------*/

export type SearchResultEntry = PickerEntry & {
  onSelect: () => void;
};

export interface SearchResult {
  entries: SearchResultEntry[];
  loaded: boolean;
}

export interface SearchContext {
  required: (string) => void;
  result: SearchResult;
}

export interface SearchProvider {
  // Bindings to Redux, along with a callback to trigger update if the provider isn't injecting results via Redux store
  (reduxState: any, dispatch: Redux.Dispatch<any>, onResultChange: () => void): SearchContext;
}

interface StateProps {
  state: any;
}

interface DispatchProps {
  dispatch: Redux.Dispatch<any>;
}

interface BasicOwnProps {
  className?: string;
  placeholder?: string;
}

type OwnProps = BasicOwnProps & {
  providers: { [id: string] : SearchProvider };
};

type AllProps = StateProps & DispatchProps & OwnProps;

interface State {
  contexts: { [id: string ] : SearchContext };
  cache: { [ id: string ] : SearchResultEntry[] };
}

class SearchContainer extends React.Component<AllProps, State> {
  constructor(props: AllProps) {
    super(props);
    this.state = {
      contexts: this.bindContexts(props),
      cache: {},
    };
  }

  componentWillReceiveProps(nextProps: AllProps) {
    if (nextProps !== this.props) {
      const contexts = this.bindContexts(nextProps);
      this.setState({
        contexts,
        cache: _.mapObject(contexts, (ctx, name) => this.cachedOrNewEntries(name, ctx)),
      });
    }
  }

  private bindContexts(props: AllProps) {
    return _.mapObject(
      props.providers,
      (p, name) => p(props.state, props.dispatch, () => this.onResultChange(name)),
    );
  }

  private onResultChange(name: string) {
    const ctx = this.state.contexts[name];
    this.setState({
      cache: Object.assign({}, this.state.cache, {
        [name]: this.cachedOrNewEntries(name, ctx),
      }),
    });
  }

  private cachedOrNewEntries(name: string, ctx: SearchContext) {
    return ctx.result.loaded ? ctx.result.entries : (this.state.cache[name] || []);
  }

  render() {
    return (
      <Picker
        className={this.props.className}
        iconName="search"
        defaultEntryIconName="person"
        placeholder={this.props.placeholder}
        entries={_.flatten(_.values(this.state.cache))}
        selectedKey={null}
        onEntrySelect={entry => (entry as any).onSelect()}
        errorDisabled={true}
        onQueryChange={query => _.forEach(this.state.contexts, ctx => ctx.required(query))}
        working={_.any(this.state.contexts, ctx => !ctx.result.loaded)}
      />
    );
  }
}

// We're capturing Redux state and dispatch as props, in order to be able to pass to SearchProviders
const mapStateToProps = (state: any) => ({ state });
export default connect<StateProps, DispatchProps, OwnProps>(mapStateToProps, null)(SearchContainer);
