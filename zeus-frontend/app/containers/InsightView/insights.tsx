import * as React from "react";
import HighestLowest2016 from "./highestLowest2016";
import HighestConcentration2016 from "./highestConcentration2016";

export interface Insight {
  componentClass: any;
  title: string;
  description?: JSX.Element;
  disabled: boolean;
}

const insights: { [name: string] : Insight } = {
  "highestLowest2016": {
    componentClass: HighestLowest2016,
    title: "Highest / lowest defects (2016)",    
    disabled: false,
    description: (
      <div>
        RSLs ranked by defects per metre (based on 2016 DVI survey results).
      </div>
    ),
  },
  "highestConcentration2016": {
    componentClass: HighestConcentration2016,
    title: "Upcoming treatments with highest defect concentration (2016)",
    disabled: false,
    description: (
      <div>
        <p>
          RSLs due to be treated in 2017-2018 (according to AOM model output), with anomalous defect concentration.
        </p>
        <p>
          Defect concentration is a measure of how defects are distributed across the RSL surface:
        </p>
        <ul>
          <li><b>High concentration</b> &mdash; defects are relatively clustered.</li>
          <li><b>Low concentration</b> &mdash; defects are relatively spread out.</li>
        </ul>
        <p>
          For long RSLs with high concentration, it may be more cost-effective to treat only the defective chainage
          lengths, rather than the entire surface.
        </p>
      </div>
    ),
  },
  "predictions2017": {
    componentClass: null,
    title: "Predictions (2017)",
    disabled: true,
  },
  "smartOps": {
    componentClass: null,
    title: "SmartOps",
    disabled: true,
  },
};

export default insights;
