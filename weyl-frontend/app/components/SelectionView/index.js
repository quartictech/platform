/**
*
* SelectionView
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';

class SelectionView extends React.Component { // eslint-disable-line react/prefer-stateless-function

  onClearSelectionClick() {
    this.props.onClearSelection();
  }

  renderInner() {
    if (this.props.selection.length == 0) {
      return;
    }
    else {
        let rows = [];

        let feature = this.props.selection[0];
        for (var property in feature.properties) {
          if (property === "_id") continue;
          rows.push(<tr key={property}><td>{property}</td><td>{feature.properties[property]}</td></tr>);
        }

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
                  {rows}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        );
    }
  }

  render() {
    return (
      <div className={styles.selectionView}>
        {this.renderInner()}
        </div>
    );
  }
}

SelectionView.PropTypes = {
  selection: React.PropTypes.array,
  onClearSelection: React.PropTypes.func
}

export default SelectionView;
