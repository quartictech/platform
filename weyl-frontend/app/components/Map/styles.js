import * as d3 from "d3";
import * as chroma from "chroma-js";

export function computeColorScaleFromBase(baseColor, step, n) {
  let color = baseColor;
  const result = [];

  for (var i = 0; i < n; i++) {
    result.push(color);
    color = d3.hsl(color).darker(step);
    result.push(color.toString());
  }

  return result;
}

function computeStops(colorScale, nStops, minValue, maxValue) {
  const result = [];

  const colors = chroma.scale(colorScale).colors(nStops);

  for (var i = 0 ; i < nStops ; i++) {
    result.push([minValue + i * ((maxValue - minValue) / nStops), colors[i].toString(0)]);
  }
  return result;
}

export function polygonLayerStyle(layer) {
    let style = layer.style.polygon;
    if (style.property == null) {
      return {
        "fill-color": style["fill-color"],
        "fill-outline-color": style["fill-outline-color"],
        "fill-opacity": style["fill-opacity"]
      }
    }
    else {
      let attributeStats = layer.stats.attributeStats[style.property];
      let colorScale = style["color-scale"];
      return {
        "fill-color" : {
          "property" : style.property,
          "stops" : y
        },
        "fill-opacity": style["fill-opacity"]
      }
    }
}

function colorStyle(property, style, attributeStats) {
  if (property == null) {
      return style.color;
  }
  else {
    return {
      "property": property,
      "stops": computeStops(style.colorScale, 8, attributeStats[property].minimum, attributeStats[property].maximum)
    }
  }
}

let customStyles = {}

export function buildStyleLayers(style, attributeStats) {
  if (style.type === "DEFAULT") {
    return {
      point:  {
        type: "circle",
        paint: {
          "circle-radius": style.point["circle-radius"],
          "circle-color": colorStyle(style.property, style.point, attributeStats),
          "circle-opacity": style.opacity,
        },
        filter: ["==", "$type", "Point"],
      },
      polygon: {
        type: "fill",
        paint: {
          "fill-color": colorStyle(style.property, style.polygon, attributeStats),
          "fill-outline-color": style.property == null ? style.polygon["fill-outline-color"] : null,
          "fill-opacity": style.opacity,
        },
        filter: ["==", "$type", "Polygon"],
      },
      "line": {
        type: "line",
        paint: {
          "line-color": colorStyle(style.property, style.line, attributeStats)
        },
        filter: ["==", "$type", "LineString"],
      }
    }
  }
  else {
    return customStyles[style.type];
  }
}
