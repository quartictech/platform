import * as d3 from "d3";
import * as chroma from "chroma-js";

export function computeColorScale(baseColor, step, n) {
  let color = baseColor;
  const result = [];

  for (let i = 0; i < n; i++) {
    result.push(color);
    color = d3.hsl(color).darker(step);
    result.push(color.toString());
  }

  return result;
}

function computeStops(colorScale, nStops, minValue, maxValue) {
  const result = [];
  const colors = chroma.scale(colorScale).colors(nStops);

  for (let i = 0; i < nStops; i++) {
    result.push([minValue + (i * ((maxValue - minValue) / nStops)), colors[i].toString(0)]);
  }
  return result;
}

export function polygonLayerStyle(layer) {
  const style = layer.style.polygon;
  if (style.property == null) {
    return {
      "fill-color": style["fill-color"],
      "fill-outline-color": style["fill-outline-color"],
      "fill-opacity": style["fill-opacity"],
    };
  }

  const attributeStats = layer.stats.attributeStats[style.property];
  const colorScale = style["color-scale"];
  return {
    "fill-color": {
      "property": style.property,
      "stops": computeStops(colorScale, 8, attributeStats.minimum, attributeStats.maximum),
    },
    "fill-opacity": style["fill-opacity"],
  };
}
