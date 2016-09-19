/**
*
* LayerList
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';
import classNames from 'classnames';

import BucketLayerItem from '../BucketLayerItem';

var $ = require('jquery');


const AttributeValue = ({
  value,
  onClick
}) => (
  <div className="item" key={value}>
    <div className="ui checked checkbox">
      <input type="checkbox" defaultChecked name={value} onClick={() => onClick(value)} />
      <label>{value}</label>
    </div>
  </div>
);

const AttributeValueList = ({
  attribute,
  values,
  onClick
}) => (
  <div className="ui list">
    {values.map(v => (
      <AttributeValue
        key={v}
        value={v}
        onClick={(v) => onClick(attribute, v)}
      />
    ))}
  </div>
);

const AttributeList = ({
  layerId,
  attributes,
  onClick
}) => (
  <div className="content">
    {
      Object.keys(attributes)
        .filter(key => attributes[key].categories !== null)
        .map(key => (
          <div className="ui accordion" key={key} ref={x => $(x).accordion()}>
            <div className="title">
              <i className="dropdown icon"></i>
              {key}
            </div>
            <div className="content">
              <AttributeValueList
                key={key}
                attribute={key}
                values={attributes[key].categories}
                onClick={(a,v) => onClick(layerId, a, v)}
              />
            </div>
          </div>
        ))
    }
  </div>
);

const LayerListItem = ({
  layer,
  layerToggleVisible,
  onToggleValueVisible,
  onLayerStyleChange,
  layerClose
}) => {
  let buttonClassNames = classNames("ui mini toggle compact button icon", {"active": layer.visible});
  return (
    <div className={styles.layerListItem}>
      <div className="content">
        <div className="right floated">
          <a>
            <i className="icon eye"></i>
          </a>
          <i className="icon filter"></i>
          <i className="icon paint brush"></i>
        </div>
        <div className="header">
          <a>
            <i className="icon close"></i>
          </a>
          {layer.name}
        </div>
        <div className="meta">
          {layer.description}
        </div>
      </div>

      <div className="ui secondary segment">
        <div className="content">
          <AttributeList
            layerId={layer.id}
            attributes={layer.attributeSchema.attributes}
            onClick={(l,a,v) => onToggleValueVisible(l,a,v)}
          />
        </div>
      </div>
    </div>
  );
}

class LayerList extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    let rows = [];
    if (this.props.ui.layerOp == "bucket") {
      rows.push(<BucketLayerItem onCompute={this.props.onBucketCompute} layers={this.props.layers} onUiToggle={this.props.onUiToggle} key="bucket"/>)
    }
    for (var layer of this.props.layers) {
      if (layer.closed) {
        continue;
      }
      let key="layer_" + layer.id;
      rows.push(<LayerListItem
        layer={layer}
        layerToggleVisible={this.props.layerToggleVisible} key={key}
        onToggleValueVisible={this.props.onToggleValueVisible}
        onLayerStyleChange={this.props.onLayerStyleChange}
        layerClose={this.props.layerClose}
        />);
    }

    if (this.props.layers.length > 0) {
      return (
        <div className={styles.layerList} style={{"visibility": this.props.visible ? "visible" : "hidden"}}>
          <div className="ui raised fluid card">
            <div className={styles.innerLayerList}>
              {rows}
            </div>
          </div>
        </div>
      );
    }
    else {
      return null;
    }
  }
}

export default LayerList;
