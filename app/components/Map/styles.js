import * as d3 from 'd3';

export function computeColorScale(baseColor, step, n) {
  let color = baseColor;
  let result = [];

  for(var i = 0; i < n ; i++) {
    result.push(color);
    color = d3.hsl(color).darker(step);
    result.push(color.toString());
  }

  return result;
}

function computeStops(baseColor, nStops, minValue, maxValue) {
  let result = [];

  let colors = computeColorScale(baseColor, 2, nStops);

  for (var i = 0 ; i < nStops ; i++) {
    result.push([minValue + i * ((maxValue - minValue) / nStops), colors[i].toString(0)]);
  }
  console.log(result);
  return result;
}

export function polygonLayerStyle(layer) {
    let style = layer.style.polygon;
    console.log(style.property);
    if (style.property == null) {
      return {
        "fill-color": style["fill-color"],
        "fill-opacity": style["fill-opacity"]
      }
    }
    else {
      let attributeStats = layer.stats.attributeStats[style.property];
      return {
        "fill-color" : {
          "property" : style.property,
          "stops" : computeStops(style["fill-color"], 9, attributeStats.minimum, attributeStats.maximum)
        },
        "fill-opacity": style["fill-opacity"]
      }
    }
}
