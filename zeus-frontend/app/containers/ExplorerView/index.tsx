import * as React from "react";
import { connect } from "react-redux";
import {
  Classes,
  NonIdealState,
  Spinner,
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
import {
  ResourceState,
  ResourceStatus,
} from "../../api-management";
import {
  DatasetName,
  Asset,
} from "../../models";
import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
import * as _ from "underscore";
const s = require("./style.css");

interface IProps {
  assetsRequired: () => void;
  assets: ResourceState<Map<string, Asset>>;
  location?: {
    query?: { [key : string]: string };
  };
  params: {
    datasetName: DatasetName;
  }
}

interface IState {
  filterColumn: number;
  filterValue: string;
  filterInvert: boolean;
  selectedRows: number[];
}

interface IColumn {
  name: string;
  displayValue: (Asset) => string;
}

const COLUMNS: IColumn[] = [
  { name: "Asset #", displayValue: x => x.id },
  { name: "Asset class", displayValue: x => x.clazz },
  { name: "Model #", displayValue: x => x.model.name },
  { name: "Serial #", displayValue: x => x.serial },
  { name: "Manufacturer code", displayValue: x => x.model.manufacturer },
  { name: "Location", displayValue: x => `${x.location.lat},${x.location.lon}`},
  { name: "Purchase date", displayValue: x => timestampToString(x.purchaseTimestamp) },
  { name: "Last inspection date", displayValue: x => timestampToString(x.lastInspectionTimestamp) },
  { name: "Last inspection signoff", displayValue: x => x.lastInspectionSignoff },
  { name: "Projected retirement date", displayValue: x => timestampToString(x.retirementTimestamp) },
];

class ExplorerView extends React.Component<IProps, IState> {
  private filterAssets = (
    assets: Map<string, Asset>,
    filterColumn: number,
    filterValue: string,
    filterInvert: boolean) => {
    if (filterColumn === -1 || filterValue === "") {
      return _.values(assets);
    }

    const stringInString = (needle: string, haystack: string) =>
      (haystack.toLocaleLowerCase().indexOf(needle.toLocaleLowerCase()) !== -1);

    return _.filter(
      _.values(assets),
      asset => stringInString(filterValue, COLUMNS[filterColumn].displayValue(asset)) !== filterInvert,
    );
  }

  state : IState = this.initialState();

  initialState(): IState {
    // Deal with URL params
    const firstKey = _.first(_.keys(this.props.location.query)) || "";
    const firstValue = this.props.location.query[firstKey] || "";
    const columnIndex = _.findIndex(COLUMNS, c => c.name.toLocaleLowerCase() === firstKey.toLocaleLowerCase());

    return {
      filterColumn: columnIndex,
      filterValue: firstValue,
      filterInvert: false,
      selectedRows: [],
    };
  }

  componentDidMount() {
    
  }

  render() {
    console.log("Dataset name", this.props.params.datasetName);

    const filteredAssets = this.filterAssets(
      this.props.assets.data, this.state.filterColumn, this.state.filterValue, this.state.filterInvert
    );

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

            {this.renderData(filteredAssets)}
          </div>
        </div>
    );
  }

  private renderData(filteredAssets: Asset[]) {
    switch (this.props.assets.status) {
      case ResourceStatus.LOADED:
        return (
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
        );

      case ResourceStatus.NOT_LOADED:
        return <NonIdealState
          visual="cross"
          title="No assets loaded."
        />;

      case ResourceStatus.LOADING:
        return <NonIdealState
          visual={<Spinner className={Classes.LARGE} />}
          title="Loading assets ..."
        />;

      case ResourceStatus.ERROR:
        return <NonIdealState
          visual="error"
          title="There was an error loading assets."
        />;
    }
  }

  private calculateSelectedRows = (regions: IRegion[]) => 
    _.chain(regions)
      .map(r => _.range(r.rows[0], r.rows[1] + 1))
      .flatten()
      .uniq()
      .value()
}

const timestampToString = (date: Date) => date.getFullYear()
  + "/" + formatDateComponent(date.getMonth() + 1)
  + "/" + formatDateComponent(date.getDate());
const formatDateComponent = (x: number) => ((x < 10) ? "0" : "") + x;

const cellToRow = (region) => Regions.row(region.rows[0], region.rows[1]);


const mapDispatchToProps = {
  createNote: actions.createNote,
};

const mapStateToProps = createStructuredSelector({
  assets: selectors.selectAssets, // TODO: eliminate
});

export default connect(mapStateToProps, mapDispatchToProps)(ExplorerView);
