import * as React from "react";
import * as moment from "moment";
import * as _ from "underscore";
import {
  Position,
} from "@blueprintjs/core";

import Pane from "../../components/Pane";
import NormalPicker from "../../components/NormalPicker";
import RoadSchematic, { RoadSchematicSection } from "../../components/RoadSchematic";
import { Asset } from "../../models";

interface SchematicProps {
  asset: Asset;
}

interface State {
  yearSelection: string;
}

class Schematic extends React.Component<SchematicProps, State> {
  constructor(props: SchematicProps) {
    super(props);
    this.state = {
      yearSelection: "2016",
    };
  }

  render() {
    return (
      <div style={{ width: "100%" }}>
        <Pane
          title="Defects schematic"
          iconName="error"
          extraHeaderContent={this.yearPicker()}>
          <RoadSchematic sections={this.getSurveySections()} />
        </Pane>
      </div>
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

  private getSurveySections(): RoadSchematicSection[] {
    return _.chain(this.props.asset["_surveys"])
      .filter(s => moment(s["start_date"]).year().toString() === this.state.yearSelection)
      .map(s => ({
        xMin: s["schain"],
        xMax: s["echain"],
        value: _.size(s["defects"]),
        lane: s["xsect"],
      }))
      .value();
  }
}

export default Schematic;
