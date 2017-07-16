import * as React from "react";
const DocumentTitle = require("react-document-title");  // tslint:disable-line:variable-name
import { connect } from "react-redux";
import {
  AnchorButton,
  Classes,
  InputGroup,
  Intent,
  NonIdealState,
  Spinner,
} from "@blueprintjs/core";
import {
  Cell,
  Column,
  IRegion,
  Regions,
  SelectionModes,
  Table,
} from "@blueprintjs/table";
import * as classNames from "classnames";
import {
  resourceActions,
  ResourceState,
  ResourceStatus,
  ifLoaded,
} from "../../api-management";
import {
  datasetContent,
  datasetInfo,
  downloadLinkFor,
} from "../../api";
import {
  Dataset,
  DatasetInfo,
  DatasetName,
} from "../../models";
import { createStructuredSelector } from "reselect";
import * as selectors from "../../redux/selectors";
import * as _ from "underscore";
import Pane from "../../components/Pane";
import { stringInString } from "../../helpers/Utils";
const s = require("./style.css");

interface Props {
  datasetInfoRequired: () => void;
  datasetInfo: ResourceState<{ [id: string] : DatasetInfo}>;
  datasetContentRequired: (dataset: DatasetName) => void;
  datasetContent: ResourceState<Dataset<any>>;
  params: {
    datasetName: DatasetName;
  };
}

interface State {
  filteredItems: any[];
  filterTerm: string;
  selectedRows: number[];
}

class ExplorerView extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      filteredItems: [],
      filterTerm: "",
      selectedRows: [],
    };
  }

  componentDidMount() {
    this.props.datasetInfoRequired();
    this.props.datasetContentRequired(this.props.params.datasetName);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.params.datasetName !== this.props.params.datasetName) {
      this.props.datasetContentRequired(nextProps.params.datasetName);
    }

    if (nextProps.datasetContent.status === ResourceStatus.LOADED &&
      this.props.datasetContent.status !== ResourceStatus.LOADED) {
      this.updateFilter(this.state.filterTerm, nextProps.datasetContent.data);
    }
  }

  render() {
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
        return this.renderData();

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

  private renderData() {
    return (
      <DocumentTitle title={`Quartic - ${this.maybePrettyName()}`}>
        <div>
          <Pane
            title={this.maybePrettyName()}
            iconName="database"
            extraHeaderContent={this.renderControls()}
          >
            <div style={{ height: "600px" }}>
              <Table
                isRowResizable={true}
                numRows={this.state.filteredItems.length}
                selectionModes={SelectionModes.ROWS_AND_CELLS}
                onSelection={regions => this.setState({ selectedRows: this.calculateSelectedRows(regions) })}
                selectedRegionTransform={cellToRow}
              >
                {
                  this.columns().map(col => <Column
                    key={col}
                    name={col}
                    renderCell={(row: number) => <Cell>{stringify(_.values(this.state.filteredItems)[row][col])}</Cell>}
                  />)
                }
              </Table>
            </div>
          </Pane>

          <div style={{ float: "right", margin: "10px" }}>
            <AnchorButton
              href={downloadLinkFor(this.props.params.datasetName)}
              intent={Intent.PRIMARY}
              iconName="download"
              text={`Download all ${this.maybePrettyName()} as CSV`}
            />
          </div>
        </div>
      </DocumentTitle>
    );
  }

  private renderControls() {
    return (
      <div style={{ width: "20%" }}>
        <InputGroup
          className={classNames(Classes.ROUND)}
          leftIconName="filter"
          placeholder="Filter..."
          value={this.state.filterTerm}
          onChange={e => this.updateFilter(e.target.value, this.props.datasetContent.data)}
        />
      </div>
    );
  }

  private updateFilter(term: string, dataset: Dataset<any>) {
    this.setState({
      filterTerm: term,
      filteredItems: this.filterData(term.trim(), dataset),
    });
  }

  private filterData = (term: string, dataset: Dataset<any>) => {
    const items = _.values(dataset.content);

    if (term === "") {
      return items;
    }

    return _.filter(items, item => _.any(item, v => stringInString(term, stringify(v))));
  }

  private maybePrettyName() {
    return ifLoaded(this.props.datasetInfo, info => info[this.props.params.datasetName].prettyName, "<< unknown >>");
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

const cellToRow = region => ((region.rows)
    ? Regions.row(region.rows[0], region.rows[1])
    : Regions.row(0, -1)  // This is a bit of a hack to support column selections
);

const mapDispatchToProps = {
  datasetInfoRequired: resourceActions(datasetInfo).required,
  datasetContentRequired: resourceActions(datasetContent).requiredFresh,
};

const mapStateToProps = createStructuredSelector({
  datasetInfo: selectors.selectDatasetInfo,
  datasetContent: selectors.selectDatasetContent,
});

export default connect(mapStateToProps, mapDispatchToProps)(ExplorerView);
