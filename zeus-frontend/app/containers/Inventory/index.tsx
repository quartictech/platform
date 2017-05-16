import * as React from "react";
import { connect } from "react-redux";
import {
  Button,
  Intent,
  Switch,
  Tag,
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
import { toaster } from "../../containers/App/toaster";
import * as _ from "underscore";
const s = require("./style.css");

interface IProps {
  createNote: (assetIds: string[], text: string) => void;
  assets: { [id : string]: IAsset };
  location?: {
    query?: { [key : string]: string };
  };
}

const enum DialogMode {
   None,
   AddNote,
   ScheduleMaintenance,
};

interface IState {
  filterColumn: number;
  filterValue: string;
  filterInvert: boolean;
  selectedRows: number[];
  dialogMode: DialogMode;
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
  { name: "Location", displayValue: x => `${x.location.lat},${x.location.lon}`},
  { name: "Purchase date", displayValue: x => dateToString(x.purchaseDate) },
  { name: "Last inspection date", displayValue: x => dateToString(x.lastInspectionDate) },
  { name: "Last inspection signoff", displayValue: x => x.lastInspectionSignoff },
  { name: "Projected retirement date", displayValue: x => dateToString(x.retirementDate) },
];

class Inventory extends React.Component<IProps, IState> {
  private filterAssets = (
    assets: { [id : string]: IAsset },
    _filterColumn: number,
    _filterValue: string,
    _filterInvert: boolean) => {
    // HACK!!!
    const MAGIC_ASSET_IDS = ["AB74476", "AB29632", "AB65062", "AB10711"];
    return _.filter(_.values(assets), asset => MAGIC_ASSET_IDS.indexOf(asset.id) > -1);
    // if (filterColumn === -1 || filterValue === "") {
    //   return _.values(assets);
    // }

    // return _.filter(
    //   _.values(assets),
    //   asset => (COLUMNS[filterColumn].displayValue(asset).toLocaleLowerCase()
    //    .indexOf(filterValue.toLocaleLowerCase()) !== -1) !== filterInvert
    // );
  }

  state : IState = this.initialState();

  initialState(): IState {
    const firstKey = _.first(_.keys(this.props.location.query)) || "";
    const firstValue = this.props.location.query[firstKey];

    const columnIndex = _.findIndex(COLUMNS, c => c.name.toLocaleLowerCase() === firstKey.toLocaleLowerCase());

    return {
      filterColumn: columnIndex,
      filterValue: columnIndex ? firstValue : "",
      filterInvert: false,
      selectedRows: [],
      dialogMode: DialogMode.None,
      noteText: "",
    };
  }

  render() {
    const filteredAssets = this.filterAssets(
      this.props.assets, this.state.filterColumn, this.state.filterValue, this.state.filterInvert
    );
    const selectedAssets = this.state.selectedRows.map(i => filteredAssets[i]);

    return (
        <div className={s.container}>
          <div className={s.main}>

            <div className="pt-control-group">
              <div className="pt-select">
                <select
                  value={this.state.filterColumn}
                  onChange={e => this.setState({ filterColumn: +e.target.value })}
                >
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
              this.uniqueNotes(selectedAssets).map(note =>
                <div
                  key={note.id}
                  className="pt-callout pt-elevation-2 pt-intent-warning"
                  style={{ marginBottom: "10px" }}
                >
                  <h5>{dateToString(note.created)}</h5>
                  <p>{note.text}</p>
                  <p style={{ textAlign: "right" }}>
                    <Tag intent={Intent.WARNING}>Serious</Tag>
                  </p>
                </div>
              )
            }

            {
              this.renderDialog(selectedAssets)
            }

          </div>
        </div>
    );
  }

  private renderDialog(selectedAssets: IAsset[]) {
    if (selectedAssets.length === 0) {
      return null;
    }

    switch (this.state.dialogMode) {
      case DialogMode.None:
        return (
          <div className="pt-button-group pt-vertical pt-align-left">
            <Button
              iconName="annotation"
              text="Add note"
              onClick={() => this.setState({
                dialogMode: DialogMode.AddNote,
                noteText: "",
              })}
            />
            <Button
              iconName="calendar"
              text="Schedule maintenance"
              onClick={() => this.setState({
                dialogMode: DialogMode.ScheduleMaintenance,
                noteText: "",
              })}
            />
          </div>
        );

      case DialogMode.AddNote:
        return (
          <div className="pt-callout pt-elevation-2">
            <h5>New note</h5>
            <textarea
              id="note"
              className="pt-input pt-intent-primary"
              dir="auto"
              rows={4}
              style={{ width: "100%" }}
              value={this.state.noteText}
              onChange={(e) => this.setState({ noteText: e.target.value })}
            />

            <div style={{ textAlign: "right", paddingTop: "10px" }}>
              <Button
                iconName="undo"
                text="Cancel"
                onClick={() => this.setState({ dialogMode: DialogMode.None })}
              />
              <Button
                intent={Intent.PRIMARY}
                iconName="upload"
                text="Submit"
                disabled={this.state.noteText.trim().length === 0}
                onClick={() => {
                  this.props.createNote(_.map(selectedAssets, a => a.id), this.state.noteText.trim());
                  this.setState({ dialogMode: DialogMode.None });
                }}
              />
            </div>
          </div>
        );

      case DialogMode.ScheduleMaintenance:
        return (
          <div className="pt-callout pt-elevation-2">
            <h5>Schedule maintenance</h5>

            <label className="pt-label">
              Type
              <div className="pt-select">
                <select>
                  <option value="1">Inspect</option>
                  <option value="2">Repair</option>
                  <option value="3">Replace</option>
                  <option value="4">Provision</option>
                  <option value="5">Calibrate</option>
                </select>
              </div>
            </label>

            <label className="pt-label">
              Priority
              <div className="pt-select">
                <select>
                  <option value="1">Low</option>
                  <option value="2">Medium</option>
                  <option value="3">High</option>
                </select>
              </div>
            </label>

            <label className="pt-label">
              Deadline
              <div className="pt-select">
                <select>
                  <option value="1">1 day</option>
                  <option value="2">2 days</option>
                  <option value="3">3 days</option>
                  <option value="4">1 week</option>
                  <option value="5">2 weeks</option>
                  <option value="6">1 month</option>
                </select>
              </div>
            </label>

            <label>Additional notes</label>
            <textarea
              id="note"
              className="pt-input pt-intent-primary"
              dir="auto"
              rows={4}
              style={{ width: "100%" }}
              value={this.state.noteText}
              onChange={(e) => this.setState({ noteText: e.target.value })}
            />

            <div style={{ textAlign: "right", paddingTop: "10px" }}>
              <Button
                iconName="undo"
                text="Cancel"
                onClick={() => this.setState({ dialogMode: DialogMode.None })}
              />
              <Button
                intent={Intent.PRIMARY}
                iconName="upload"
                text="Submit"
                disabled={this.state.noteText.trim().length === 0}
                onClick={() => {
                  // TODO: store the form info somewhere!
                  toaster.show({
                    iconName: "calendar",
                    intent: Intent.SUCCESS,
                    message: `Maintenance successfully scheduled in work order system (${
                      selectedAssets.map(a => a.id).join(", ")
                    })`,
                  });
                  this.setState({ dialogMode: DialogMode.None });
                }}
              />
            </div>
          </div>
        );
    }
  }

  private uniqueNotes = (assets: IAsset[]) =>
    _.chain(assets)
      .map(a => a.notes)
      .flatten()
      .uniq(a => a.id)
      .sortBy(a => a.created)
      .value();

  private calculateSelectedRows = (regions: IRegion[]) => 
    _.chain(regions)
      .map(r => _.range(r.rows[0], r.rows[1] + 1))
      .flatten()
      .uniq()
      .value();
}

const dateToString = (date: Date) => date.getFullYear()
  + "/" + formatDateComponent(date.getMonth() + 1)
  + "/" + formatDateComponent(date.getDate());
const formatDateComponent = (x: number) => ((x < 10) ? "0" : "") + x;

const cellToRow = (region) => Regions.row(region.rows[0], region.rows[1]);


const mapDispatchToProps = {
  createNote: actions.createNote,
};

const mapStateToProps = createStructuredSelector({
  assets: selectors.selectAssets,
});

export default connect(mapStateToProps, mapDispatchToProps)(Inventory);
