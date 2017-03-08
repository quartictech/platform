import * as React from "react";
import { Header } from "../../components";

import { createStructuredSelector } from "reselect";
import * as actions from "../../redux/actions";
import { connect } from "react-redux";

const s = require("./style.css");

interface IProps {
  children?: any;
  location?: {
    pathname: string
  };
  params?: {
    node: string
  };
  showNewDatasetModal: any;
  searchDatasets: any;
}

export class App extends React.Component<IProps, void> {
  render() {
    const { children } = this.props;
    return (
      <div>
      <section className={s.App}>
        <Header
          newDatasetClick={this.props.showNewDatasetModal}
          searchBoxChange={this.props.searchDatasets}
        />
        {children}
      </section>
      </div>
    );
  }
}

const mapDispatchToProps = {
  showNewDatasetModal: () => actions.setActiveModal("newDataset"),
  searchDatasets: (s) => actions.searchDatasets(s),
};

const mapStateToProps = createStructuredSelector({
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(App);
