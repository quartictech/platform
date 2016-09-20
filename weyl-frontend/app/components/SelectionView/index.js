/**
*
* SelectionView
*
*/

import React from "react";
import naturalsort from 'javascript-natural-sort';

import styles from "./styles.css";

class SelectionView extends React.Component { // eslint-disable-line react/prefer-stateless-function

  onClearSelectionClick() {
    this.props.onClearSelection();
  }

  renderInner() {
    if (this.props.selection.length === 0) {
      return null;
    }

    const feature = this.props.selection[0];
    return (
      <div className={styles.innerSelectionView}>
        <div className="ui raised fluid card">
          <div className="content">
            <div className="ui header">
              Selection
              <button className="right floated tiny ui button primary" onClick={this.onClearSelectionClick.bind(this)}>Clear</button>
            </div>
            <table className="ui celled table">
              <thead>
                <tr>
                  <th>Attribute</th>
                  <th>Value</th>
                </tr>
              </thead>
              <tbody>
                {Object.keys(feature.properties)
                  .filter(key => key !== "_id")
                  .sort(naturalsort)
                  .map(key =>
                    <tr key={key}><td>{key}</td><td>{feature.properties[key]}</td></tr>
                  )
                }
              </tbody>
            </table>
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
