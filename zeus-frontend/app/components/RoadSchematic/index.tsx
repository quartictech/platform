import * as React from "react";
import * as Plottable from "plottable";
import {
  Classes,
  Colors,
  Utils as BlueprintUtils,
} from "@blueprintjs/core";
import * as classNames from "classnames";
import * as _ from "underscore";

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
}

interface RoadSchematicProps {
  sections: RoadSchematicSection[];
  maxValue: number;
  hoverText?: (RoadSchematicSection) => string | JSX.Element;
}

// Categorical scales don't allow heterogeneous width.  Thus we have to use a Linear scale, and manually
// compute the y-axis interval corresponding to each XSP.
interface XspToInterval {
  [xsp: string] : [number, number];
}

interface PlotBits {
  dataset: Plottable.Dataset;
  colorScale: Plottable.Scales.InterpolatedColor;
  xScale: Plottable.Scales.Linear;
  yScale: Plottable.Scales.Linear;
  outer: Plottable.Component;
}

interface State {
  plot: PlotBits;
  hoveredEntity: Plottable.Plots.IPlotEntity;
}


class RoadSchematic extends React.Component<RoadSchematicProps, State> {
  // Managed outside of React state to avoid setState not actually updating intervalMap before Plottable redraw
  // requires it.
  private intervalMap: XspToInterval;

  constructor(props: RoadSchematicProps) {
    super(props);
    this.state = {
      plot: this.createInitialPlotState(),
      hoveredEntity: null,
    };
    this.intervalMap = intervalMap(props.sections);
    this.setPlotData(props.sections); // Attach initial data to plot
  }

  // TODO - get the initial size to be regular (currently too big on the right)
  render() {
    // overflow:hidden required due to https://github.com/palantir/plottable/issues/3298
    return (
        <div style={{padding: "10px", width: "99%"}}>
          <div style={{ width: "100%", height: 250, overflow: "hidden" }} ref="svg" />
          <span
            style={{ visibility: this.state.hoveredEntity ? "visible" : "hidden" }}
            className={classNames(Classes.CALLOUT, Classes.TEXT_MUTED)}
          >
            {this.state.hoveredEntity
              ? BlueprintUtils.safeInvoke(this.props.hoverText, this.state.hoveredEntity.datum)
              : ""
            }
          </span>
        </div>
    );
  }

  componentDidMount() {
    window.addEventListener("resize", () => this.state.plot.outer.redraw());
    this.state.plot.outer.renderTo(this.refs["svg"] as HTMLElement);
  }

  componentWillUpdate(nextProps: RoadSchematicProps) {
    if (nextProps.sections !== this.props.sections || nextProps.maxValue !== this.props.maxValue) {
      this.intervalMap = intervalMap(nextProps.sections);
      this.setPlotData(nextProps.sections);
      this.state.plot.outer.redraw();
    }
  }

  private setPlotData(sections: RoadSchematicSection[]) {
    this.state.plot.colorScale.domain([0, this.props.maxValue]);
    this.state.plot.xScale
      .domainMin(_.min(sections, s => s.xMin).xMin || 0)
      .domainMax(_.max(sections, s => s.xMax).xMax || 0);
    this.state.plot.yScale
      .domain([0, _.max(this.intervalMap, i => i[1])[1]])
      .tickGenerator(() => _.map(this.intervalMap, i => (i[1] + i[0]) / 2));  // Ticks at interval midpoints
    this.state.plot.dataset.data(sections);
  }

  private createInitialPlotState(): PlotBits {
    const dataset = new Plottable.Dataset();

    const xScale = new Plottable.Scales.Linear();
    const yScale = new Plottable.Scales.Linear();

    const colorScale = new Plottable.Scales.InterpolatedColor("sqrt")
      .range([Colors.GRAY1, Colors.RED5, Colors.RED4, Colors.RED3, Colors.RED2, Colors.RED1]);

    const newPlot = () => new Plottable.Plots.Rectangle()
      .addDataset(dataset)
      .x((s: RoadSchematicSection) => s.xMin, xScale)
      .x2((s: RoadSchematicSection) => s.xMax)
      .y((s: RoadSchematicSection) => this.intervalMap[s.lane][0], yScale)
      .y2((s: RoadSchematicSection) => this.intervalMap[s.lane][1]);

    const plot = newPlot()
      .attr("fill", (s: RoadSchematicSection) => s.value, colorScale)
      .attr("stroke", Colors.LIGHT_GRAY5)
      .attr("stroke-width", 1);

    const plotHighlighter = newPlot()
      .attr("fill", "black")
      .attr("fill-opacity", 0);

    this.configureInteraction(plot, plotHighlighter);

    const xAxis = new Plottable.Axes.Numeric(xScale, "bottom");
    const yAxis = new Plottable.Axes.Numeric(yScale, "left");

    
    yAxis
      .tickLabelPadding(0)
      .formatter(label => this.getXspNameFromYPosition(label));

    const yLabel = new Plottable.Components.AxisLabel("Lane", 270);

    const group = new Plottable.Components.Group([plot, plotHighlighter]);

    const outer = new Plottable.Components.Table([
      [yLabel, yAxis, group],
      [null, null,  xAxis],
    ]);

    return {
      dataset,
      colorScale,
      xScale,
      yScale,
      outer,
    };
  }

  private configureInteraction(plot: Plottable.Plot, plotHighlighter: Plottable.Plot) {
    const set = (entity: Plottable.Plots.IPlotEntity) => {
      entity.selection.attr("fill-opacity", 0.5);
      this.setState({ hoveredEntity: entity });
    };

    const clear = () => {
      if (this.state.hoveredEntity) {
        this.state.hoveredEntity.selection.attr("fill-opacity", 0);
        this.setState({ hoveredEntity: null });
      }
    };

    const interaction = new Plottable.Interactions.Pointer();
    interaction.onPointerMove((p) => {
      clear();
      const selected = plotHighlighter.entitiesAt(p);
      if (selected.length === 1) {
        set(selected[0]);
      }
    });
    interaction.onPointerExit(clear);
    interaction.attachTo(plot);
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
      return [nextMax, { ...(memo[1]), [lane]: nextInterval}] as [number, XspToInterval];
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
