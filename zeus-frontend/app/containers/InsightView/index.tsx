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
  {
    "rank": 1, "rsl": "D244/010_5E_01", "score": 11.044694762716798,
    "description": "HAMPTON LN TO RIVERDALE RD (MILLBOURNE RD)", "name": "MILLBOURNE ROAD",
  },
  {
    "rank": 2, "rsl": "D758/005_5E_01", "score": 10.984039245395659,
    "description": "JERSEY RD TO PEVENSEY CL (S)", "name": "PEVENSEY CLOSE",
  },
  {
    "rank": 3, "rsl": "D1181/050_5E_01", "score": 10.655653212181992,
    "description": "STAVELEY GRDS TO END OF RD (STAVELEY GRDS EB)", "name": "STAVELEY GARDENS",
  },
  {
    "rank": 4, "rsl": "D666/005_5E_01", "score": 10.323185274237224,
    "description": "MONTAGUE RD TO END OF RD", "name": "YORK ROAD",
  },
  {
    "rank": 5, "rsl": "D852/010_5E_01", "score": 10.270034203090121,
    "description": "RBT TREVOR CL TO RBT TREVOR CL", "name": "TREVOR CLOSE",
  },
  ];
const LOWEST_DEFECT_ROADS: Entry[] = [
  {
    "rsl": "D2016/010_5E_01", "score": 0.0,
    "description": "WORTON RD TO END OF RD (FLEMING WAY)", "name": "FLEMING WAY", "rank": 2533,
  },
  {
    "rsl": "C013/525_3D_01", "score": 0.0,
    "description": "ROSTREVOR GRDS TO THORNCLIFFE RD", "name": "NORTH HYDE LANE", "rank": 2532,
  },
  {
    "rsl": "C014/515_3E_01", "score": 0.0,
    "description": "JCT SPRINGWELL RD TO BERKELEY WAYE", "name": "CRANFORD LANE", "rank": 2531,
  },
  {
    "rsl": "C014/515_3E_02", "score": 0.0,
    "description": "FAIRMEAD CL TO END OF SLIP (CRANFORD LN)", "name": "CRANFORD LANE", "rank": 2530,
  },
  {
    "rsl": "D757/005_5E_01", "score": 0.0,
    "description": "JERSEY RD TO GREAT W RD (STUCLEY RD)", "name": "STUCLEY ROAD", "rank": 2529,
  },
  ];

class InsightView extends React.Component<{}, {}> {

  render() {
    return (
      <div>
        {this.renderPane("Highest defects (2016)", HIGHEST_DEFECT_ROADS,
          { backgroundColor: "rgba(219, 55, 55, 0.15)" })
        }
        {this.renderPane("Lowest defects (2016)", LOWEST_DEFECT_ROADS,
          { backgroundColor: "rgba(15, 153, 96, 0.15)" })
        }
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
