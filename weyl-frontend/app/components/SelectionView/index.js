/**
*
* SelectionView
*
*/

import React from "react";
import naturalsort from 'javascript-natural-sort';

import styles from "./styles.css";

// TODO: these are currently hardcoded
const LAYER_NAME = "London Boroughs";
const BLESSED_TITLE_ATT = "name";
const BLESSED_PRIMARY_ATTS = [
  "hectares",
  "political control in council",
  "average age 2015",
  "crime rates per thousand population 2014/15"
];

class SelectionView extends React.Component { // eslint-disable-line react/prefer-stateless-function
  onClearSelectionClick() {
    this.props.onClearSelection();
  }

  getTitle(properties) {
    return properties.hasOwnProperty(BLESSED_TITLE_ATT) ? properties[BLESSED_TITLE_ATT] : "< WTF >";
  }

  renderInner() {
    if (this.props.selection.length === 0) {
      return null;
    }

    const properties = this.props.selection[0].properties;

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
              {LAYER_NAME}
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
                  <table className="ui very basic celled fixed table">
                    <tbody>
                      {Object.keys(properties)
                        .filter(key => key !== "_id")
                        .sort(naturalsort)
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
