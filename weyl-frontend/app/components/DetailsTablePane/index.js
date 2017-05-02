import React from "react";
import {
  Colors,
} from "@blueprintjs/core";

// import classNames from "classnames";
// import naturalsort from "javascript-natural-sort";
import * as _ from "underscore";

import Pane from "../Pane";


class DetailsTablePane extends React.Component { // eslint-disable-line react/prefer-stateless-function
  render() {
    const schema = this.props.details.schema;
    return (
      <Pane
        title="Details"
        iconName="th"
        visible={this.props.visible}
        onClose={() => this.props.onUiToggle("table")}
      >
        <div style={{ overflow: "auto" }}>
          <table className="pt-table pt-interactive pt-elevation-0" style={{ width: "100%", tableLayout: "fixed" }}>
            <thead>
              <tr>
                { _.map(schema, e => <th key={e}>{e}</th>) }
              </tr>
            </thead>
            <tbody>
              {
                _.map(this.props.details.records, (r, idx) => (
                  <tr key={idx}>
                    { _.map(schema, e => (
                      <td key={e} style={{ fontWeight: "bold", wordWrap: "break-word", backgroundColor: Colors.DARK_GRAY3 }}>
                        <small>{r[e]}</small>
                      </td>
                    ))}
                  </tr>
                ))
              }
            </tbody>
          </table>
        </div>
      </Pane>
    );
  }
}

export default DetailsTablePane;
