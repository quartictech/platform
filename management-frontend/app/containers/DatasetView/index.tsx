import * as React from 'react';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';

import { Grid, Col } from 'react-bootstrap';
import * as Blueprint from "@blueprintjs/core";
const { Menu, MenuItem, MenuDivider } = Blueprint;

import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require('./style.css');

interface IProps {
  datasets: any;
  fetchDatasets: any;
}

class DatasetView extends React.Component<IProps, any> {
  componentDidMount() {
    this.props.fetchDatasets();
  }

  render() {
    return (
      <div className={s.DatasetView}>
      Wat
      </div>
    );
  }
}

export { DatasetView };

const mapDispatchToProps = {
  fetchDatasets: actions.fetchDatasets
};

const mapStateToProps = createStructuredSelector({
  datasets: selectors.selectDatasets,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DatasetView);
