import * as React from "react";
import * as Plottable from "plottable";
import {
  Colors,
} from "@blueprintjs/core";


interface RoadSchematicProps {
  data: any[];
}

interface State {
  plot: Plottable.Plot;
}

class RoadSchematic extends React.Component<RoadSchematicProps, State> {
  constructor(props: RoadSchematicProps) {
    super(props);
    this.state = {
      plot: this.createPlot(),
    };
    this.setPlotData(props.data); // Set initial data
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
    window.addEventListener("resize", () => this.state.plot.redraw());
    this.state.plot.renderTo(this.refs["svg"]);
  }

  componentWillUpdate(nextProps: RoadSchematicProps) {
    if (nextProps.data !== this.props.data) {
      this.setPlotData(nextProps.data);
    }
  }

  private setPlotData(data: any[]) {
    this.state.plot.datasets()[0].data(data);
  }

  private createPlot(): Plottable.Plot {
    const xScale = new Plottable.Scales.Category();
    const yScale = new Plottable.Scales.Category();
    const colorScale = new Plottable.Scales.InterpolatedColor()
      .range([Colors.GRAY1, Colors.RED4, Colors.RED3, Colors.RED2, Colors.RED1]);

    return new Plottable.Plots.Rectangle()
      .addDataset(new Plottable.Dataset())
      .x((d) => d.x, xScale)
      .y((d) => d.y, yScale)
      .attr("fill", (d) => d.val, colorScale)
      .attr("stroke", Colors.LIGHT_GRAY5)
      .attr("stroke-width", 1)
  }
}

export default RoadSchematic;
