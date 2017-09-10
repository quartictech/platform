import * as React from "react";
import { Header } from "../../components";

import { Ui, Profile } from "../../models";

import * as selectors from "../../redux/selectors";

import { createStructuredSelector } from "reselect";
import * as actions from "../../redux/actions";
import { connect } from "react-redux";
import { gql, graphql } from "react-apollo";

const s = require("./style.css");

interface IProps {
  children?: any;
  location?: {
    pathname: string,
  };
  params?: {
    node: string,
  };
  data: {
    loading: boolean;
    profile?: Profile;
    error: any;
  };
  ui: Ui;
  showNewDatasetModal: any;
  searchDatasets: any;
  logout: () => void;
}

export class App extends React.Component<IProps, {}> {
  render() {
    const { children } = this.props;
    return (
      <div>
        <section className={s.App}>
          <Header
            searchBoxChange={this.props.searchDatasets}
            onLogOutClick={this.props.logout}
            profile={this.props.data.profile}
          />
          <div className={s.container}>
            {children}
          </div>
        </section>
      </div>
    );
  }
}

const mapDispatchToProps = {
  searchDatasets: actions.searchDatasets,
  logout: actions.userLogout,
};

const mapStateToProps = createStructuredSelector({
  ui: selectors.selectUi,
});

const PROFILE_QUERY = gql`{
  profile { name, avatarUrl }
}
`

export default graphql(PROFILE_QUERY)(connect(
  mapStateToProps,
  mapDispatchToProps,
)(App));
