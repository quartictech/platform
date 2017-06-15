import * as React from "react";
import * as moment from "moment";
import * as numeral from "numeraljs";
import * as _ from "underscore";
import {
  Classes,
  NonIdealState,
  Tab2,
  Tabs2,
} from "@blueprintjs/core";

import Pane from "../../components/Pane";
import RoadSchematic, { RoadSchematicSection, Schemes } from "../../components/RoadSchematic";
import { Asset } from "../../models";
const styles = require("./style.css");

interface SchematicProps {
  asset: Asset;
  yearSelection: string;
  onSelectYear: (year: string) => void;
}

interface State {
  surveySelection: string;
}

class Schematic extends React.Component<SchematicProps, State> {
  constructor(props: SchematicProps) {
    super(props);
    this.state = {
      surveySelection: "dvi",
    };
  }

  render() {
    const sections = this.getSurveySections();
    return (
      <div style={{ width: "100%" }}>
        <Pane
          title="Survey schematic"
          iconName="error"
          extraHeaderContent={this.controls()}>
          { (_.size(sections) > 0)
            ? (
              <RoadSchematic
                sections={sections}
                filterPredicate={s => s.year === this.props.yearSelection}
                hoverText={s => this.defectsCallout(s.raw)}
                colorScheme={(this.state.surveySelection === "dvi") ? Schemes.RED : Schemes.GOLD}
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

  private defectsCallout(section: RoadSchematicSection) {
    if (!section["assessed"]) {
      return <span>Not assessed</span>;
    }
    if (_.size(section["defects"]) === 0) {
      return <span>No defects</span>;
    }
    return (
      <span>
        {_.map(section["defects"], (v, k: string) => (
          <span key={k} style={{ paddingRight: "10px" }}>
            <b>{_.last(k.split("-"))}:</b>&nbsp;{v}
          </span>
        ))}
      </span>
    );
  }

  private controls() {
    // TODO: have this populated from data
    return (
      <div>
        <Tabs2
          className={styles.tabs}
          id="survey-picker"
          onChange={id => this.setState({ surveySelection: id.toString() })}
          selectedTabId={this.state.surveySelection}
        >
          <Tab2 id="dvi" title="DVI" disabled={!this.hasDVIData()} />
          <Tab2 id="scanner" title="SCANNER (3m LPV)" disabled={!this.hasScannerData()} />
        </Tabs2>

        <span
          className={Classes.NAVBAR_DIVIDER}
          style={{ marginLeft: "20px", marginRight: "20px" }}
        />

        <Tabs2
          className={styles.tabs}
          id="year-picker"
          onChange={id => this.props.onSelectYear(id.toString())}
          selectedTabId={this.props.yearSelection}
        >
          <Tab2 id="2013" title="2013" disabled={!this.hasData()}/>
          <Tab2 id="2014" title="2014" disabled={!this.hasData()}/>
          <Tab2 id="2015" title="2015" disabled={!this.hasData()}/>
          <Tab2 id="2016" title="2016" disabled={!this.hasData()}/>
        </Tabs2>
      </div>
    );
  }

  private getSurveySections(): RoadSchematicSection[] {
    return _.chain(this.props.asset["_surveys"][this.state.surveySelection])
      .map(section => ({
        xMin: numeral(section["schain"]),
        xMax: numeral(section["echain"]),
        value: this.getDefectScore(section),
        lane: section["xsect"],
        faded: !section["assessed"],
        year: moment(section["date"]).year().toString(),  // Extra information used by filterPredicate
        raw: section,
      }))
      .value();
  }

  private hasScannerData(): boolean {
    return "scanner" in this.props.asset["_surveys"];
  }

  private hasData(): boolean {
    return this.hasScannerData() || this.hasDVIData();
  }

  private hasDVIData(): boolean {
    return "dvi" in this.props.asset["_surveys"];
  }

  private getDefectScore(section: RoadSchematicSection) {
    return _.reduce(section["defects"], (d: number, sum: number) => sum + d, 0);
  }
}

export default Schematic;
