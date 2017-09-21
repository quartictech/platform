import * as React from "react";
import {
  Classes,
  Intent,
  Button,
  Tooltip,
  Position,
  Icon,
  IconClasses,
  InputGroup,
} from "@blueprintjs/core";
import { IDataset, IDatasetMetadata, DatasetMap } from "../../models";
import * as classNames from "classnames";
import _ = require("underscore");
import { withRouter, InjectedRouter } from "react-router";
import * as CopyToClipboard from "react-copy-to-clipboard";

const s = require("./style.css");

interface IDatasetListProps {
  datasets: DatasetMap;
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

interface IDatasetListState {
  searchString: string;
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
      <Tooltip
        content="Use the locator in your pipeline code. Click to copy."
        position={Position.RIGHT}
        intent={Intent.PRIMARY}
        useSmartPositioning={true}
      >
        <CopyToClipboard text={props.id}>
          <code onClick={(e) => {e.stopPropagation();}}>
            {props.id}
          </code>
        </CopyToClipboard>
      </Tooltip>
    </td>
  </tr>
);

const maybeDescription = (metadata: IDatasetMetadata) =>
  (metadata.name === metadata.description)
    ? null
    : <span>{metadata.description}</span>;

class DatasetListInner extends React.Component<IDatasetListProps, IDatasetListState> {
  constructor() {
    super();
    this.state = { searchString: "" };
  }

  onSearch(e) {
    this.setState({ searchString: e.target.value });
  }

  noDatasets = () => (
    <div>
      <div className={classNames(Classes.NON_IDEAL_STATE, s.noItems)}>
        <div className={classNames(Classes.NON_IDEAL_STATE_VISUAL, Classes.NON_IDEAL_STATE_ICON)}>
          <span className={classNames(Classes.ICON, IconClasses.LIGHTBULB)}/>
        </div>
        <h4 className={Classes.NON_IDEAL_STATE_TITLE}>You haven't created any datasets.</h4>
        <div className={Classes.NON_IDEAL_STATE_DESCRIPTION}>
          Run a build or <a onClick={this.props.showNewDatasetModal}> upload a file</a> to get started.
        </div>
      </div>
    </div>
  )

  private datasetRows() {
    return _.map(
      this.props.datasets,
      (datasets, namespace) => this.datasetsInNamespace(namespace, datasets),
    );
  }

  private datasetsInNamespace(namespace: string, datasets: { [id: string]: IDataset }) {
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

  datasetsTable = () => (
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
              content="Use the locator in your pipeline code. Click to copy."
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
  )

  datasetsView() {
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
          <InputGroup
            leftIconName="search"
            className={classNames(Classes.ROUND, s.searchBox)}
            placeholder="Search datasets..."
            onChange={e => this.onSearch(e)}
          />
          {this.datasetsTable()}
        </h3>
      </div>
    );
  }

  render() {
    return (
    <div className={s.main}>
      {_.isEmpty(this.props.datasets) ? this.noDatasets() : this.datasetsView()}
    </div>
    );
  }


  private datasetVisible(namespace: string, datasetId: string, dataset: IDataset) {
    return this.state.searchString == null || this.state.searchString.length === 0 ||
      _.some(
        [
          namespace,
          datasetId,
          dataset.metadata.name,
          dataset.metadata.description,
          dataset.locator.type,
        ],
        s => s.toLowerCase().includes(this.state.searchString),
      );
  }
}
// tslint:disable-next-line:variable-name
export const DatasetList = withRouter(DatasetListInner);
