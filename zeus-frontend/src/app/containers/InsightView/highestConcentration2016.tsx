import * as React from "react";
import { Link } from "react-router";
import {
  Classes,
} from "@blueprintjs/core";
import * as classNames from "classnames";
import * as _ from "underscore";
import * as numeral from "numeraljs";
import Pane from "../../components/Pane";
import { toTitleCase } from "../../helpers/Utils";

interface Entry {
  name: string;
  rsl: string;
  description: string;
  length?: number;
  concentration: number;
}

interface Props {
  entries: Entry[];
}

class HighestConcentration2016 extends React.Component<Props, {}> {
  render() {
    return (
      <div>
        <Pane title="Highest defect concentration" iconName="error">
          <table
            className={classNames(Classes.TABLE, Classes.TABLE_STRIPED, Classes.TABLE_CONDENSED)}
            style={{ width: "100%", tableLayout: "fixed" }}
          >
            <thead>
              <tr>
                <th>Ranking</th>
                <th>RSL</th>
                <th>Road name</th>
                <th>Section description</th>
                <th>Defect concentration</th>
                <th>Length (m)</th>
              </tr>
            </thead>
            <tbody>
              {_.map(this.props.entries,
                (entry, idx) => this.row({ backgroundColor: "rgba(219, 55, 55, 0.15)" }, entry, idx))
              }
            </tbody>
          </table>
        </Pane>
      </div>
    );
  }

  private row(style, entry: Entry, idx: number) {
    return (
      <tr key={entry.rsl} style={style}>
        <td>{idx + 1}</td>
        <td><Link to={`/assets/${encodeURIComponent(entry.rsl)}`}><b>{entry.rsl}</b></Link></td>
        <td>{toTitleCase(entry.name)}</td>
        <td>{toTitleCase(entry.description)}</td>
        <td>{numeral(entry.concentration).format("0.0")}</td>
        <td>{entry.length ? numeral(entry.length).format("0") : null}</td>
      </tr>
    );
  }
}


export default HighestConcentration2016;
