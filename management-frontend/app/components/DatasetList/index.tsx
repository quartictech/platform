import * as React from "react";
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
  <td>{props.dataset.metadata.attribution}</td>
  </tr>
)

export const DatasetList = (props: IDatasetListProps) => {console.log(props); return (
  <div>
  <table className="pt-table pt-striped">
    <thead>
      <tr>
      <th>Type</th>
      <th>Name</th>
      <th>Description</th>
      <th>Attribution</th>
      </tr>
    </thead>
    <tbody>
    {
      _.map(props.datasets, (dataset, id) => <DatasetRow id={id} dataset={dataset}/>)
    }
    </tbody>
  </table>
  </div>
)};
