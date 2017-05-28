import * as React from "react";
import * as Redux from "redux";
import { connect } from "react-redux";
import * as _ from "underscore";

import Picker, { PickerEntry } from "../../components/Picker";
import {
  ResourceState,
} from "../../api-management";

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

 *-----------------------------------------------------*/

// TODO - eliminate assets/jobs as named props params

interface StateProps {
  states: { [id: string] : ResourceState<any> };
}

interface DispatchProps {
  dispatch: Redux.Dispatch<any>;
}

interface OwnProps {
  className?: string;
  placeholder?: string;
  providers: { [id: string] : SearchProvider<any> };
}

export type AllProps = StateProps & DispatchProps & OwnProps;

interface State {
  cache: { [ id: string ] : PickerEntry[] };
}

// TODO - get rid of props currying
export interface SearchProvider<TStore> {
  selector: (state) => TStore;
  required: (dispatch: Redux.Dispatch<any>, fromStore: TStore) => (string) => void;
  results: (dispatch: Redux.Dispatch<any>, fromStore: TStore) => PickerEntry[],
  loaded: (dispatch: Redux.Dispatch<any>, fromStore: TStore) => boolean;
}

class SearchContainer extends React.Component<AllProps, State> {
  constructor(props: AllProps) {
    super(props);
    console.log(props);
    this.state = { cache: {} };
  }

  componentWillReceiveProps(nextProps: AllProps) {
    const cache = _.mapObject(nextProps.providers, (provider: SearchProvider<any>, name) =>
      provider.loaded(nextProps.dispatch, nextProps.states[name])
      ? provider.results(nextProps.dispatch, nextProps.states[name])
      : this.state.cache[name]
    );
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
        onQueryChange={query => _.forEach(this.props.providers, (p, name) => p.required(this.props.dispatch, this.props.states[name])(query))}
        working={_.any(this.props.providers, (p, name) => !p.loaded(this.props.dispatch, this.props.states[name]))}
      />
    );
  }
}

const mapStateToProps = (state: any, ownProps: OwnProps) => ({
  states: _.mapObject(ownProps.providers, p => p.selector(state)),
});

export default connect<StateProps, DispatchProps, OwnProps>(mapStateToProps, null)(SearchContainer);
