import * as React from "react";
import {
  Classes,
} from "@blueprintjs/core";
import * as classNames from "classnames";
import HighestLowest2016 from "./HighestLowest2016";
import HighestConcentration2016 from "./HighestConcentration2016";
import Flytipping from "./Flytipping";

export interface InsightType {
  componentClass: any;
  title: string;
  description?: JSX.Element;
  disabled: boolean;
}

const insights: { [name: string] : InsightType } = {
  highestLowest2016: {
    componentClass: HighestLowest2016,
    title: "Best / worst roads",
    disabled: false,
    description: (
      <div>
        RSLs ranked by defects per metre (based on 2016 DVI survey results).
      </div>
    ),
  },
  highestConcentration2016: {
    componentClass: HighestConcentration2016,
    title: "Panel patching opportunities",
    disabled: false,
    description: (
      <div>
        <p>
          RSLs due to be treated in 2017-2018 (according to AOM model output), with concentrated defects.
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
  flytipping: {
    componentClass: Flytipping,
    title: "Flytipping",
    disabled: false,
    description: (
      <div>
        Flytipping trends in Hounslow.
        <div className={classNames(Classes.CALLOUT, Classes.iconClass("warning-sign"), Classes.INTENT_DANGER)}>
          This insight requires a high-bandwidth network connection, and may fail to display otherwise.
        </div>
      </div>
    ),
  },
  predictions2017: {
    componentClass: null,
    title: "Predictions (2017)",
    disabled: true,
  },
  smartOps: {
    componentClass: null,
    title: "SmartOps",
    disabled: true,
  },
};

export default insights;
