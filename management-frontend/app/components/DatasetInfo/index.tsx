import * as React from "react";

import { Tab, Tabs, TabList, TabPanel, Button, Intent } from "@blueprintjs/core";

import { IDataset } from "../../models";
import _ = require("underscore");

interface IDatasetInfoProps {
  id: String;
  dataset: IDataset;
  deleteClick: Function;
};

export class DatasetInfo extends React.Component<IDatasetInfoProps, {}> {

  public render() {
    return (
      <div className="pt-card pt-elevation-4">
        <h3>{this.props.dataset.metadata.name}</h3>
        <h5>ID: {this.props.id}</h5>

        <Tabs>
          <TabList>
            <Tab>Metadata</Tab>
            <Tab>Locator</Tab>
            <Tab>Raw</Tab>
            <Tab>Admin</Tab>
          </TabList>
          <TabPanel>
            <div className="pt-callout pt-intent-primary pt-elevation-2">
              <PropertiesTable props={this.props.dataset.metadata} />
            </div>
          </TabPanel>
          <TabPanel>
            <div className="pt-callout pt-intent-primary pt-elevation-2">
              <PropertiesTable props={this.props.dataset.locator} />
            </div>
          </TabPanel>
          <TabPanel>
            <pre className="pt-elevation-2">
              {JSON.stringify(this.props.dataset, null, "  ")}
            </pre>
          </TabPanel>
          <TabPanel>
            <Button
              className="pt-large"
              text="Delete"
              intent={Intent.DANGER}
              onClick={() => this.props.deleteClick(this.props.id)}
            />
          </TabPanel>
        </Tabs>
      </div>
    );
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
};
