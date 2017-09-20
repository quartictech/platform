import * as React from "react";
import {
  Classes,
  Intent,
  Button,
  Tooltip,
  Position,
  Icon,
} from "@blueprintjs/core";
import { IDataset, IDatasetMetadata, DatasetMap } from "../../models";
import * as classNames from "classnames";
import _ = require("underscore");
import { withRouter, InjectedRouter } from "react-router";

interface IDatasetListProps {
  datasets: DatasetMap;
  searchString: string;
  router?: InjectedRouter;
  showNewDatasetModal: any;
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

// tslint:disable-next-line:variable-name
const DatasetRow = (props: IDatasetRowProps) => (
  <tr onClick={() => props.onSelect()}>
    <td style={{ wordWrap: "break-word" }}>
        <b>{props.dataset.metadata.name}</b>
    </td>
    <td style={{ wordWrap: "break-word" }}>
      {maybeDescription(props.dataset.metadata)}
    </td>
    <td style={{ wordWrap: "break-word" }}>
      <code>
        {props.id}
      </code>
    </td>
  </tr>
);

const maybeDescription = (metadata: IDatasetMetadata) =>
  (metadata.name === metadata.description)
    ? null
    : <span>{metadata.description}</span>;

class DatasetListInner extends React.Component<IDatasetListProps, {}> {
  render() {
    return (
      <div>
        <h3>
          Datasets
          <Button
            style={{ marginLeft: "10px" }}
            text="Upload"
            className={Classes.MINIMAL}
            iconName="cloud-upload"
            intent={Intent.PRIMARY}
            onClick={this.props.showNewDatasetModal}
          />
        </h3>
        <div style={{ height: "100%", overflow: "auto" }}>
          <table
            className={classNames(Classes.TABLE, Classes.INTERACTIVE)}
            style={{ width: "100%", tableLayout: "fixed" }}
          >
            <thead>
              <tr>
                <th width="20%">Name</th>
                <th width="40%">Description</th>
                <th width="40%">Locator &nbsp;
                  <Tooltip
                    content="Use this locator in your pipeline code."
                    position={Position.RIGHT}
                    intent={Intent.PRIMARY}
                    useSmartPositioning={true}
                  >
                    <Icon
                      iconName="info-sign"
                      intent={Intent.PRIMARY}
                    />
                  </Tooltip>
                </th>
              </tr>
            </thead>
            <tbody>
             {this.datasetRows()}
            </tbody>
          </table>
        </div>
      </div>
    );
  }

  private datasetRows() {
    return _.map(
      this.props.datasets,
      (datasets, namespace) => this.renderDatasetsInNamespace(namespace, datasets),
    );
  }

  private renderDatasetsInNamespace(namespace: string, datasets: { [id: string]: IDataset }) {
    return _
      .map(datasets, (dataset, id) => [id, dataset] as [string, IDataset])
      .filter(([id, dataset]) => this.datasetVisible(namespace, id, dataset))
      .sort(([, a], [, b]) => comparison(a, b))
      .map(([id, dataset]) => (
        <DatasetRow
          key={id}
          namespace={namespace}
          id={id}
          dataset={dataset}
          active={false}
          onSelect={() => this.props.router.push(`/datasets/${namespace}/${encodeURIComponent(id)}`)}
        />
      ));
  }

  private datasetVisible(namespace: string, datasetId: string, dataset: IDataset) {
    return this.props.searchString == null || this.props.searchString.length === 0 ||
      _.some(
        [
          namespace,
          datasetId,
          dataset.metadata.name,
          dataset.metadata.description,
          dataset.locator.type,
        ],
        s => s.toLowerCase().includes(this.props.searchString),
      );
  }
}
// tslint:disable-next-line:variable-name
export const DatasetList = withRouter(DatasetListInner);
