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

class LayerListItem extends React.Component { // eslint-disable-line react/prefer-stateless-function
  onLayerVisibleClick(event) {
    this.props.layerToggleVisible(event.currentTarget.id);
  }

  onLayerCloseClick(event) {
    this.props.layerClose(event.currentTarget.id);
  }

  componentDidMount() {
    $(this.accordion).accordion();
  }

  renderLayerStats(layer) {
    return (
      Object.keys(layer.attributeSchema.attributes)
        .filter(key => layer.attributeSchema.attributes[key].categories !== null)
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
                values={layer.attributeSchema.attributes[key].categories}
                onClick={(a,v) => this.props.onToggleValueVisible(this.props.layer.id, a, v)}
              />
            </div>
          </div>
      ))
    );
  }

  render() {
    let layer = this.props.layer;
    let buttonClassNames = classNames("ui toggle compact button icon left attached", {"active": layer.visible});
    let layerToggleVisible = this.props.layerToggleVisible;
    return (
      <div className={styles.layerListItem}>
      <div className="ui fluid card">
      <div className="content">
        <div className="right floated">
          <button className={buttonClassNames} onClick={this.onLayerVisibleClick.bind(this)} id={layer.id}>
            <i className="icon eye"></i>
          </button>
            <button className="ui compact button icon right attached" onClick={this.onLayerCloseClick.bind(this)} id={layer.id}>
            <i className="icon close"></i>
          </button>
    </div>
    <div className="header">
          {layer.name}
        </div>
        <div className="meta">
        {layer.description}
        </div>

        <div className="right floated">
          <div className="ui mini statistic"><div className="value">{numeral(layer.stats.featureCount).format('0.0a')}</div><div className="label"> Features </div> </div>
        </div>
        <div className="ui styled fluid accordion" ref={x => this.accordion=x}>
          <div className="title">
            <i className="dropdown icon"></i>
            Attributes
          </div>
          <div className="content">
            {this.renderLayerStats(layer)}
          </div>

          <div className="title">
            <i className="dropdown icon"></i>
            Styles
          </div>
          <div className="content">
            <LayerStyleSettings layer={layer} onChange={this.props.onLayerStyleChange}/>
          </div>
        </div>
      </div>
    </div>
  </div>

    );
  }
}

LayerListItem.propTypes = {
  layer: React.PropTypes.object,
  layerToggleVisible: React.PropTypes.func,
  layerClose: React.PropTypes.func,
  onLayerStyleChange: React.PropTypes.func
}

export default LayerListItem;
