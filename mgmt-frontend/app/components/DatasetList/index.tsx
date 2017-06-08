import * as React from "react";
import { Classes } from "@blueprintjs/core";
import { IDataset, IDatasetCoords, DatasetMap } from "../../models";
import * as classNames from "classnames";
import _ = require("underscore");


interface IDatasetListProps {
  datasets: DatasetMap;
  selected: IDatasetCoords;
  onSelect: (string) => void;
  searchString: string;
  selectedNamespace: string;
};

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

const DatasetRow = (props: IDatasetRowProps) => (
  <tr onClick={props.onSelect} style={props.active ? { fontWeight: "bold" } : {}}>
    <td style={{ wordWrap: "break-word" }}>{props.namespace}</td>
    <td style={{ wordWrap: "break-word" }}>{props.id}</td>
    <td style={{ wordWrap: "break-word" }}>{props.dataset.locator.type}</td>
    <td style={{ wordWrap: "break-word" }}>{props.dataset.metadata.name}</td>
    <td style={{ wordWrap: "break-word" }}>{props.dataset.metadata.description}</td>
  </tr>
);

export class DatasetList extends React.Component<IDatasetListProps, void> {
  render() {
    return (
      <div className={classNames(Classes.CARD, Classes.ELEVATION_4)}>
        <h3>Datasets</h3>

        <table
          className={classNames(Classes.TABLE, Classes.INTERACTIVE, Classes.TABLE_STRIPED, Classes.TABLE_CONDENSED)}
          style={{ width: "100%", tableLayout: "fixed" }}
        >
          <thead>
            <tr>
            <th width="10%">Namespace</th>
            <th width="20%">Id</th>
            <th width="10%">Type</th>
            <th width="30%">Name</th>
            <th width="30%">Description</th>
            </tr>
          </thead>
          <tbody>
          {
              _.map(this.props.datasets,
                (datasets, namespace) => !this.props.selectedNamespace || this.props.selectedNamespace === namespace ?
                  this.renderDatasetsInNamespace(namespace, datasets) : null)
          }
          </tbody>
        </table>
      </div>
    );
  }

  private renderDatasetsInNamespace(namespace: string, datasets: { [id: string]: IDataset }) {
    return _
      .map(datasets, (dataset, id) => [id, dataset] as [string, IDataset])
      .filter(([id, dataset]) => this.datasetVisible(namespace, id, dataset))
      .sort(([, a], [, b]) => comparison(a, b))
      .map(([id, dataset]) => <DatasetRow
        key={id}
        namespace={namespace}
        id={id}
        dataset={dataset}
        active={this.props.selected === { id, namespace }}
        onSelect={() => this.props.onSelect({ namespace, id })}
      />);
  }

  private datasetVisible(namespace: string, datasetId: string, dataset: IDataset) {
    return this.props.searchString == null || this.props.searchString.length === 0 ||
      _.some([namespace, datasetId, dataset.metadata.name, dataset.metadata.description, dataset.locator.type],
        s => s.toLowerCase().includes(this.props.searchString));
  }
}

