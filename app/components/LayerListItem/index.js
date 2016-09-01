/**
*
* LayerListItem
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';
import classNames from 'classnames';

class LayerListItem extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    let layer = this.props.layer;
    let buttonClassNames = classNames("ui toggle compact button icon left attached", {"active": layer.visible});
    return (
      <div className="ui raised fluid card" key={layer.id}>
      <div className="content">
        <div className="right floated">
          <button className={buttonClassNames} onClick={this.props.layerVisibleToggle} id={layer.id}>
            <i className="icon eye"></i>
          </button>
          <div className="ui compact dropdown button right attached icon secondary">
            <i className="icon paint brush"></i>
            <div className="menu">
              <div className="item">
              <i className="dropdown icon"></i>Fill
              <div className="menu">
                <div className="item">Choropleth</div>
              </div>
            </div>
          <div className="item">Stroke</div>
        </div>
      </div>
    </div>
    <div className="header">
          {layer.name}
        </div>
        <div className="meta">
        {layer.description}
        </div>
    </div>
    </div>

    );
  }
}

LayerListItem.propTypes = {
  layer: React.PropTypes.object
}

export default LayerListItem;
