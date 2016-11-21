import * as React from "react";
import { connect } from "react-redux";

import * as Blueprint from "@blueprintjs/core";
const { Menu, MenuItem, MenuDivider } = Blueprint;

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require("./style.css");

import { DatasetList } from "../../components/DatasetList";

interface IProps {
  datasets: any;
  fetchDatasets: any;
}

class Home extends React.Component<IProps, any> {
  componentDidMount() {
    this.props.fetchDatasets();
  }

  render() {
    return (
      <div className={s.container}>

      <div className={s.left}>

      <Menu className="pt-elevation-1">
               <MenuItem
                   iconName="new-text-box"
                   text="Live" />
               <MenuItem
                   iconName="new-object"
                   text="Static" />
               <MenuDivider />
               <MenuItem text="Settings..." iconName="cog" />
           </Menu>
      </div>

      <div className={s.main}>
        <DatasetList datasets={this.props.datasets.datasets} />
      </div>
      </div>
    );
  }
}

export { Home };

const mapDispatchToProps = {
  fetchDatasets: actions.fetchDatasets
};

const mapStateToProps = createStructuredSelector({
  datasets: selectors.selectDatasets,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Home);
