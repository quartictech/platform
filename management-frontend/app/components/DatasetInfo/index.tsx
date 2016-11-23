import * as React from "react";

// import * as Blueprint from "@blueprintjs/core";

import { IDataset } from "../../models";
// import _ = require("underscore");

interface IDatasetInfoProps {
  dataset: IDataset;
};

export const DatasetInfo = (props: IDatasetInfoProps) => (
  <div>
    <h2>London Boroughs</h2>
    Some info.

    <pre>
      {JSON.stringify(props.dataset, null, '  ')}
    </pre>
  </div>
);
