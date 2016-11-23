import * as React from "react";
import { Link } from "react-router";

import { IDataset } from "../../models";
import _ = require("underscore");

interface IDatasetListProps {
  datasets: { [id: string]: IDataset };
};

interface IDatasetRowProps {
  id: string;
  dataset: IDataset;
}

export const DatasetRow = (props: IDatasetRowProps) => (
  <tr key={props.id}>
  <td>{props.dataset.locator.type}</td>
  <td>{props.dataset.metadata.name}</td>
  <td>{props.dataset.metadata.description}</td>
  <td><Link to={`/dataset/${props.id}`}>View</Link></td>
  </tr>
);

export const DatasetList = (props: IDatasetListProps) => (
  <table className="pt-table pt-striped" style={{ width: "100%" }}>
    <thead>
      <tr>
      <th>Type</th>
      <th>Name</th>
      <th>Description</th>
      </tr>
    </thead>
    <tbody>
    {
      _.map(props.datasets, (dataset, id) => <DatasetRow
        key={id}
        id={id}
        dataset={dataset}
      />)
    }
    </tbody>
  </table>
);
