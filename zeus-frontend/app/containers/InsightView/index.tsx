import * as React from "react";

import * as classNames from "classnames";
import * as _ from "underscore";
import { appHistory } from "../../routes";

import {
  Classes,
} from "@blueprintjs/core";
import Pane from "../../components/Pane";

interface Entry {
  rank: number;
  name: string;
  rsl: string;
  description: string;
  score: number;
}

const HIGHEST_DEFECT_ROADS: Entry[] = [
  { rank: 1, name: "Noob Road", rsl: "B454/030_3E_01", description: "Somewhere naughty", score: 1.96 },
  { rank: 2, name: "Noob Road", rsl: "B454/030_3E_01", description: "Somewhere naughty", score: 1.96 },
  { rank: 3, name: "Noob Road", rsl: "B454/030_3E_01", description: "Somewhere naughty", score: 1.96 },
  { rank: 4, name: "Noob Road", rsl: "B454/030_3E_01", description: "Somewhere naughty", score: 1.96 },
  { rank: 5, name: "Noob Road", rsl: "B454/030_3E_01", description: "Somewhere naughty", score: 1.96 },
];

const LOWEST_DEFECT_ROADS: Entry[] = [
  { rank: 1, name: "Noob Road", rsl: "B454/030_3E_01", description: "Somewhere naughty", score: 1.96 },
  { rank: 2, name: "Noob Road", rsl: "B454/030_3E_01", description: "Somewhere naughty", score: 1.96 },
  { rank: 3, name: "Noob Road", rsl: "B454/030_3E_01", description: "Somewhere naughty", score: 1.96 },
  { rank: 4, name: "Noob Road", rsl: "B454/030_3E_01", description: "Somewhere naughty", score: 1.96 },
  { rank: 5, name: "Noob Road", rsl: "B454/030_3E_01", description: "Somewhere naughty", score: 1.96 },
];


class InsightView extends React.Component<{}, {}> {

  render() {
    return (
      <div>
        {this.renderPane("Highest defects (2016)", HIGHEST_DEFECT_ROADS, { backgroundColor: "rgba(219, 55, 55, 0.15)" })}
        {this.renderPane("Lowest defects (2016)", LOWEST_DEFECT_ROADS, { backgroundColor: "rgba(15, 153, 96, 0.15)" })}
      </div>
    );
  }

  private renderPane(title: string, entries: Entry[], style: any) {
    return (
      <Pane title={title} iconName="error">
        <table
          className={classNames(Classes.TABLE, Classes.INTERACTIVE, Classes.TABLE_STRIPED, Classes.TABLE_CONDENSED)}
          style={{ width: "100%", tableLayout: "fixed" }}
        >
          <thead>
            <tr>
              <th>Ranking</th>
              <th>Road name</th>
              <th>RSL</th>
              <th>Section description</th>
              <th>Defect score</th>
            </tr>
          </thead>
          <tbody>
            {_.map(entries, entry => this.renderRow(style, entry))}
          </tbody>
        </table>
      </Pane>
    );
  }

  private renderRow(style, entry: Entry) {
    return (
      <tr style={style} onClick={() => appHistory.push(`/assets/${encodeURIComponent(entry.rsl)}`)}>
        <td>{entry.rank}</td>
        <td>{entry.name}</td>
        <td>{entry.rsl}</td>
        <td>{entry.description}</td>
        <td>{entry.score}</td>
      </tr>
    );
  }
}


export default InsightView;
