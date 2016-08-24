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

function Toolbar(props) {
  let buttonClass = "ui button";
  if (props.loading) {
    buttonClass += " loading";
  }
  return (
    <div className={styles.toolbar}>
    <div className="ui menu inverted attached">
      <div className="item">
      Quartic Map
      </div>
      <div className="item">
        <div className={buttonClass} onClick={props.importLayerClick}>Add Layer</div>
      </div>
      <div className="item">
      <LayerSearch/>
      </div>
    </div>
    </div>
  );
}

Toolbar.propTypes = {
  importLayerClick: React.PropTypes.func,
  loading: React.PropTypes.bool
}

export default Toolbar;
