import * as React from "react";
import { connect } from "react-redux";

import { createStructuredSelector } from "reselect";

import * as moment from "moment";
import * as numeral from "numeraljs";

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
  asset
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

class AssetView extends React.Component<IProps, void> {
  componentDidMount() {
    this.props.assetRequired(this.props.params.assetId);
  }

  renderAttributes(asset) {
    return (
      <table className="pt-table pt-striped">
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
      </table>
    );
  }

  renderJobsTable(asset) {
    return (
      <table className="pt-table pt-interactive pt-striped">
        <tr>
          <th>Job No.</th>
          <th>Start Date</th>
          <th>Notes</th>
        </tr>

        { asset._jobs.map(job =>
          <tr>
            <td>
              { job["Number"] }
            </td>

            <td>
              { moment(job["Start Date"]).format("Do MMMM YYYY, h:mm:ss a") }
            </td>

            <td>
              { job["Notes"] }
            </td>
          </tr>
        )}
      </table>
    );
  }


  renderData() {
    const asset = this.props.asset;
    switch (asset.status) {
      case ResourceStatus.LOADED:
        return (
          <div style={{flex: 1}}>
            <h1>{asset.data.RSL}</h1>
            {this.renderAttributes(asset.data)}
            <TimeChart yLabel="Road Quality" events={[]} timeSeries={[]} />
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
