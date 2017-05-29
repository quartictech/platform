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

export interface SearchContextMapper {
  (dispatch: Redux.Dispatch<any>): SearchContext
}

export interface SearchProvider {
  (reduxState: any): SearchContextMapper;
}

interface StateProps {
  mappers: { [id: string] : SearchContextMapper };
}

interface DispatchProps {
  dispatch: Redux.Dispatch<any>;
}

interface OwnProps {
  className?: string;
  placeholder?: string;
  providers: { [id: string] : SearchProvider };
}

export type AllProps = StateProps & DispatchProps & OwnProps;

interface State {
  cache: { [ id: string ] : SearchResultEntry[] };
}

class SearchContainer extends React.Component<AllProps, State> {
  constructor(props: AllProps) {
    super(props);
    console.log(props);
    this.state = { cache: {} };
  }

  // TODO - optimise for no change in data
  componentWillReceiveProps(nextProps: AllProps) {
    const cache = _.mapObject(contexts(nextProps), (ctx, name) => ctx.loaded ? ctx.results : this.state.cache[name]);
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
        onQueryChange={query => _.forEach(contexts(this.props), ctx => ctx.required(query))}
        working={_.any(contexts(this.props), ctx => !ctx.loaded)}
      />
    );
  }
}

const contexts = (props: AllProps) => _.mapObject(props.mappers, m => m(props.dispatch));

const mapStateToProps = (state: any, ownProps: OwnProps) => ({
  mappers: _.mapObject(ownProps.providers, provider => provider(state)),
});

export default connect<StateProps, DispatchProps, OwnProps>(mapStateToProps, null)(SearchContainer);
