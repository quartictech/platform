import * as React from "react";
import * as Plottable from "plottable";
import {
  Classes,
  Colors,
  Utils as BlueprintUtils,
} from "@blueprintjs/core";
import * as classNames from "classnames";
import * as _ from "underscore";
import { registerPointerHandler } from "../../helpers/plottable";


export const Schemes = {
  RED: [Colors.GRAY1, Colors.RED5, Colors.RED4, Colors.RED3, Colors.RED2, Colors.RED1],
  GOLD: [Colors.GRAY1, Colors.GOLD5, Colors.GOLD4, Colors.GOLD3, Colors.GOLD2, Colors.GOLD1],
  BLUE: [Colors.GRAY1, Colors.BLUE5, Colors.BLUE4, Colors.BLUE3, Colors.BLUE2, Colors.BLUE1],
  GREEN: [Colors.GRAY1, Colors.GREEN5, Colors.GREEN4, Colors.GREEN3, Colors.GREEN2, Colors.GREEN1],
};

// See http://www.ukpms.com/owner_forum/shared_files/UKPMS_Manual_02_Chapter4_XSP_v04.pdf
// We specify right-to-left, so that right-most roads get lower y values, thus appearing at the bottom.
// TODO: what about off-road features?  And more lanes
const ALL_LANES = ([
  "RE",
  "-R3", "-R2", "-R1",
  "CR1", "CR2", "CR3",
  "+R1", "+R2", "+R3",
  "CC",
  "+L3", "+L2", "+L1",
  "CL3", "CL2", "CL1",
  "-L1", "-L2", "-L3",
  "LE",
]);

export interface RoadSchematicSection {
  xMin: number;
  xMax: number;
  lane: string;
  value: number;
  faded?: boolean;
}

interface RoadSchematicProps {
  sections: RoadSchematicSection[];
  filterPredicate?: (RoadSchematicSection) => boolean;
  hoverText?: (RoadSchematicSection) => string | JSX.Element;
  colorScheme?: string[];
  maxValue?: number;
}

// Categorical scales don't allow heterogeneous width.  Thus we have to use a Linear scale, and manually
// compute the y-axis interval corresponding to each XSP.
interface XspToInterval {
  [xsp: string] : [number, number];
}

interface State {
  hoveredEntity: Plottable.Plots.IPlotEntity;
}


class RoadSchematic extends React.Component<RoadSchematicProps, State> {
  public static defaultProps: Partial<RoadSchematicProps> = {
    colorScheme: Schemes.RED,
  };

  private dataset: Plottable.Dataset;
  private colorScale: Plottable.Scales.InterpolatedColor;
  private xScale: Plottable.Scales.Linear;
  private yScale: Plottable.Scales.Linear;
  private outer: Plottable.Component;

  // Managed outside of React state to avoid setState not actually updating intervalMap before Plottable redraw
  // requires it.
  private intervalMap: XspToInterval;

  constructor(props: RoadSchematicProps) {
    super(props);
    this.state = {
      hoveredEntity: null,
    };
    this.createInitialPlotState(),
    this.intervalMap = intervalMap(props.sections);
    this.setPlotData(props); // Attach initial data to plot
  }

  // TODO - get the initial size to be regular (currently too big on the right)
  render() {
    // overflow:hidden required due to https://github.com/palantir/plottable/issues/3298
    return (
        <div style={{ padding: "10px", width: "99%" }}>
          <div style={{ width: "100%", height: 250, overflow: "hidden" }} ref="svg" />
          <div
            style={{ visibility: this.state.hoveredEntity ? "visible" : "hidden", display: "inline-block" }}
            className={classNames(Classes.CALLOUT, Classes.INTENT_PRIMARY)}
          >
            {this.state.hoveredEntity
              ? BlueprintUtils.safeInvoke(this.props.hoverText, this.state.hoveredEntity.datum)
              : "&nbsp;"
            }
          </div>
        </div>
    );
  }

  componentDidMount() {
    window.addEventListener("resize", () => this.outer.redraw());
    this.outer.renderTo(this.refs["svg"] as HTMLElement);
  }

