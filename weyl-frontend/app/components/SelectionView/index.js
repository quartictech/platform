/**
*
* SelectionView
*
*/

import React from "react";
import naturalsort from 'javascript-natural-sort';

import styles from "./styles.css";

// TODO: these are currently hardcoded
import { blessedAttributes } from "./blessedAttributes";
const BLESSED_TITLE_ATT = "name";


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

  getTitle(properties) {
    return properties.hasOwnProperty(BLESSED_TITLE_ATT) ? properties[BLESSED_TITLE_ATT] : "< No 'name' attribute >";
  }

  getBlessedAttributes(layerName, properties) {
    const pred = blessedAttributes.hasOwnProperty(layerName)
      ? ((k,v) => blessedAttributes[layerName].indexOf(k) !== -1)
      : (() => true);

    return Object.filter(properties, (k,v) => pred(k,v) && k !== BLESSED_TITLE_ATT);
  }

  getUnblessedAttributes(layerName, properties) {
    const pred = blessedAttributes.hasOwnProperty(layerName)
      ? ((k,v) => blessedAttributes[layerName].indexOf(k) === -1)
      : (() => true);

    return Object.filter(properties, (k,v) => pred(k,v) && k !== BLESSED_TITLE_ATT);
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
              {this.getTitle(properties)}
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
