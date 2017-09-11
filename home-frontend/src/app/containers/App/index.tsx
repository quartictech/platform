import * as React from "react";
import { Header } from "../../components";

import { Profile } from "../../models";

import * as selectors from "../../redux/selectors";

import { createStructuredSelector } from "reselect";
import * as actions from "../../redux/actions";
import { connect } from "react-redux";

const s = require("./style.css");

interface IProps {
  children?: any;
  showNewDatasetModal: any;
  searchDatasets: any;
  logout: () => void;
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
            onLogOutClick={this.props.logout}
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
  logout: actions.userLogout,
};

const mapStateToProps = createStructuredSelector({
  profile: selectors.selectProfile,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(App);
