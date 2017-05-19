import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";

import * as moment from "moment";
import * as numeral from "numeraljs";

import * as classNames from "classnames";

import { TimeSeriesPoint } from "../../models";

import {
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";

import {
  Classes,
  NonIdealState,
  Spinner,
} from "@blueprintjs/core";

import {
  asset,
} from "../../api";

import * as selectors from "../../redux/selectors";
import * as actions from "../../redux/actions";
import { Asset } from "../../models";
const s = require("./style.css");

import { TimeChart } from "../../components/TimeChart";

interface IProps {
  ui: any;
  params: {
    assetId: string;
  };

  asset: ResourceState<Asset>;
  assetRequired: (string) => void;
}

interface IState {
  defectChartSelection: string;
}

class AssetView extends React.Component<IProps, IState> {
  constructor() {
    super();
    this.state = {
      defectChartSelection: null,
    };
  }

  componentDidMount() {
    this.props.assetRequired(this.props.params.assetId);
  }

  computeTimeSeries(asset): TimeSeriesPoint[] {
    if (this.state.defectChartSelection) {
      return asset._defect_time_series[this.state.defectChartSelection]
        .series.map( ({ timestamp, value }) => ({x: new Date(timestamp), y: value }));
    }
    return [];
  }

  renderDefectsChart(asset) {
    const timeSeries = this.computeTimeSeries(asset);
    return (
      <div>
        {this.renderChartButtons(asset)}
        <TimeChart yLabel="Road Quality" events={[]} timeSeries={timeSeries} />
      </div>
    );
  }

  renderChartButtons(asset) {
    const charts = Object.keys(asset._defect_time_series);
    return (
      <div className="pt-button-group pull-right">
        {
          charts.map( c =>
            <a
              key={c}
              id={c}
              className={classNames("pt-button", {"pt-active": c === this.state.defectChartSelection})}
              role="button"
              onClick={e => this.setState({ defectChartSelection: e.currentTarget.id })}
            >
              {c}
            </a>
          )
        }
      </div>
    );
  }

  renderAttributes(asset) {
    return (
      <table className="pt-table pt-striped">
        <tbody>
          <tr>
            <th>
              Road Name
            </th>
            <td>
              {asset["Road Name"]}
            </td>
          </tr>
          <tr>
            <th>
              Length (m)
            </th>
            <td>
              { numeral(asset["Length"]).format("0.00") }
            </td>
          </tr>
        </tbody>
      </table>
    );
  }

  renderJobsTable(asset) {
    return (
      <table className="pt-table pt-interactive pt-striped">
        <thead>
          <tr>
            <th>Job No.</th>
            <th>Start Date</th>
            <th>Notes</th>
          </tr>
        </thead>
        <tbody>
          {asset._jobs.map(job =>
            <tr key={job["Number"]}>
              <td>
                {job["Number"]}
              </td>

              <td>
                {moment(job["Start Date"]).format("Do MMMM YYYY, h:mm:ss a")}
              </td>

              <td>
                {job["Notes"]}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    );
  }

  componentWillReceiveProps(nextProps) {
    if (!this.state.defectChartSelection &&
      nextProps.asset.data &&
      nextProps.asset.data._defect_time_series) {
      const timeSeriesKeys = Object.keys(nextProps.asset.data._defect_time_series);
      if (timeSeriesKeys.length > 0) {
        this.setState({ defectChartSelection: timeSeriesKeys[0] });
      }
    }
  }


  renderData() {
    const asset = this.props.asset;
    switch (asset.status) {
      case ResourceStatus.LOADED:
        return (
          <div style={{flex: 1}}>
            <h1>{asset.data.RSL}</h1>
            {this.renderAttributes(asset.data)}
            {this.renderDefectsChart(asset.data)}
            {this.renderJobsTable(asset.data)}
          </div>
        );

      case ResourceStatus.NOT_LOADED:
        return <NonIdealState
          visual="cross"
          title="No asset loaded."
        />;

      case ResourceStatus.LOADING:
        return <NonIdealState
          visual={<Spinner className={Classes.LARGE} />}
          title="Loading asset ..."
        />;

      case ResourceStatus.ERROR:
        return <NonIdealState
          visual="error"
          title="There was an error loading asset."
        />;
    }
  }

  render() {
    return (
      <div className={s.container}>
      {this.renderData()}
      </div>
    );
  }
}

export { AssetView };

const mapDispatchToProps = {
  closeNewDatasetModal: () => actions.setActiveModal(null as string),
  assetRequired: resourceActions(asset).required,
};

const mapStateToProps = createStructuredSelector({
  ui: selectors.selectUi,
  asset: selectors.selectAsset,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AssetView);
