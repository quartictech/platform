import * as React from "react";
// import { Link } from "react-router";

import * as Blueprint from "@blueprintjs/core";
const { Button } = Blueprint;

import { IDataset } from "../../models";
import _ = require("underscore");

interface IDatasetListProps {
  datasets: { [id: string]: IDataset };
  onSelect: (String) => void;
};

export const DatasetList = (props: IDatasetListProps) => (
  <table className="pt-table pt-interactive pt-striped" style={{ width: "100%" }}>
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
        onSelect={() => props.onSelect(id)}
      />)
    }
    </tbody>
  </table>
);

interface IDatasetRowProps {
  id: string;
  dataset: IDataset;
  onSelect: () => void;
}

export const DatasetRow = (props: IDatasetRowProps) => (
  <tr key={props.id}>
  <td>{props.dataset.locator.type}</td>
  <td>{props.dataset.metadata.name}</td>
  <td>{props.dataset.metadata.description}</td>
  <td>
    <Button onClick={props.onSelect}>
      Details
    </Button>
  </td>
  </tr>
);
