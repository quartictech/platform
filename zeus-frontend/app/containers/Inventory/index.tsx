import * as React from "react";
import { connect } from "react-redux";
import { Button, Classes, Intent } from "@blueprintjs/core";
import { Cell, Column, Table } from "@blueprintjs/table";

import { IDataset } from "../../models";

import { createStructuredSelector } from "reselect";
// import * as classNames from "classnames";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
const s = require("./style.css");

interface IProps {
  datasets: { [id: string]: IDataset };
  ui: any;
  createDataset: (any) => any;
  fetchDatasets: any;
  closeNewDatasetModal: any;
  deleteDataset: (string) => void;
}

interface IModel {
  name: string,
  manufacturer: string,
  snGen: () => string
};

interface IAsset {
  id: string;
  clazz: string;
  model: string;
  serial: string;
  manufacturer: string;
  purchaseDate: Date;
  lastInspectionDate: Date;
  lastInspectionSignoff: string;
  retirementDate: Date;
  location: string;
};

interface IState {
  assets: IAsset[];
};

class Inventory extends React.Component<IProps, IState> {
  public state : IState = {
    assets: this.generateAssets(),
  };

  render() {
    return (
      <div className={s.container}>
        <div className={s.main}>
          <Table
            isRowResizable={true}
            numRows={50}
          >
            <Column name="Asset #" renderCell={this.renderCell(x => x.id)} />
            <Column name="Asset class" renderCell={this.renderCell(x => x.clazz)} />
            <Column name="Model #" renderCell={this.renderCell(x => x.model)} />
            <Column name="Serial #" renderCell={this.renderCell(x => x.serial)} />
            <Column name="Manufacturer code" renderCell={this.renderCell(x => x.manufacturer)} />
            <Column name="Location" renderCell={this.renderCell(x => x.location)} />
            <Column name="Purchase date" renderCell={this.renderCell(x => this.dateToString(x.purchaseDate))} />
            <Column name="Last inspection date" renderCell={this.renderCell(x => this.dateToString(x.lastInspectionDate))} />
            <Column name="Last inspection signoff" renderCell={this.renderCell(x => x.lastInspectionSignoff)} />
            <Column name="Projected retirement date" renderCell={this.renderCell(x => this.dateToString(x.retirementDate))} />
          </Table>
        </div>

        <div className={s.right}>
          <div className="pt-button-group pt-vertical">
            <Button className={Classes.MINIMAL} intent={Intent.PRIMARY} iconName="annotation" text="Add note" />
            <Button className={Classes.MINIMAL} intent={Intent.PRIMARY} iconName="refresh" text="Add note" />
            <Button className={Classes.MINIMAL} intent={Intent.PRIMARY} iconName="refresh" text="Add note" />
          </div>
        </div>
      </div>
    );
  }

  private renderCell = (func: (IAsset) => string) => (row: number) => <Cell>{func(this.state.assets[row])}</Cell>

  private dateToString = (date: Date) => date.getFullYear() + "/" + (date.getMonth() + 1) + "/" + (date.getDay() + 1);

  private generateAssets() {
    const models: IModel[] = [
      { name: "S-5000C", snGen: () => Math.random().toString(36).slice(2, 10), manufacturer: "SIEM" },
      { name: "S-5000B", snGen: () => Math.random().toString(36).slice(2, 10), manufacturer: "SIEM" },
      { name: "QQ-19", snGen: () => Math.random().toString(10).slice(2, 12), manufacturer: "GE" },
      { name: "QQ-23", snGen: () => Math.random().toString(10).slice(2, 12), manufacturer: "GE" },
    ];

    var assets = new Array<IAsset>();
    for (var i = 0; i < 50; i++) {
      const model = models[Math.floor(Math.random() * models.length)]; 

      assets.push({
        id: "AB" + (Math.floor(Math.random() * 90000) + 10000),
        clazz: "Boiler",
        model: model.name,
        serial: model.snGen(),
        manufacturer: model.manufacturer,
        purchaseDate: this.randomDate(new Date(2003, 0, 1), new Date(2013, 0, 1)),
        lastInspectionDate: this.randomDate(new Date(2016, 0, 1), new Date(2017, 0, 1)),
        lastInspectionSignoff: ["J Nole", "A McFadden", "G Kemp", "B Wilson", "B Gee", "P Graham"][Math.floor(Math.random() * 6)],
        retirementDate: this.randomDate(new Date(2018, 0, 1), new Date(2020, 0, 1)),
        location: this.randomLocation()
      });
    }
    return assets;
  }

  private randomLocation() {
    return (Math.random() * (58.64 - 50.83) + 50.83).toFixed(3) + ", " + (Math.random() * (1.32 - -5.37) + -5.37).toFixed(3)
  }

  private randomDate(start: Date, end: Date) {
    return new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()));
  }
}

export { Inventory };

const mapDispatchToProps = {
  fetchDatasets: actions.fetchDatasets,
  createDataset: actions.createDataset,
  deleteDataset: actions.deleteDataset,
  closeNewDatasetModal: () => actions.setActiveModal(null as string)
};

const mapStateToProps = createStructuredSelector({
  datasets: selectors.selectDatasets,
  ui: selectors.selectUi
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Inventory);
