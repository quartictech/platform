import * as React from "react";
const DocumentTitle = require("react-document-title");  // TODO: wtf - doesn't work with import
import * as classNames from "classnames";
import * as _ from "underscore";
import { appHistory } from "../../routes";

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

const HIGHEST_DEFECT_ROADS: Entry[] = [
  {
    "rank": 1, "rsl": "D244/010_5E_01", "score": 11.044694762716798,
    "description": "HAMPTON LN TO RIVERDALE RD (MILLBOURNE RD)", "name": "MILLBOURNE ROAD",
    "length": 198.9099787,
  },
  {
    "rank": 2, "rsl": "D758/005_5E_01", "score": 10.984039245395659,
    "description": "JERSEY RD TO PEVENSEY CL (S)", "name": "PEVENSEY CLOSE",
    "length": 161.0227313,
  },
  {
    "rank": 3, "rsl": "D1181/050_5E_01", "score": 10.655653212181992,
    "description": "STAVELEY GRDS TO END OF RD (STAVELEY GRDS EB)", "name": "STAVELEY GARDENS",
    "length": 114.0014578,
  },
  {
    "rank": 4, "rsl": "D666/005_5E_01", "score": 10.323185274237224,
    "description": "MONTAGUE RD TO END OF RD", "name": "YORK ROAD",
    "length": 76.40083744,
  },
  {
    "rank": 5, "rsl": "D852/010_5E_01", "score": 10.270034203090121,
    "description": "RBT TREVOR CL TO RBT TREVOR CL", "name": "TREVOR CLOSE",
    "length":  30.54517578,
  },
  ];
const LOWEST_DEFECT_ROADS: Entry[] = [
    {
      "rsl": "D2027/015_3E_01", "score": 0.0,
      "description": "COUNTRY WAY TO JCT SUNBURY RD", "name": "FELTHAMHILL ROAD/SNAKEY LANE",
      "length": 1125.785353, "rank": 1,
    },
    {
      "rsl": "B3003/020_3E_01", "score": 0.0,
      "description": "BOUNDARY TO ASCOT RD", "name": "CLOCKHOUSE LANE",
      "length": 824.9326788, "rank": 2,
    },
    {
      "rsl": "B454/065_3D_01", "score": 0.0,
      "description": "JCT JERSEY RD TO BOUNDARY", "name": "WINDMILL LANE",
      "length": 788.9269841, "rank": 3,
    },
    {
      "rsl": "D440/505_4E_01", "score": 0.0,
      "description": "JOHNSON RD TO CRANFORD LN", "name": "BRABAZON ROAD",
      "length": 748.9370139, "rank": 4,
    },
    {
      "rsl": "D085/005_5E_01", "score": 0.0,
      "description": "BEDFONT LN TO JCT SVILLE CRES", "name": "SOUTHVILLE ROAD",
      "length": 614.6817878, "rank": 5,
    },
];

class InsightView extends React.Component<{}, {}> {

  render() {
    return (
      <DocumentTitle title="Quartic - Highest / lowest defects (2016)">
        <div>
          {this.renderPane("Highest defects (2016)", HIGHEST_DEFECT_ROADS,
            { backgroundColor: "rgba(219, 55, 55, 0.15)" })
          }
          {this.renderPane("Lowest defects (2016)", LOWEST_DEFECT_ROADS,
            { backgroundColor: "rgba(15, 153, 96, 0.15)" })
          }
        </div>
      </DocumentTitle>
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
              <th>Length (m)</th>
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
      <tr key={entry.rsl} style={style} onClick={() => appHistory.push(`/assets/${encodeURIComponent(entry.rsl)}`)}>
        <td>{entry.rank}</td>
        <td>{toTitleCase(entry.name)}</td>
        <td>{entry.rsl}</td>
        <td>{toTitleCase(entry.description)}</td>
        <td>{numeral(entry.score).format("0.00")}</td>
        <td>{entry.length ? numeral(entry.length).format("0.00") : null}</td>
      </tr>
    );
  }
}


export default InsightView;
