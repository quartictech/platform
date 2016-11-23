import * as React from "react";

import { Button, Collapse } from "@blueprintjs/core";

import { IDataset } from "../../models";
import _ = require("underscore");

interface IDatasetInfoProps {
  id: String;
  dataset: IDataset;
};

interface IDatasetInfoState {
  isOpen: boolean;
};

export class DatasetInfo extends React.Component<IDatasetInfoProps, IDatasetInfoState> {
  public state : IDatasetInfoState = {
    isOpen: false,
  };

  public render() {
    return (
      <div className="pt-card pt-elevation-4">
        <h3>{this.props.dataset.metadata.name}</h3>

        <div className="pt-callout pt-intent-primary pt-elevation-2">
          <h5>ID: {this.props.id}</h5>
        </div>

        <p />

        <div className="pt-callout pt-intent-primary pt-elevation-2">
          <h5>Metadata</h5>
          <PropertiesTable props={this.props.dataset.metadata} />
        </div>

        <p />

        <div className="pt-callout pt-intent-primary pt-elevation-2">
          <h5>Locator</h5>
          <PropertiesTable props={this.props.dataset.locator} />
        </div>

        <p />

        <Button className="pt-intent-primary" onClick={this.handleClick}>
          {this.state.isOpen ? "Hide" : "Show"} raw configuration
        </Button>

        <Collapse isOpen={this.state.isOpen}>
          <pre className="pt-elevation-2">
            {JSON.stringify(this.props.dataset, null, '  ')}
          </pre>
        </Collapse>
      </div>
    );
  }

  private handleClick = () => {
    this.setState({isOpen: !this.state.isOpen});
  }
};

interface IPropertiesTableProps {
  props: {[key: string]: any};
}

const PropertiesTable = (props: IPropertiesTableProps) => (
  <table className="pt-table pt-condensed">
    <tbody>
      {
        _.map(props.props, (value, key) => (
          <tr key={key}>
            <td style={{ fontWeight: "bold" }}>{toUpperCase(key)}</td>
            <td>{value}</td>
          </tr>
        ))
      }
    </tbody>
  </table>
);

const toUpperCase = (str: string) => {
  return str.substr(0, 1).toUpperCase() + str.substr(1);
}
