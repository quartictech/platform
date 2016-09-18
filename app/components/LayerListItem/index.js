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

  renderAttributeEnum(data) {
    return (
      <div className="ui list">
        {data.map(value => {
          return (
            <div className="item" key={value}>
              <div className="ui checked checkbox">
                <input type="checkbox" defaultChecked name={value} />
                <label>{value}</label>
              </div>
            </div>
          );
        })}
      </div>
    );
  }

  renderLayerStats(layer) {
    let rows = [];
    let attributeStats = layer.stats.attributeStats;
    for (var key in attributeStats) {
      rows.push(
        <div className="ui accordion" key={key}>
          <div className="title">
            <i className="dropdown icon"></i>
            {key}
          </div>
          <div className="content">
            {this.renderAttributeEnum(['Arse', 'Bum', 'Cheeks'])}
          </div>
        </div>
      );
    }
    return rows;
  }

  render() {
    let layer = this.props.layer;
    let buttonClassNames = classNames("ui toggle compact button icon left attached", {"active": layer.visible});
    let layerToggleVisible = this.props.layerToggleVisible;
    return (
      <div className={styles.layerListItem}>
      <div className="ui raised fluid card">
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