  componentWillUpdate(nextProps: RoadSchematicProps) {
    if (nextProps !== this.props) {
      this.intervalMap = intervalMap(nextProps.sections);
      this.setPlotData(nextProps);
      this.outer.redraw();
    }
  }

  private setPlotData(props: RoadSchematicProps) {
    this.colorScale
      .domain([0, this.props.maxValue || _.max(props.sections, s => s.value).value])
      .range(props.colorScheme);
    this.xScale
      .domainMin(_.min(props.sections, s => s.xMin).xMin || 0)
      .domainMax(_.max(props.sections, s => s.xMax).xMax || 0);
    this.yScale
      .domain([0, _.max(this.intervalMap, i => i[1])[1]])
      .tickGenerator(() => _.map(this.intervalMap, i => (i[1] + i[0]) / 2));  // Ticks at interval midpoints

    const sections = props.filterPredicate
      ? _.filter(props.sections, props.filterPredicate)
      : props.sections;

    this.dataset.data(sections);
  }

  private createInitialPlotState() {
    this.dataset = new Plottable.Dataset();

    this.xScale = new Plottable.Scales.Linear();
    this.yScale = new Plottable.Scales.Linear();

    this.colorScale = new Plottable.Scales.InterpolatedColor("sqrt");

    const newPlot = () => new Plottable.Plots.Rectangle()
      .addDataset(this.dataset)
      .x((s: RoadSchematicSection) => s.xMin, this.xScale)
      .x2((s: RoadSchematicSection) => s.xMax)
      .y((s: RoadSchematicSection) => this.intervalMap[s.lane][0], this.yScale)
      .y2((s: RoadSchematicSection) => this.intervalMap[s.lane][1])
      .attr("fill-opacity", (s: RoadSchematicSection) => s.faded ? 0.3 : 1.0);

    const plot = newPlot()
      .attr("fill", (s: RoadSchematicSection) => s.value, this.colorScale)
      .attr("stroke", Colors.LIGHT_GRAY5)
      .attr("stroke-width", 1);

    const plotHighlighter = newPlot()
      .attr("fill", "black")
      .attr("fill-opacity", 0);

    this.configureInteraction(plotHighlighter);

    const xAxis = new Plottable.Axes.Numeric(this.xScale, "bottom");
    const yAxis = new Plottable.Axes.Numeric(this.yScale, "left");

    yAxis
      .tickLabelPadding(0)
      .formatter(label => this.getXspNameFromYPosition(label));

    const yLabel = new Plottable.Components.AxisLabel("Lane", 270);

    const group = new Plottable.Components.Group([plot, plotHighlighter]);

    this.outer = new Plottable.Components.Table([
      [yLabel, yAxis, group],
      [null, null,  xAxis],
    ]);
  }

  private configureInteraction(plotHighlighter: Plottable.Plot) {
    registerPointerHandler(
      plotHighlighter,
      (entity) => {
        if (this.state.hoveredEntity) {
          this.state.hoveredEntity.selection.attr("fill-opacity", 0);
        }
        if (entity) {
          entity.selection.attr("fill-opacity", 0.5);
        }
        this.setState({ hoveredEntity: entity });
      },
    );
  }

  private getXspNameFromYPosition(y: number) {
    return _.findKey(this.intervalMap, i => i[0] < y && y < i[1]).toString();
  }
}

// The magic here is the reducer - it accumulates the y-axis position from interval to interval.
const intervalMap = (sections: RoadSchematicSection[]) => _.chain(sections)
  .map(s => s.lane)
  .uniq()
  .sortBy(lane => _.indexOf(ALL_LANES, lane))
  .reduce(
    (memo: [number, XspToInterval], lane) => {
      const nextMax = memo[0] + laneWidth(lane);
      const nextInterval = [memo[0], nextMax];
      return [nextMax, { ...(memo[1]), [lane]: nextInterval }] as [number, XspToInterval];
    },
    [0, {}] as [number, XspToInterval],
  )
  .value()[1];

const laneWidth = (lane: string) => {
  switch (lane) {
    case "LE":
    case "RE":
    case "CC":
      return 0.5;
    default:
      return 1.0;
  }
};

export default RoadSchematic;
