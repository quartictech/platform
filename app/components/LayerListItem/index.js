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

class LayerListItem extends React.Component { // eslint-disable-line react/prefer-stateless-function
  onLayerVisibleClick(event) {
    this.props.layerToggleVisible(event.currentTarget.id);
  }

  componentDidMount() {
    $(this.accordion).accordion();
  }

  renderLayerStats(layer) {
    let rows = [];
    let attributeStats = layer.stats.attributeStats;
    for (var key in attributeStats) {
      rows.push(
      <tr key={key}><td>{key}</td><td>{attributeStats[key].type}</td><td>{numeral(attributeStats[key].minimum).format()} -> {numeral(attributeStats[key].maximum).format()}</td></tr>
    )
    }
    return rows;
  }

  render() {
    let layer = this.props.layer;
    let buttonClassNames = classNames("ui toggle compact button icon left attached", {"active": layer.visible});
    let layerToggleVisible = this.props.layerToggleVisible;
    return (
      <div className="ui raised fluid card">
      <div className="content">
        <div className="right floated">
          <button className={buttonClassNames} onClick={this.onLayerVisibleClick.bind(this)} id={layer.id}>
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

        <div className="ui accordion" ref={x => this.accordion=x}>
          <div className="title">
            <i className="dropdown icon"></i>
            Attributes
          </div>
          <div className="content">
            <table className="ui celled table">
              <thead>
                <tr><th>Attribute</th> <th>Type</th> <th>Detail</th> </tr>
              </thead>
              <tbody>
                {this.renderLayerStats(layer)}
              </tbody>
            </table>
          </div>

          <div className="title">
            <i className="dropdown icon"></i>
            Styles
          </div>
          <div className="content">
            {layer.style.polygon.property}
          </div>
        </div>
      </div>
    </div>

    );
  }
}

LayerListItem.propTypes = {
  layer: React.PropTypes.object,
  layerToggleVisible: React.PropTypes.func
}

export default LayerListItem;
