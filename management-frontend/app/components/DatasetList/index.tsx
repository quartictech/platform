import * as React from "react";

import { IDataset } from "../../models";
import _ = require("underscore");

interface IDatasetListProps {
  datasets: { [id: string]: IDataset };
  selectedId: string;
  onSelect: (string) => void;
  searchString: string;
};

export const DatasetList = (props: IDatasetListProps) => (
  <div className="pt-card pt-elevation-4">
    <h3>Datasets</h3>

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
        _.map(props.datasets, (dataset, id) => [id, dataset] as [string, IDataset])
        .filter(([,dataset]) =>
          props.searchString == null || props.searchString.length === 0 ||
          dataset.metadata.name.toLowerCase().includes(props.searchString))
        .map(([id, dataset]) => <DatasetRow
          key={id}
          id={id}
          dataset={dataset}
          active={props.selectedId === id}
          onSelect={() => props.onSelect(id)}
        />)
      }
      </tbody>
    </table>
  </div>
);

interface IDatasetRowProps {
  id: string;
  dataset: IDataset;
  active: boolean;
  onSelect: () => void;
}

export const DatasetRow = (props: IDatasetRowProps) => (
  <tr key={props.id} onClick={props.onSelect} style={props.active ? { fontWeight: "bold" } : {}}>
    <td>{props.dataset.locator.type}</td>
    <td>{props.dataset.metadata.name}</td>
    <td>{props.dataset.metadata.description}</td>
  </tr>
);
