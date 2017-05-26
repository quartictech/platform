import * as React from "react";
const DocumentTitle = require("react-document-title");  // TODO: wtf - doesn't work with import
import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";
import * as moment from "moment";
import * as _ from "underscore";
import {
  Position,
} from "@blueprintjs/core";

import Pane from "../../components/Pane";
import NormalPicker from "../../components/NormalPicker";
import RoadSchematic, { RoadSchematicSection } from "../../components/RoadSchematic";

import {
  resourceActions,
  ResourceState,
  ResourceStatus,
} from "../../api-management";

import {
  asset,
} from "../../api";

import { Asset } from "../../models";
import * as selectors from "../../redux/selectors";


interface Props {
  params: {
    assetId: string;
  };

  asset: ResourceState<Asset>;
  assetRequired: (string) => void;
}

interface State {
  plot: any;
  yearSelection: string;
}

class SchematicView extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      plot: null,
      yearSelection: "2016",
    };
  }

  render() {
    return (
      <DocumentTitle title="Quartic - Road schematic">
        <div style={{ width: "100%" }}>
          <h1>Oh hello</h1>
          <Pane
            title="Roads are noob"
            iconName="drive-time"
            extraHeaderContent={this.yearPicker()}>
            <RoadSchematic sections={this.getSurveySections()} />
          </Pane>
        </div>
      </DocumentTitle>
    );
  }

  private yearPicker() {
    // TODO: have this populated from data
    return (
      <NormalPicker
        iconName="calendar"
        position={Position.TOP}
        selected={this.state.yearSelection}
        entries={["2013", "2014", "2015", "2016"]}
        onChange={id => this.setState({ yearSelection: id })}
      />
    );
  }

  componentWillMount() {
    this.onNewAsset(this.props.params.assetId);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.params.assetId !== nextProps.params.assetId) {
      this.onNewAsset(nextProps.params.assetId);
    }
  }

  private onNewAsset(assetId: string) {
    this.props.assetRequired(assetId);
  }

  private getSurveySections(): RoadSchematicSection[] {
    const asset = this.props.asset;
    switch (asset.status) {
      case ResourceStatus.LOADED:
        console.log("surveys", asset.data["_surveys"]);
        return _.chain(asset.data["_surveys"])
          .filter(s => moment(s["start_date"]).year().toString() === this.state.yearSelection)
          .map(s => ({
            xMin: s["schain"],
            xMax: s["echain"],
            value: _.size(s["defects"]),
            lane: s["xsect"],
          }))
          .value();

      default:
        return [];
    }
  }
}

const mapDispatchToProps = {
  assetRequired: resourceActions(asset).required,
};

const mapStateToProps = createStructuredSelector({
  asset: selectors.selectAsset,
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(SchematicView);
