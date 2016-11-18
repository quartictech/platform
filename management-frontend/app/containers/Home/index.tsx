import * as React from 'react';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';
import { slugify } from '../../helpers/Utils';

import { Grid, Col } from 'react-bootstrap';
import * as Blueprint from "@blueprintjs/core";
const { Menu, MenuItem, MenuDivider } = Blueprint;

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
const s = require('./style.css');

interface IProps {
  datasets: any
}

class Home extends React.Component<IProps, any> {
  render() {
    return (
      <div className={s.container}>

      <div className={s.left}>
      <Menu className=".modifier pt-elevation-1">
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
      {this.props.datasets.size}
      </div>
      </div>
    );
  }
}

export { Home };

const mapDispatchToProps = {
};

const mapStateToProps = createStructuredSelector({
  datasets: selectors.selectDatasets,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Home);
