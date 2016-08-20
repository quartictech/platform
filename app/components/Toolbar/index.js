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

function Toolbar(props) {
  return (
    <div className={styles.toolbar}>
    <div className="ui menu inverted attached">
      <div className="item">
      Quartic Map
      </div>
      <div className="item">
        <div className="ui button" onClick={props.importLayerClick}>Add Layer</div>
      </div>
    </div>
    </div>
  );
}

Toolbar.propTypes = { importLayerClick: React.PropTypes.func}

export default Toolbar;
