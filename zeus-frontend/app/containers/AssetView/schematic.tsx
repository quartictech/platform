import * as React from "react";
import * as moment from "moment";
import * as numeral from "numeraljs";
import * as _ from "underscore";
import {
  NonIdealState,
  Tab2,
  Tabs2,
} from "@blueprintjs/core";

import Pane from "../../components/Pane";
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
    const sections = this.getSurveySections();
    return (
      <div style={{ width: "100%" }}>
        <Pane
          title="Defects schematic"
          iconName="error"
          extraHeaderContent={this.yearPicker()}>
          { (_.size(sections) > 0)
            ? (
              <RoadSchematic
                sections={sections}
                filterPredicate={s => s.year === this.state.yearSelection}
                hoverText={s => <span><b>Defect score:</b> {numeral(s.value).format("0.0")}</span>}
              />
            )
            : (
              <NonIdealState
                visual="info"
                title="No survey data available"
              />
            )
          }
        </Pane>
      </div>
    );
  }

  private yearPicker() {
    // TODO: have this populated from data
    return (
      <Tabs2
        id="year-picker"
        onChange={id => this.setState({ yearSelection: id.toString() })}
        selectedTabId={this.state.yearSelection}
      >
        <Tab2 id="2013" title="2013" />
        <Tab2 id="2014" title="2014" />
        <Tab2 id="2015" title="2015" />
        <Tab2 id="2016" title="2016" />
      </Tabs2>
    );
  }
  
  private getSurveySections(): RoadSchematicSection[] {
    return _.chain(this.props.asset["_surveys"])
      .map(s => ({
        xMin: numeral(s["schain"]),
        xMax: numeral(s["echain"]),
        value: this.getDefectScore(s),
        lane: s["xsect"],
        year: moment(s["start_date"]).year().toString(),  // Extra information used by filterPredicate
      }))
      .value();
  }

  private getDefectScore(section: RoadSchematicSection) {
    return _.reduce(section["defects"], (d: number, sum: number) => sum + d, 0);
  }
}

export default Schematic;
