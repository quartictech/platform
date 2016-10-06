import React from "react";
import naturalsort from "javascript-natural-sort";
const $ = require("jquery");
const _ = require("underscore");

import styles from "./styles.css";
import { defaultBehavior, curatedBehaviors } from "./behaviors";

class SelectionView extends React.Component { // eslint-disable-line react/prefer-stateless-function
  renderInner() {
    if ((this.props.selection.length === 0)
      || !this.props.selection[0].layer.visible) {
      return null;
    }

    const numUniqueLayers = _.chain(this.props.selection)
      .map(s => s.layer.id).uniq().size().value();
    const numFeatures = this.props.selection.length;

    if (numFeatures === 1) {
      console.log("Single object");
    } else if (numFeatures === 2 && numUniqueLayers === 1) {
      console.log("Side-by-side comparison");
    } else {
      console.log("Aggregates");
    }

    return (
      <div className={styles.innerSelectionView}>
        <div className="ui raised fluid card">
          <div className="content">
            <Header selection={this.props.selection} onClose={this.props.onClose} />
            <Media selection={this.props.selection} />
            <BlessedProperties selection={this.props.selection} />
          </div>

          <div className="extra content">
            <div className="ui accordion" ref={x => $(x).accordion()}>
              <div className="title">
                <i className="dropdown icon"></i>
                More properties
              </div>

              <div className="content">
                <UnblessedProperties selection={this.props.selection} />
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

const Header = ({ selection, onClearSelectionClick }) => (
  <div className="header">
    <a onClick={onClearSelectionClick}>
      <i className="icon close"></i>
    </a>
    {
      (selection.length > 1)
        ? `${selection.length} features selected`
        : getTitle(selection[0].layer.metadata.name, selection[0].properties)
    }
  </div>
);

const Media = ({ selection }) => {
  const properties = selection[0].properties;
  const layerName = selection[0].layer.metadata.name;

  return (hasImageUrl(layerName)) ? (
    <div className="ui segment">
      <img
        className="ui fluid image"
        src={properties[getImageUrl(layerName)]}
        alt={properties[getImageUrl(layerName)]}
      />
    </div>
  ) : <div></div>;
};

const BlessedProperties = ({ selection }) => {
  const properties = selection[0].properties;
  const layerName = selection[0].layer.metadata.name;

  return (
    <div className="ui segment">
      <PropertiesTable
        properties={properties}
        order={
          isAnythingBlessed(layerName)
            ? getBlessedPropertyOrder(layerName, properties)
            : getUnblessedPropertyOrder(layerName, properties)
        }
      />
    </div>
  );
};

const UnblessedProperties = ({ selection }) => {
  const properties = selection[0].properties;
  const layerName = selection[0].layer.metadata.name;

  return (
    <div className="ui secondary segment">
      <PropertiesTable
        properties={properties}
        order={
          isAnythingBlessed(layerName)
            ? getUnblessedPropertyOrder(layerName, properties)
            : getBlessedPropertyOrder(layerName, properties)
        }
      />
    </div>
  );
};

const PropertiesTable = ({ properties, order }) => (
  <table className="ui very basic celled very compact fixed selectable table">
    <tbody>
      {order
        .filter(key => key !== "_id")
        .filter(key => key in properties)
        .filter(key => String(properties[key]).trim() !== "")
        .map(key =>
          <tr key={key}>
            <td className="right aligned">
              <div className="ui sub header">{key}</div>
            </td>
            <td>{properties[key]}</td>
          </tr>
        )
      }
    </tbody>
  </table>
);

const getTitle = (layerName, properties) =>
  getBehavior(layerName).title(properties);

const hasImageUrl = (layerName) =>
  ("imageUrl" in getBehavior(layerName));

const getImageUrl = (layerName) =>
  getBehavior(layerName).imageUrl;

const isAnythingBlessed = (layerName) =>
  getBehavior(layerName).blessed.length > 0;

// In the specified order
const getBlessedPropertyOrder = (layerName, properties) =>
  getBehavior(layerName).blessed.filter(k => k in properties);

// Find all other properties, and then natural-sort for convenience
const getUnblessedPropertyOrder = (layerName, properties) => {
  const behavior = getBehavior(layerName);
  return Object.keys(properties)
    .filter(k => (behavior.blessed.indexOf(k) === -1))
    .sort(naturalsort);
};

const getBehavior = (layerName) =>
  ((layerName in curatedBehaviors) ? curatedBehaviors[layerName] : defaultBehavior);

export default SelectionView;
