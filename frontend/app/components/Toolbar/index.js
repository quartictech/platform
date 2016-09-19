/**
*
* Toolbar
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';

import 'lato-font/css/lato-font.css';
import styles from './styles.css';

import LayerSearch from '../LayerSearch';
import classNames from 'classnames';

function Toolbar(props) {
  let bucketClassNames = classNames("item", {"active": props.ui.layerOp === "bucket"})
  let chartClassNames = classNames("item", {"active": props.ui.panels.chart})
  let layerListClassNames = classNames("item", {"active": props.ui.panels.layerList})
  let satelliteClassNames = classNames("item", {"active": props.ui.settings.satellite})
  console.log(props.ui);
  return (
    <div className={styles.toolbar}>
    <div className="ui menu inverted attached labeled icon">
      <div className="header item">
      <i className="icon map"/><span className={styles.brand}>Quartic</span>
      </div>

      <div className="right menu">
        <a className={satelliteClassNames} onClick={(e) => props.onUiToggle("satellite")}>
          <i className="icon rocket"></i>
          Satellite
        </a>
        <a className={layerListClassNames} onClick={(e) => props.onUiToggle("layerList")}>
          <i className="icon list"></i>
          Layers
        </a>
        <a className={bucketClassNames} onClick={(e) => props.onUiToggle("bucket")}>
          <i className="icon object group"></i>
          Bucket
        </a>
        <a className={chartClassNames} onClick={(e) => props.onUiToggle("chart")}>
          <i className="icon line chart"></i>
          Chart
        </a>
        <div className="item">
        <LayerSearch onSearch={props.onSearch} onSelect={props.onSelect}/>
        </div>
      </div>
    </div>
    </div>
  );
}

Toolbar.propTypes = {
  onSearch: React.PropTypes.func,
  onSelect: React.PropTypes.func,
  loading: React.PropTypes.bool,
  ui: React.PropTypes.object,
  onUiToggle: React.PropTypes.func
}

export default Toolbar;
