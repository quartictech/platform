import * as React from "react";
import { connect } from "react-redux";
import { Button, Classes, Intent, Switch } from "@blueprintjs/core";
import { Cell, Column, IRegion, SelectionModes, Table } from "@blueprintjs/table";

import { IAsset } from "../../models";
import { createStructuredSelector } from "reselect";
// import * as classNames from "classnames";
import * as selectors from "../../redux/selectors";
// import * as actions from "../../redux/actions";
const s = require("./style.css");

interface IProps {
  ui: any;
  assets: IAsset[];
}

interface IState {
  filteredAssets: IAsset[];
  filterColumn: number;
  filterValue: string;
  filterInvert: boolean;
};

interface IColumn {
  name: string;
  displayValue: (IAsset) => string;
};

const COLUMNS: IColumn[] = [
  { name: "Asset #", displayValue: x => x.id },
  { name: "Asset class", displayValue: x => x.clazz },
  { name: "Model #", displayValue: x => x.model.name },
  { name: "Serial #", displayValue: x => x.serial },
  { name: "Manufacturer code", displayValue: x => x.model.manufacturer },
  { name: "Location", displayValue: x => x.location },
  { name: "Purchase date", displayValue: x => dateToString(x.purchaseDate) },
  { name: "Last inspection date", displayValue: x => dateToString(x.lastInspectionDate) },
  { name: "Last inspection signoff", displayValue: x => x.lastInspectionSignoff },
  { name: "Projected retirement date", displayValue: x => dateToString(x.retirementDate) },
];

const dateToString = (date: Date) => date.getFullYear() + "/" + (date.getMonth() + 1) + "/" + (date.getDay() + 1);

class Inventory extends React.Component<IProps, IState> {
  public state : IState = {
    filteredAssets: this.filterAssets(-1, "", false),  // We materialise rather than using a view due to the inefficient way that Blueprint Table works
    filterColumn: -1,
    filterValue: "",
    filterInvert: false,
  };

  render() {
    return (
      <div className={s.container}>
        <div className={s.main}>

          <div className="pt-control-group">
            <div className="pt-select">
              <select value={this.state.filterColumn} onChange={e => this.setState({
                  filterColumn: +e.target.value,
                  filteredAssets: this.filterAssets(+e.target.value, this.state.filterValue, this.state.filterInvert),
                })}>
                <option value="-1">Filter...</option>
                { COLUMNS.map((col, idx) => <option key={col.name} value={`${idx}`}>{col.name}</option>) }
              </select>
            </div>

            <input
              type="text"
              className="pt-input"
              placeholder="Value"
              disabled={this.state.filterColumn === -1}
              value={this.state.filterValue}
              onChange={e => this.setState({
                filterValue: e.target.value,
                filteredAssets: this.filterAssets(this.state.filterColumn, e.target.value, this.state.filterInvert),
              })}
            />

            <Switch
              label="Invert"
              disabled={this.state.filterColumn === -1}
              checked={this.state.filterInvert}
              onChange={() => this.setState({
                filterInvert: !this.state.filterInvert,
                filteredAssets: this.filterAssets(this.state.filterColumn, this.state.filterValue, !this.state.filterInvert),
              })}
            />
          </div>

          <Table
            isRowResizable={true}
            numRows={this.state.filteredAssets.length}
            selectionModes={SelectionModes.ROWS_ONLY}
            onSelection={regions => console.log("Selected:", calculateExtractedRows(regions))}
          >
            {
              COLUMNS.map(col => <Column
                key={col.name}
                name={col.name}
                renderCell={(row: number) => <Cell>{col.displayValue(this.state.filteredAssets[row])}</Cell>}
              />)
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
  
  private filterAssets(filterColumn: number, filterValue: string, filterInvert: boolean) {
    if (filterColumn === -1 || filterValue === "") {
      return this.props.assets;
    }

    const lowerCaseFilterValue = filterValue.toLocaleLowerCase();
    return this.props.assets.filter(asset => (COLUMNS[filterColumn].displayValue(asset).toLocaleLowerCase().indexOf(lowerCaseFilterValue) !== -1) !== filterInvert);
  }
}

const calculateExtractedRows = (regions: IRegion[]) => regions.map(r => r.rows[0]);   // Expecting only one row per region

const mapDispatchToProps = {
};

const mapStateToProps = createStructuredSelector({
  assets: selectors.selectAssets,
  ui: selectors.selectUi
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Inventory);
