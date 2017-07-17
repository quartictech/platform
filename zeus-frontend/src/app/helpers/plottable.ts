import * as Plottable from "plottable";
import * as _ from "underscore";

export const registerPointerHandler = (
  plot: Plottable.Plot,
  handler: (entity?: Plottable.Plots.IPlotEntity) => void,
  closeEnough?: (point: Plottable.Point, entity: Plottable.Plots.IPlotEntity) => boolean,
) => {
  const interaction = new Plottable.Interactions.Pointer();
  interaction.onPointerMove((p) => {
    const selected = _.filter(plot.entitiesAt(p), e => closeEnough ? closeEnough(p, e) : true);
    if (selected.length === 1) {
      handler(selected[0]);
    } else {
      handler();
    }
  });
  interaction.onPointerExit(_ => handler());
  interaction.attachTo(plot);
};

export const registerClickHandler = (
  plot: Plottable.Plot,
  handler: (entity?: Plottable.Plots.IPlotEntity) => void,
  closeEnough?: (point: Plottable.Point, entity: Plottable.Plots.IPlotEntity) => boolean,
) => {
  const interaction = new Plottable.Interactions.Click();
  interaction.onClick((p) => {
    const selected = _.filter(plot.entitiesAt(p), e => closeEnough ? closeEnough(p, e) : true);
    if (selected.length === 1) {
      handler(selected[0]);
    }
  });
  interaction.attachTo(plot);
};
