/**
*
* SelectionView
*
*/

import React from "react";
import naturalsort from "javascript-natural-sort";

import styles from "./styles.css";
import { defaultBehavior, curatedBehaviors } from "./behaviors";

Object.filter = (obj, predicate) =>
    Object.keys(obj)
          .filter( key => predicate(key, obj[key]) )
          .reduce( (res, key) => Object.assign(res, { [key]: obj[key] }), {} );

const AttributeTable = ({
  attributes,
  order
}) => (
  <table className="ui very basic celled very compact fixed selectable table">
    <tbody>
      {order
        .filter(key => key !== "_id")
        .filter(key => attributes.hasOwnProperty(key))
        .filter(key => String(attributes[key]).trim() !== "")
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
    return curatedBehaviors.hasOwnProperty(layerName)
      ? curatedBehaviors[layerName]
      : defaultBehavior;
  }

  getTitle(layerName, properties) {
    const behavior = this.getAttributeBehavior(layerName);
    return behavior.title(properties);
  }

  isAnythingBlessed(layerName) {
    const behavior = this.getAttributeBehavior(layerName);
    return behavior.blessed.length > 0;
  }

  // In the order specified in curatedAttributes
  getBlessedAttributeOrder(layerName, properties) {
    const behavior = this.getAttributeBehavior(layerName);
    return behavior.blessed
      .filter(k => properties.hasOwnProperty(k));
  }

  // Find all other properties, and then natural-sort for convenience
  getUnblessedAttributeOrder(layerName, properties) {
    const behavior = this.getAttributeBehavior(layerName);
    return Object.keys(properties)
      .filter(k => (behavior.blessed.indexOf(k) === -1))
      .sort(naturalsort);
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
              <AttributeTable
                attributes={properties}
                order={
                  this.isAnythingBlessed(layerName)
                    ? this.getBlessedAttributeOrder(layerName, properties)
                    : this.getUnblessedAttributeOrder(layerName, properties)
                }
              />
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
                  <AttributeTable
                    attributes={properties}
                    order={
                      this.isAnythingBlessed(layerName)
                        ? this.getUnblessedAttributeOrder(layerName, properties)
                        : this.getBlessedAttributeOrder(layerName, properties)
                    }
                  />
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
