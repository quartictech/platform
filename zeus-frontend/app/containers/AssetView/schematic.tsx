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
  yearSelection: string;
  onSelectYear: (year: string) => void;
}

class Schematic extends React.Component<SchematicProps, {}> {
  constructor(props: SchematicProps) {
    super(props);
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
                filterPredicate={s => s.year === this.props.yearSelection}
                hoverText={s => this.defectsCallout(s.raw)}
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

  private defectsCallout(defects: any) {
    if (_.size(defects) === 0) {
      return <span>No defects</span>;
    }
    return (
      <span>
        {_.map(defects, (v, k: string) => (
          <span key={k} style={{ paddingRight: "10px" }}>
            <b>{_.last(k.split("-"))}:</b>&nbsp;{v}
          </span>
        ))}
      </span>
    );
  }

  private yearPicker() {
    // TODO: have this populated from data
    return (
      <Tabs2
        id="year-picker"
        onChange={id => this.props.onSelectYear(id.toString())}
        selectedTabId={this.props.yearSelection}
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
        raw: s["defects"],
      }))
      .value();
  }

  private getDefectScore(section: RoadSchematicSection) {
    return _.reduce(section["defects"], (d: number, sum: number) => sum + d, 0);
  }
}

export default Schematic;
