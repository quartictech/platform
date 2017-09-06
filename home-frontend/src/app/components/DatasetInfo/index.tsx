import * as React from "react";
import * as moment from "moment";

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
}

interface IDatasetInfoState {
  isDeleteDialogOpen: boolean;
}

export class DatasetInfo extends React.Component<IDatasetInfoProps, IDatasetInfoState> {
  constructor() {
    super();
    this.state = {
      isDeleteDialogOpen: false,
    };
  }

  public render() {
    return (
      <div>
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
            <div>
              <PropertiesTable props={this.props.dataset.metadata} />
            </div>
          </TabPanel>
          <TabPanel>
            <div>
              <PropertiesTable props={this.props.dataset.locator} />
            </div>
          </TabPanel>
          <TabPanel>
            <pre>
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
}

interface IPropertiesTableProps {
  props: {[key: string]: any};
}

// tslint:disable-next-line:variable-name
const PropertiesTable = (props: IPropertiesTableProps) => (
  <table className="pt-table pt-condensed">
    <tbody>
      {_.map(props.props, (value, key) => <PropertiesRow key={key} propertyKey={key} value={value} />)}
    </tbody>
  </table>
);

// tslint:disable-next-line:variable-name
const PropertiesRow = ({ propertyKey, value }) => {
  let formattedValue = value;
  if (propertyKey === "registered") {
    formattedValue = moment.unix(value);
  }
  return (
  <tr>
    <td style={{ fontWeight: "bold" }}>{toUpperCase(propertyKey)}</td>
    <td>{formattedValue && formattedValue.toString()}</td>
  </tr>
  );
};

const toUpperCase = (str: string) => {
  return str.substr(0, 1).toUpperCase() + str.substr(1);
};
