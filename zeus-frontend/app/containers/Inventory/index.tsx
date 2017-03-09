import * as React from "react";
// import { connect } from "react-redux";
import { Button, Classes, Intent } from "@blueprintjs/core";
import { Cell, Column, Table } from "@blueprintjs/table";

// import { createStructuredSelector } from "reselect";
// import * as classNames from "classnames";
// import * as selectors from "../../redux/selectors";
// import * as actions from "../../redux/actions";
const s = require("./style.css");

interface IProps {
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
  filterColumn: number;
  filterValue: string;
};

interface IColumn {
  name: string;
  displayValue: (IAsset) => string;
};

const ENGINEERS = ["J Nole", "A McFadden", "G Kemp", "B Wilson", "B Gee", "P Graham"];

const MODELS: IModel[] = [
  { name: "S-5000C", snGen: () => Math.random().toString(36).slice(2, 10), manufacturer: "SIEM" },
  { name: "S-5000B", snGen: () => Math.random().toString(36).slice(2, 10), manufacturer: "SIEM" },
  { name: "QQ-19", snGen: () => Math.random().toString(10).slice(2, 12), manufacturer: "GE" },
  { name: "QQ-23", snGen: () => Math.random().toString(10).slice(2, 12), manufacturer: "GE" },
];

const COLUMNS: IColumn[] = [
  { name: "Asset #", displayValue: x => x.id },
  { name: "Asset class", displayValue: x => x.clazz },
  { name: "Model #", displayValue: x => x.model },
  { name: "Serial #", displayValue: x => x.serial },
  { name: "Manufacturer code", displayValue: x => x.manufacturer },
  { name: "Location", displayValue: x => x.location },
  { name: "Purchase date", displayValue: x => dateToString(x.purchaseDate) },
  { name: "Last inspection date", displayValue: x => dateToString(x.lastInspectionDate) },
  { name: "Last inspection signoff", displayValue: x => x.lastInspectionSignoff },
  { name: "Projected retirement date", displayValue: x => dateToString(x.retirementDate) },
];

const dateToString = (date: Date) => date.getFullYear() + "/" + (date.getMonth() + 1) + "/" + (date.getDay() + 1);

class Inventory extends React.Component<IProps, IState> {
  public state : IState = {
    assets: this.generateAssets(),
    filterColumn: -1,
    filterValue: ""
  };

  render() {
    return (
      <div className={s.container}>
        <div className={s.main}>

          <div className="pt-control-group">
            <div className="pt-select">
              <select value={this.state.filterColumn} onChange={e => this.setState({ filterColumn: +e.target.value })}>
                <option value="-1">Filter...</option>
                { COLUMNS.map((col, idx) => <option value={`${idx}`}>{col.name}</option>) }
              </select>
            </div>

            <input
              type="text"
              className="pt-input"
              placeholder="Value"
              disabled={this.state.filterColumn === -1}
              value={this.state.filterValue}
              onChange={e => this.setState({ filterValue: e.target.value })}
            />

            <Button
              intent={Intent.PRIMARY}
              text="Apply"
              disabled={this.state.filterColumn === -1 || this.state.filterValue === ""}
            />
          </div>

          <Table
            isRowResizable={true}
            numRows={50}
          >
            {
              COLUMNS.map(col => <Column name={col.name} renderCell={(row: number) => <Cell>{col.displayValue(this.state.assets[row])}</Cell>} />)
            }            
          </Table>
        </div>

        <div className={s.right}>

          <div className="pt-button-group pt-align left pt-vertical">
            <Button className={Classes.MINIMAL} intent={Intent.PRIMARY} iconName="annotation" text="Add note" />
            <Button className={Classes.MINIMAL} intent={Intent.PRIMARY} iconName="globe" text="View on map" />
            <Button className={Classes.MINIMAL} intent={Intent.PRIMARY} iconName="refresh" text="Add note" />
          </div>
        </div>
      </div>
    );
  }

  private generateAssets() {
    var assets = new Array<IAsset>();
    for (var i = 0; i < 50; i++) {
      const model = MODELS[Math.floor(Math.random() * MODELS.length)]; 

      assets.push({
        id: "AB" + (Math.floor(Math.random() * 90000) + 10000),
        clazz: "Boiler",
        model: model.name,
        serial: model.snGen(),
        manufacturer: model.manufacturer,
        purchaseDate: this.randomDate(new Date(2003, 0, 1), new Date(2013, 0, 1)),
        lastInspectionDate: this.randomDate(new Date(2016, 0, 1), new Date(2017, 0, 1)),
        lastInspectionSignoff: ENGINEERS[Math.floor(Math.random() * 6)],
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

export default Inventory;

// const mapDispatchToProps = {
//   fetchDatasets: actions.fetchDatasets,
//   createDataset: actions.createDataset,
//   deleteDataset: actions.deleteDataset,
//   closeNewDatasetModal: () => actions.setActiveModal(null as string)
// };

// const mapStateToProps = createStructuredSelector({
//   datasets: selectors.selectDatasets,
//   ui: selectors.selectUi
// });

// export default connect(
//   mapStateToProps,
//   mapDispatchToProps
// )(Inventory);
