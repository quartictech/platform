import * as React from "react";
import { Header } from "../../components";

import { Ui, Profile } from "../../models";

import * as selectors from "../../redux/selectors";

import { createStructuredSelector } from "reselect";
import * as actions from "../../redux/actions";
import { connect } from "react-redux";

const s = require("./style.css");

interface IProps {
  children?: any;
  location?: {
    pathname: string,
  };
  params?: {
    node: string,
  };
  ui: Ui;
  showNewDatasetModal: any;
  searchDatasets: any;
  selectNamespace: (string) => any;
  logout: () => void;
  namespaces: string[];
  profile?: Profile;
}

export class App extends React.Component<IProps, {}> {
  render() {
    const { children } = this.props;
    return (
      <div>
        <section className={s.App}>
          <Header
            searchBoxChange={this.props.searchDatasets}
            selectedNamespace={this.props.ui.namespace}
            namespaceSelectChange={this.props.selectNamespace}
            onLogOutClick={this.props.logout}
            namespaces={this.props.namespaces}
            profile={this.props.profile}
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
  selectNamespace: actions.selectNamespace,
  logout: actions.userLogout,
};

const mapStateToProps = createStructuredSelector({
  profile: selectors.selectProfile,
  ui: selectors.selectUi,
  namespaces: selectors.selectNamespaces,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(App);
