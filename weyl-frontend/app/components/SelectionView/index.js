/**
*
* SelectionView
*
*/

import React from "react";
import naturalsort from 'javascript-natural-sort';

import styles from "./styles.css";
import { curatedAttributes } from "./curatedAttributes";

Object.filter = (obj, predicate) =>
    Object.keys(obj)
          .filter( key => predicate(key, obj[key]) )
          .reduce( (res, key) => Object.assign(res, { [key]: obj[key] }), {} );

const AttributeTable = ({
  attributes
}) => (
  <table className="ui very basic celled very compact fixed selectable table">
    <tbody>
      {Object.keys(attributes)
        .filter(key => key !== "_id")
        .sort(naturalsort)
        .map(key =>
          <tr key={key}>
            <td className="right aligned">
              <div className="ui sub header">{key}</div>
            </td>
            <td>{attributes[key]}</td>
          </tr>
        )
      }
    </tbody>
  </table>
);

class SelectionView extends React.Component { // eslint-disable-line react/prefer-stateless-function
  onClearSelectionClick() {
    this.props.onClearSelection();
  }

  getAttributeBehavior(layerName) {
    return curatedAttributes.hasOwnProperty(layerName)
      ? curatedAttributes[layerName]
      : { title: () => "<< Unknown title >>", blessed: [] };
  }

  getTitle(layerName, properties) {
    const behavior = this.getAttributeBehavior(layerName);
    return behavior.title(properties);
  }

  getBlessedAttributes(layerName, properties) {
    const behavior = this.getAttributeBehavior(layerName);
    return Object.filter(properties, (k, v) => (behavior.blessed.indexOf(k) !== -1) && String(v).trim() !== "");
  }

  getUnblessedAttributes(layerName, properties) {
    const behavior = this.getAttributeBehavior(layerName);
    return Object.filter(properties, (k, v) => (behavior.blessed.indexOf(k) === -1) && String(v).trim() !== "");
  }

  renderInner() {
    if (this.props.selection.length === 0) {
      return null;
    }

    const properties = this.props.selection[0].properties;
    const layerName = this.props.selection[0].layerName;

    return (
      <div className={styles.innerSelectionView}>
        <div className="ui raised fluid card">
          <div className="content">
            <div className="header">
              <a onClick={this.onClearSelectionClick.bind(this)}>
                <i className="icon close"></i>
              </a>
              {this.getTitle(layerName, properties)}
            </div>
            <div className="meta">
              {layerName}
            </div>

            <div className="ui segment">
              <AttributeTable attributes={this.getBlessedAttributes(layerName, properties)} />
            </div>
          </div>

          <div className="extra content">
            <div className="ui accordion" ref={x => $(x).accordion()}>
              <div className="title">
                <i className="dropdown icon"></i>
                More attributes
              </div>

              <div className="content">
                <div className="ui secondary segment">
                  <AttributeTable attributes={this.getUnblessedAttributes(layerName, properties)} />
                </div>
              </div>

            </div>
          </div>

        </div>
      </div>
    );
  }

  render() {
    return (
      <div className={styles.selectionView}>
        {this.renderInner()}
      </div>
    );
  }
}

export default SelectionView;
