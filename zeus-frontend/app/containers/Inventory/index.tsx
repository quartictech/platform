import * as React from "react";
import { connect } from "react-redux";
import {
  Button,
  Classes,
  Intent,
  Switch,
} from "@blueprintjs/core";
import {
  Cell,
  Column,
  IRegion,
  Regions,
  SelectionModes,
  Table,
} from "@blueprintjs/table";

import { IAsset } from "../../models";
import { createStructuredSelector } from "reselect";
// import * as classNames from "classnames";
import * as selectors from "../../redux/selectors";
// import * as actions from "../../redux/actions";
import * as _ from "underscore";
const s = require("./style.css");

interface IProps {
  assets: IAsset[];
}

interface IState {
  filteredAssets: IAsset[];
  filterColumn: number;
  filterValue: string;
  filterInvert: boolean;
  selectedRows: number[];
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

class Inventory extends React.Component<IProps, IState> {
  public state : IState = {
    filteredAssets: this.filterAssets(-1, "", false),  // We materialise rather than using a view due to the inefficient way that Blueprint Table works
    filterColumn: -1,
    filterValue: "",
    filterInvert: false,
    selectedRows: [],
  };

  render() {
    console.log("Selected rows", this.state.selectedRows);
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
            selectionModes={SelectionModes.ROWS_AND_CELLS}
            onSelection={regions => this.setState({ selectedRows: calculateSelectedRows(regions) })}
            selectedRegionTransform={cellToRow}
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
          <h4>Actions</h4>

          <div className="pt-button-group pt-align-left">
            <Button className={Classes.MINIMAL} intent={Intent.PRIMARY} iconName="annotation" text="Add note" />
            <Button className={Classes.MINIMAL} intent={Intent.PRIMARY} iconName="globe" text="View on map" />
          </div>

          <h4>Notes</h4>

          {
            (this.state.selectedRows.length === 1)
              ? (
                this.state.filteredAssets[this.state.selectedRows[0]].notes.map(note =>
                  <div className="pt-card pt-elevation-2">
                    <h5>{dateToString(note.created)}</h5>
                    <p>{note.text}</p>
                  </div>
                )
              )
              : null
          }

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

const dateToString = (date: Date) => date.getFullYear() + "/" + formatDateComponent(date.getMonth() + 1) + "/" + formatDateComponent(date.getDay() + 1);
const formatDateComponent = (x: number) => ((x < 10) ? "0" : "") + x;

const cellToRow = (region) => Regions.row(region.rows[0], region.rows[1]);

const calculateSelectedRows = (regions: IRegion[]) => _.uniq(_.flatten(_.map(regions, r => _.range(r.rows[0], r.rows[1] + 1))));

const mapDispatchToProps = {
};

const mapStateToProps = createStructuredSelector({
  assets: selectors.selectAssets,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Inventory);
