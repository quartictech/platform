import * as React from "react";

import { BarChart } from "../BarChart";
import { TimeChart } from "../TimeChart";
import * as _ from "underscore";

// tslint:disable-next-line:variable-name
const InsightSummary = ({ insight, assets }) => (
      <div>
      { insight.insightType === "failure"
        ? <div><TimeChart yLabel="Voltage" events={assets[_.first(insight.assetIds) as string].events} /></div>
        : null
      }
      { insight.barChart ? (<div>
        <BarChart data={insight.barChart.data} xLabel={insight.barChart.xLabel} yLabel={insight.barChart.yLabel} />
      </div>) : null}
      </div>
     );

export { InsightSummary };
