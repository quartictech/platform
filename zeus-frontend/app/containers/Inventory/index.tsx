import * as React from "react";
import { connect } from "react-redux";
import {
  Button,
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
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
import * as _ from "underscore";
const s = require("./style.css");

interface IProps {
  createNote: (assetIds: string[], text: string) => void;
  assets: { [id : string]: IAsset };
}

interface IState {
  filterColumn: number;
  filterValue: string;
  filterInvert: boolean;
  selectedRows: number[];
  noteText: string;
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
  private filterAssets = (assets: { [id : string]: IAsset }, filterColumn: number, filterValue: string, filterInvert: boolean) => {
    if (filterColumn === -1 || filterValue === "") {
      return _.values(assets);
    }

    return _.filter(
      _.values(assets),
      asset => (COLUMNS[filterColumn].displayValue(asset).toLocaleLowerCase().indexOf(filterValue.toLocaleLowerCase()) !== -1) !== filterInvert
    );
  }

  private anyAssetsSelected = () => this.state.selectedRows.length > 0;

  public state : IState = {
    filterColumn: -1,
    filterValue: "",
    filterInvert: false,
    selectedRows: [],
    noteText: "",
  };

  render() {
    const filteredAssets = this.filterAssets(this.props.assets, this.state.filterColumn, this.state.filterValue, this.state.filterInvert );
    const selectedAssets = this.state.selectedRows.map(i => filteredAssets[i]);

    return (
        <div className={s.container}>
          <div className={s.main}>

            <div className="pt-control-group">
              <div className="pt-select">
                <select value={this.state.filterColumn} onChange={e => this.setState({ filterColumn: +e.target.value })}>
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
                onChange={e => this.setState({ filterValue: e.target.value })}
              />

              <Switch
                label="Invert"
                disabled={this.state.filterColumn === -1}
                checked={this.state.filterInvert}
                onChange={() => this.setState({ filterInvert: !this.state.filterInvert })}
              />
            </div>

            <Table
              isRowResizable={true}
              numRows={filteredAssets.length}
              selectionModes={SelectionModes.ROWS_AND_CELLS}
              onSelection={regions => this.setState({ selectedRows: this.calculateSelectedRows(regions) })}
              selectedRegionTransform={cellToRow}
            >
              {
                COLUMNS.map(col => <Column
                  key={col.name}
                  name={col.name}
                  renderCell={(row: number) => <Cell>{col.displayValue(filteredAssets[row])}</Cell>}
                />)
              }
            </Table>
          </div>

          <div className={s.right}>
            <h4>Notes</h4>

            {
              (selectedAssets.length === 1)
                ? (
                  selectedAssets[0].notes.map(note =>
                    <div key={note.id} className="pt-card pt-elevation-2">
                      <h5>{dateToString(note.created)}</h5>
                      <p>{note.text}</p>
                    </div>
                  )
                )
                : null
            }

            {
              this.anyAssetsSelected()
                ? (
                  <div className="pt-card pt-elevation-2">
                    <h5>New note</h5>
                    <textarea
                      id="note"
                      className="pt-input pt-intent-primary"
                      dir="auto"
                      style={{ width: "100%" }}
                      value={this.state.noteText}
                      onChange={(e) => this.setState({ noteText: e.target.value })}
                    />
                    <div style={{ textAlign: "right" }}>
                      <Button
                        intent={Intent.PRIMARY}
                        iconName="upload"
                        text="Submit"
                        disabled={this.state.noteText.trim().length === 0}
                        onClick={() => {
                          this.props.createNote(_.map(selectedAssets, a => a.id), this.state.noteText.trim());
                          this.setState({ noteText: "" });
                        }}
                      />
                    </div>
                  </div>
                )
                : null
            }

          </div>
        </div>
    );
  }

  private calculateSelectedRows = (regions: IRegion[]) => 
    _.chain(regions)
      .map(r => _.range(r.rows[0], r.rows[1] + 1))
      .flatten()
      .uniq()
      .value();
}

const dateToString = (date: Date) => date.getFullYear() + "/" + formatDateComponent(date.getMonth() + 1) + "/" + formatDateComponent(date.getDate());
const formatDateComponent = (x: number) => ((x < 10) ? "0" : "") + x;

const cellToRow = (region) => Regions.row(region.rows[0], region.rows[1]);


const mapDispatchToProps = {
  createNote: actions.createNote,
};

const mapStateToProps = createStructuredSelector({
  assets: selectors.selectAssets,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Inventory);
