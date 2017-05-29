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
}

export interface SearchProvider {
  (reduxState: any, dispatch: Redux.Dispatch<any>, onResultChange: (SearchResult) => void): SearchContext;
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
}

type AllProps = StateProps & DispatchProps & OwnProps;

interface State {
  contexts: { [id: string ] : SearchContext };
  cache: { [ id: string ] : SearchResult };
}

class SearchContainer extends React.Component<AllProps, State> {
  constructor(props: AllProps) {
    super(props);
    this.state = {
      contexts: this.bindContexts(props),
      cache: {}
    };
  }

  // TODO - rebuild cache if list of datasets changes
  componentWillReceiveProps(nextProps: AllProps) {
    if (nextProps !== this.props) {
      this.setState({ contexts: this.bindContexts(nextProps) });
    }
  }

  private bindContexts(props: AllProps) {
    return _.mapObject(
      props.providers,
      (p, name) => p(props.state, props.dispatch, (result) => this.onResultChange(name, result))
    );
  }

  private onResultChange(name: string, result: SearchResult) {
    this.setState({
      cache: Object.assign({}, this.state.cache, {
        [name]: result.loaded ? result : { loaded: false, entries: this.state.cache[name].entries } as SearchResult
      }),
    });
  }

  render() {
    return (
      <Picker
        className={this.props.className}
        iconName="search"
        defaultEntryIconName="person"
        placeholder={this.props.placeholder}
        entries={_.flatten(_.map(_.values(this.state.cache), result => result.entries))}
        selectedKey={null}
        onEntrySelect={entry => (entry as any).onSelect()}
        errorDisabled={true}
        onQueryChange={query => _.forEach(this.state.contexts, ctx => ctx.required(query))}
        working={_.any(this.state.cache, result => !result.loaded)}
      />
    );
  }
}

// We're capturing Redux state and dispatch as props, in order to be able to pass to SearchProviders
const mapStateToProps = (state: any) => ({ state });
export default connect<StateProps, DispatchProps, OwnProps>(mapStateToProps, null)(SearchContainer);
