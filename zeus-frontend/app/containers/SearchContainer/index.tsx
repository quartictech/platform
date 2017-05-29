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

export interface SearchContext {
  required: (string) => void;
  results: SearchResultEntry[],
  loaded: boolean;  
}

export interface SearchProvider {
  (reduxState: any, dispatch: Redux.Dispatch<any>): SearchContext;
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

type MergedProps = BasicOwnProps & {
  contexts: { [id: string] : SearchContext };
}

interface State {
  cache: { [ id: string ] : SearchResultEntry[] };
}

class SearchContainer extends React.Component<MergedProps, State> {
  constructor(props: MergedProps) {
    super(props);
    console.log(props);
    this.state = { cache: {} };
  }

  // TODO - optimise for no change in data
  componentWillReceiveProps(nextProps: MergedProps) {
    const cache = _.mapObject(nextProps.contexts, (ctx, name) => ctx.loaded ? ctx.results : this.state.cache[name]);
    this.setState({ cache });
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
        onQueryChange={query => _.forEach(this.props.contexts, ctx => ctx.required(query))}
        working={_.any(this.props.contexts, ctx => !ctx.loaded)}
      />
    );
  }
}

const mapStateToProps = (state: any) => ({ state });

const mergeProps = (stateProps: StateProps, dispatchProps: DispatchProps, ownProps: OwnProps): MergedProps => {
  return {
    className: ownProps.className,
    placeholder: ownProps.placeholder,
    contexts: _.mapObject(ownProps.providers, p => p(stateProps.state, dispatchProps.dispatch))
  };
}

export default connect(mapStateToProps, null, mergeProps)(SearchContainer);
