import * as React from "react";
import { Link } from "react-router";
import * as classNames from "classnames";
import * as _ from "underscore";
import * as numeral from "numeraljs";

import {
  Classes,
} from "@blueprintjs/core";
import Pane from "../../components/Pane";
import { toTitleCase } from "../../helpers/Utils";

interface Entry {
  rank: number;
  name: string;
  rsl: string;
  description: string;
  length?: number;
  score: number;
}

interface Props {
  highest: Entry[];
  lowest: Entry[];
}

class HighestLowest2016 extends React.Component<Props, {}> {
  render() {
    return (
      <div>
        {this.pane("Highest defects (2016)", this.props.highest,
          { backgroundColor: "rgba(219, 55, 55, 0.15)" })
        }
        {this.pane("Lowest defects (2016)", this.props.lowest,
          { backgroundColor: "rgba(15, 153, 96, 0.15)" })
        }
      </div>
    );
  }

  private pane(title: string, entries: Entry[], style: any) {
    return (
      <Pane title={title} iconName="error">
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
              <th>Defect score</th>
              <th>Length (m)</th>
            </tr>
          </thead>
          <tbody>
            {_.map(entries, entry => this.row(style, entry))}
          </tbody>
        </table>
      </Pane>
    );
  }

  private row(style, entry: Entry) {
    return (
      <tr key={entry.rsl} style={style}>
        <td>{entry.rank}</td>
        <td><Link to={`/assets/${encodeURIComponent(entry.rsl)}`}><b>{entry.rsl}</b></Link></td>
        <td>{toTitleCase(entry.name)}</td>

        <td>{toTitleCase(entry.description)}</td>
        <td>{numeral(entry.score).format("0.0")}</td>
        <td>{entry.length ? numeral(entry.length).format("0") : null}</td>
      </tr>
    );
  }
}


export default HighestLowest2016;
