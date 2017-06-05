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

class HighestConcentration2016 extends React.Component<{}, {}> {
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
              {_.map(ENTRIES, (entry, idx) => this.row({ backgroundColor: "rgba(219, 55, 55, 0.15)" }, entry, idx))}
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

const ENTRIES: Entry[] = [
  {
    "description": "VICARAGE FARM RD TO JCT SPRINGWELL RD",
    "length": 350.7031564,
    "rsl": "C014/010_3E_01",
    "name": "CRANFORD LANE",
    "concentration": 36.0,
  },
  {
    "description": "ARMYTAGE RD TO HIGH STREET",
    "length": 855.7390217,
    "rsl": "C014/505_3E_01",
    "name": "CRANFORD LANE",
    "concentration": 34.34650455927052,
  },
  {
    "description": "RBT STAINES RD TO RBT BEDFONT RD",
    "length": 286.8025489,
    "rsl": "C002/005_3E_01",
    "name": "BEDFONT ROAD",
    "concentration": 29.090909090909093,
  },
  {
    "description": "ACTON LN TO CHISWICK RD",
    "length": 196.678923,
    "rsl": "A315/080_1B_01",
    "name": "CHISWICK HIGH ROAD",
    "concentration": 16.88659793814433,
  },
  {
    "description": "ISLAY GRDS TO GREEN LN",
    "length": 421.1536212,
    "rsl": "A315/500_1E_01",
    "name": "STAINES ROAD",
    "concentration": 16.81260945709282,
  },
  {
    "description": "START OF ISLAND (L SPLAY) TO FAIRHOLME",
    "length": 369.9509614,
    "rsl": "A315/460_1D_01",
    "name": "STAINES ROAD",
    "concentration": 16.033254156769594,
  },
  {
    "description": "SHAKESPEARE  AV TO WHITE BRIDGE CL",
    "length": 230.6075763,
    "rsl": "A315/440_1E_01",
    "name": "STAINES ROAD",
    "concentration": 13.457943925233643,
  },
  {
    "description": "RBT BEDFONT RD TO RBT STAINES RD",
    "length": 272.2763444,
    "rsl": "C002/015_3E_01",
    "name": "BEDFONT ROAD",
    "concentration": 12.292682926829269,
  },
  {
    "description": "NEW HESTON RD TO WHEATLANDS RD",
    "length": 247.1010359,
    "rsl": "A3005/130_2D_01",
    "name": "HESTON ROAD",
    "concentration": 10.805252725470764,
  },
  {
    "description": "CHESTNUT GROVE TO BRANTWOOD AV",
    "length": 171.5941357,
    "rsl": "A310/030_2E_01",
    "name": "TWICKENHAM ROAD",
    "concentration": 10.285714285714286,
  },
  {
    "description": "JCT WELLESLEY RD TO CHISWICK HIGH RD",
    "length": 273.2566462,
    "rsl": "A3063/510_3D_01",
    "name": "SUTTON LANE",
    "concentration": 9.561696045736065,
  },
  {
    "description": "BARRACK RD TO ISLAY GRDS",
    "length": 194.280132,
    "rsl": "A315/390_1E_01",
    "name": "STAINES ROAD",
    "concentration": 9.148524923702947,
  },
  {
    "description": "CRANFORD LN TO END OF RD (REDWOOD ESTATE)",
    "length": 234.145524,
    "rsl": "D2043/010_5E_01",
    "name": "REDWOOD ESTATE",
    "concentration": 8.928571428571427,
  },
  {
    "description": "TEESDALE AV TO JCT WOOD LN",
    "length": 349.4539186,
    "rsl": "A315/230_1E_01",
    "name": "LONDON ROAD",
    "concentration": 8.461178671655752,
  },
  {
    "description": "STAINES RD TO END OF SLIP (GROVE RD)",
    "length": 136.471232,
    "rsl": "A315/315_1E_01",
    "name": "GROVE ROAD",
    "concentration": 7.5,
  },
  {
    "description": "SUMMERHOUSE AV TO THE CROSSWAYS",
    "length": 259.2590268,
    "rsl": "D519/520_3D_01",
    "name": "VICARAGE FARM",
    "concentration": 7.248691820435141,
  },
  {
    "description": "REDFORD CL TO NR DEPOT",
    "length": 490.4018749,
    "rsl": "C001/005_3E_01",
    "name": "BEDFONT ROAD",
    "concentration": 7.205747616699994,
  },
  {
    "description": "BRIDGE RD TO JCT HALL RD",
    "length": 288.9816302,
    "rsl": "C021/005_3D_01",
    "name": "WORTON ROAD",
    "concentration": 6.875,
  },
  {
    "description": "SWAN RD TO HAMPTON LN (RIVERDALE RD)",
    "length": 411.4439958,
    "rsl": "D246/015_5E_01",
    "name": "RIVERDALE ROAD",
    "concentration": 6.481069042316258,
  },
  {
    "description": "HIBERNIA RD TO INVERNESS RD",
    "length": 313.2147536,
    "rsl": "A315/360_1E_01",
    "name": "STAINES ROAD",
    "concentration": 6.464669336692093,
  },
];

export default HighestConcentration2016;
