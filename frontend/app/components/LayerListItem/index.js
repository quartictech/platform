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

import * as numeral from 'numeral';

import LayerStyleSettings from '../LayerStyleSettings';

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
          <div className="ui accordion" key={key}>
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

class LayerListItem extends React.Component { // eslint-disable-line react/prefer-stateless-function
  onLayerVisibleClick(event) {
    this.props.layerToggleVisible(event.currentTarget.id);
  }

  onLayerCloseClick(event) {
    this.props.layerClose(event.currentTarget.id);
  }

  componentDidMount() {
    $(".ui.accordion").accordion();
  }

  render() {
    let layer = this.props.layer;
    let buttonClassNames = classNames("ui mini toggle compact button icon", {"active": layer.visible});
    let layerToggleVisible = this.props.layerToggleVisible;
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
              layerId={this.props.layer.id}
              attributes={layer.attributeSchema.attributes}
              onClick={(l,a,v) => this.props.onToggleValueVisible(l,a,v)}
            />
          </div>
        </div>
      </div>
    );
  }
}

export default LayerListItem;
