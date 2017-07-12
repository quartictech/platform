import * as React from "react";

import {
  Tab,
  Tabs,
  TabList,
  TabPanel,
  Button,
  Intent,
  Dialog,
  Classes,
} from "@blueprintjs/core";

import { IDataset, IDatasetCoords } from "../../models";
import _ = require("underscore");

interface IDatasetInfoProps {
  coords: IDatasetCoords;
  dataset: IDataset;
  deleteClick: Function;
};

interface IDatasetInfoState {
  isDeleteDialogOpen: boolean;
};

export class DatasetInfo extends React.Component<IDatasetInfoProps, IDatasetInfoState> {
  constructor() {
    super();
    this.state = {
      isDeleteDialogOpen: false,
    };
  }

  public render() {
    return (
      <div className="pt-card pt-elevation-4">
        <h3>{this.props.dataset.metadata.name}</h3>
        <h5 className="pt-monospace-text">{this.props.coords.namespace}::{this.props.coords.id}</h5>
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
              iconName="trash"
              intent={Intent.DANGER}
              onClick={() => this.setState({ isDeleteDialogOpen: true })}
            />
            <Dialog
              iconName="trash"
              onClose={() => this.setState({ isDeleteDialogOpen: false })}
              title={`Delete dataset: ${this.props.dataset.metadata.name}`}
              isOpen={this.state.isDeleteDialogOpen}
            >
              <div className={Classes.DIALOG_BODY}>
                Do you really want to delete this dataset?
              </div>
              <div className={Classes.DIALOG_FOOTER}>
                <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                  <Button
                    onClick={() => this.setState({ isDeleteDialogOpen: false })}
                    iconName=""
                  >
                    No
                  </Button>
                  <Button
                    intent={Intent.DANGER}
                    onClick={() => this.onYes()}
                  >
                    Yes
                  </Button>
                </div>
              </div>
            </Dialog>
          </TabPanel>
        </Tabs>
      </div>
    );
  }

  onYes() {
    this.setState({ isDeleteDialogOpen: false });
    this.props.deleteClick(this.props.coords);
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
            <td>{value && value.toString()}</td>
          </tr>
        ))
      }
    </tbody>
  </table>
);

const toUpperCase = (str: string) => {
  return str.substr(0, 1).toUpperCase() + str.substr(1);
};
