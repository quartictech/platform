import * as React from "react";
import { IDataset, IDatasetCoords, DatasetMap } from "../../models";
import _ = require("underscore");


interface IDatasetListProps {
  datasets: DatasetMap;
  selected: IDatasetCoords;
  onSelect: (string) => void;
  searchString: string;
};

export class DatasetList extends React.Component<IDatasetListProps, void> {
  render() {
    return (
      <div className="pt-card pt-elevation-4">
        <h3>Datasets</h3>

        <table className="pt-table pt-interactive pt-striped" style={{ width: "100%" }}>
          <thead>
            <tr>
            <th>Namespace</th>
            <th>Type</th>
            <th>Name</th>
            <th>Description</th>
            </tr>
          </thead>
          <tbody>
          {
            _.map(this.props.datasets, (datasets, namespace) => this.renderDatasetsInNamespace(namespace, datasets))
          }
          </tbody>
        </table>
      </div>
    );
  }

  private renderDatasetsInNamespace(namespace: string, datasets: { [id: string]: IDataset }) {
    return _
      .map(datasets, (dataset, id) => [id, dataset] as [string, IDataset])
      .filter(([,dataset]) => this.datasetVisible(dataset))
      .sort(([,a], [,b]) => comparison(a, b))
      .map(([id, dataset]) => <DatasetRow
        key={id}
        namespace={namespace}
        id={id}
        dataset={dataset}
        active={this.props.selected === <IDatasetCoords>{ id, namespace })
        onSelect={() => this.props.onSelect(id)}
      />)
  }

  private datasetVisible(dataset: IDataset) {
    return this.props.searchString == null || this.props.searchString.length === 0 ||
      _.some([dataset.metadata.name, dataset.metadata.description, dataset.locator.type],
        s => s.toLowerCase().includes(this.props.searchString));
  } 
}

const comparison = (a: IDataset, b: IDataset) => {
  const x = a.metadata.name.toLowerCase();
  const y = b.metadata.name.toLowerCase();
  return (x < y) ? -1 : (x > y) ? 1 : 0;
};

interface IDatasetRowProps {
  namespace: string;
  id: string;
  dataset: IDataset;
  active: boolean;
  onSelect: () => void;
}

export const DatasetRow = (props: IDatasetRowProps) => (
  <tr onClick={props.onSelect} style={props.active ? { fontWeight: "bold" } : {}}>
    <td>{props.namespace}</td>
    <td>{props.dataset.locator.type}</td>
    <td>{props.dataset.metadata.name}</td>
    <td>{props.dataset.metadata.description}</td>
  </tr>
);
