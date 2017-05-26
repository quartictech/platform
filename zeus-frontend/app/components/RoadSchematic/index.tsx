import * as React from "react";
import * as Plottable from "plottable";
import {
  Colors,
} from "@blueprintjs/core";
import * as _ from "underscore";


export interface RoadSchematicSection {
  xMin: number;
  xMax: number;
  lane: number;
  value: number;
}

interface RoadSchematicProps {
  sections: RoadSchematicSection[];
}

interface State {
  dataset: Plottable.Dataset,
  xScale: Plottable.Scales.Linear,
  plottableComponent: Plottable.Component;
}

class RoadSchematic extends React.Component<RoadSchematicProps, State> {
  constructor(props: RoadSchematicProps) {
    super(props);
    this.state = this.createInitialState();
    this.setPlotData(props.sections); // Set initial data
  }

  // TODO - get the initial size to be regular (currently too big on the right)
  render() {
    return (
        <div style={{padding: "10px", width: "99%"}}>
          <svg style={{ width: "100%", height: 175 }} ref="svg" />
        </div>
    );
  }

  componentDidMount() {
    window.addEventListener("resize", () => this.state.plottableComponent.redraw());
    this.state.plottableComponent.renderTo(this.refs["svg"]);
  }

  componentWillUpdate(nextProps: RoadSchematicProps) {
    if (nextProps.sections !== this.props.sections) {
      this.setPlotData(nextProps.sections);
      this.state.plottableComponent.redraw();
    }
  }

  private setPlotData(sections: RoadSchematicSection[]) {
    this.state.xScale
      .domainMin(_.min(sections, s => s.xMin).xMin)
      .domainMax(_.max(sections, s => s.xMax).xMax);
    this.state.dataset.data(sections);
  }

  private createInitialState(): State {
    const dataset = new Plottable.Dataset();

    const xScale = new Plottable.Scales.Linear();
    const yScale = new Plottable.Scales.Category();
    const colorScale = new Plottable.Scales.InterpolatedColor()
      .range([Colors.GRAY1, Colors.RED4, Colors.RED3, Colors.RED2, Colors.RED1]);

    const plot = new Plottable.Plots.Rectangle()
      .addDataset(dataset)
      .x((s: RoadSchematicSection) => s.xMin, xScale)
      .x2((s: RoadSchematicSection) => s.xMax)
      .y((s: RoadSchematicSection) => s.lane, yScale)
      .attr("fill", (s: RoadSchematicSection) => s.value, colorScale)
      .attr("stroke", Colors.LIGHT_GRAY5)
      .attr("stroke-width", 1);

    const xAxis = new Plottable.Axes.Numeric(xScale, "bottom");
    const yAxis = new Plottable.Axes.Category(yScale, "left");

    const yLabel = new Plottable.Components.AxisLabel("Lane", 270);

    const plottableComponent = new Plottable.Components.Table([
      [yLabel, yAxis, plot],
      [null, null,  xAxis]
    ]);


    return { dataset, xScale, plottableComponent };
  } 
}

export default RoadSchematic;
