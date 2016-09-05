import * as d3 from 'd3';
import * as chroma from 'chroma-js';

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

function computeStops(colorScale, nStops, minValue, maxValue) {
  let result = [];

  let colors = chroma.scale(colorScale).colors(nStops);

  for (var i = 0 ; i < nStops ; i++) {
    result.push([minValue + i * ((maxValue - minValue) / nStops), colors[i].toString(0)]);
  }
  return result;
}

function randomChoice(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
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

      let colorScale = style["color-scale"];
      if (colorScale == null) {
        colorScale = randomChoice(Object.keys(chroma.brewer));
      }
      return {
        "fill-color" : {
          "property" : style.property,
          "stops" : computeStops(colorScale, 8, attributeStats.minimum, attributeStats.maximum)
        },
        "fill-opacity": style["fill-opacity"]
      }
    }
}
