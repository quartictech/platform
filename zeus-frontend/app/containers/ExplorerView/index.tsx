import * as React from "react";
const DocumentTitle = require("react-document-title");  // TODO: wtf - doesn't work with import
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
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";
import {
  datasetContent,
} from "../../api";
import {
  Dataset,
  DatasetName,
} from "../../models";
import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as _ from "underscore";
import { stringInString, toTitleCase } from "../../helpers/Utils";
const s = require("./style.css");

interface ExplorerViewProps {
  datasetContentRequired: (dataset: DatasetName) => void;
  datasetContent: ResourceState<Dataset<any>>;
  params: {
    datasetName: DatasetName;
  };
}

interface ExplorerViewState {
  filterColumn: string;
  filterValue: string;
  filterInvert: boolean;
  selectedRows: number[];
}

class ExplorerView extends React.Component<ExplorerViewProps, ExplorerViewState> {
  private filterData = (
    datasetContent: Dataset<any>,
    column: string,
    value: string,
    invert: boolean) => {
    if (column === "" || value === "") {
      return _.values(datasetContent.content);
    }

    return _.filter(
      _.values(datasetContent.content),
      item => stringInString(value, stringify(item[column])) !== invert,
    );
  }

  state : ExplorerViewState = this.initialState();

  initialState(): ExplorerViewState {
    return {
      filterColumn: "",
      filterValue: "",
      filterInvert: false,
      selectedRows: [],
    };
  }

  public componentDidMount() {
    this.props.datasetContentRequired(this.props.params.datasetName);
  }

  public componentWillReceiveProps(nextProps: ExplorerViewProps) {
    if (nextProps.params.datasetName !== this.props.params.datasetName) {
      this.props.datasetContentRequired(nextProps.params.datasetName);
    }
  }

  public render() {
    return (
        <div className={s.container}>
          <div className={s.main}>
            {this.maybeRender()}
          </div>
        </div>
    );
  }

  private maybeRender() {
    switch (this.props.datasetContent.status) {
      case ResourceStatus.LOADED:
        return (
          <div>
            <h1>{this.props.params.datasetName}</h1>
            {this.renderControls()}
            {this.renderData()}
          </div>
        );

      case ResourceStatus.NOT_LOADED:
        return <NonIdealState
          visual="cross"
          title="No data loaded."
        />;

      case ResourceStatus.LOADING:
        return <NonIdealState
          visual={<Spinner className={Classes.LARGE} />}
          title="Loading data ..."
        />;

      case ResourceStatus.ERROR:
        return <NonIdealState
          visual="error"
          title="There was an error loading data."
        />;
    }
  }

  private renderControls() {
    return (
      <div className={Classes.CONTROL_GROUP}>
        <div className={Classes.SELECT}>
          <select
            value={this.state.filterColumn}
            onChange={e => this.setState({ filterColumn: e.target.value })}
          >
            <option value="">Filter...</option>
            { this.columns().map(col => <option key={col} value={col}>{col}</option>) }
          </select>
        </div>

        <input
          type="text"
          className={Classes.INPUT}
          placeholder="Value"
          disabled={this.state.filterColumn === ""}
          value={this.state.filterValue}
          onChange={e => this.setState({ filterValue: e.target.value })}
        />

        <Switch
          label="Invert"
          disabled={this.state.filterColumn === ""}
          checked={this.state.filterInvert}
          onChange={() => this.setState({ filterInvert: !this.state.filterInvert })}
        />
      </div>
    );
  }

  private renderData() {
    // TODO: should cache this
    const filteredItems = this.filterData(
      this.props.datasetContent.data, this.state.filterColumn, this.state.filterValue, this.state.filterInvert,
    );

    return (
      <DocumentTitle title={`Quartic - ${toTitleCase(this.props.params.datasetName)}`}>
        <Table
          isRowResizable={true}
          numRows={filteredItems.length}
          selectionModes={SelectionModes.ROWS_AND_CELLS}
          onSelection={regions => this.setState({ selectedRows: this.calculateSelectedRows(regions) })}
          selectedRegionTransform={cellToRow}
        >
          {
            this.columns().map(col => <Column
              key={col}
              name={col}
              renderCell={(row: number) => <Cell>{stringify(_.values(filteredItems)[row][col])}</Cell>}
            />)
          }
        </Table>
      </DocumentTitle>
    );
  }

  private columns = () => this.props.datasetContent.data.schema;

  private calculateSelectedRows = (regions: IRegion[]) => 
    _.chain(regions)
      .map(r => _.range(r.rows[0], r.rows[1] + 1))
      .flatten()
      .uniq()
      .value()
}

const stringify = (obj: any) => (obj === null) ? "" : obj.toString();

const cellToRow = (region) => ((region.rows)
    ? Regions.row(region.rows[0], region.rows[1])
    : Regions.row(0, -1)  // This is a bit of a hack to support column selections
);

const mapDispatchToProps = {
  datasetContentRequired: resourceActions(datasetContent).required,
};

const mapStateToProps = createStructuredSelector({
  datasetContent: selectors.selectDatasetContent,
});

export default connect(mapStateToProps, mapDispatchToProps)(ExplorerView);
