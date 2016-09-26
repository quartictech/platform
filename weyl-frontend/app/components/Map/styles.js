import * as d3 from "d3";
import * as chroma from "chroma-js";

import { customStyles, liveLayerStyle } from "./customStyles.js";

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

  for (var i = 0; i < nStops; i++) {
    result.push([minValue + (i * (maxValue - minValue) / nStops), colors[i].toString(0)]);
  }
  return result;
}

function colorStyle(property, style, attributeStats) {
  if (property == null) {
    return style.color;
  }
  return {
    "property": property,
    "stops": computeStops(style.colorScale, 8, attributeStats[property].minimum, attributeStats[property].maximum),
  };
}

function filter(property, geomType) {
  const geomFilter = ["==", "$type", geomType];
  if (property != null) {
    const propFilter = ["has", property];
    return ["all", geomFilter, propFilter];
  }
  else {
    return geomFilter;
  }
}

export function buildStyleLayers(layer) {
  let [layerName, style, attributeStats] = [layer.name, layer.style, layer.stats.attributeStats];
  if (customStyles.hasOwnProperty(layerName)) {
    return customStyles[layerName];
  }
  else if (layer.live) {
    return liveLayerStyle;
  }
  else {
    return {
      point: {
        type: "circle",
        paint: {
          "circle-radius": style.point["circle-radius"] - 2,
          "circle-color": colorStyle(style.property, style.point, attributeStats),
          "circle-opacity": style.opacity,
        },
        filter: filter(style.property, "Point"),
      },
      point2: {
        type: "circle",
        paint: {
          "circle-radius": style.point["circle-radius"],
          "circle-color": "#FFFFFF",
          "circle-opacity": style.opacity,
        },
        filter: filter(style.property, "Point"),
        _zorder: 1,
      },
      polygon: {
        type: "fill",
        paint: {
          "fill-color": colorStyle(style.property, style.polygon, attributeStats),
          "fill-outline-color": style.property == null ? style.polygon["fill-outline-color"] : style.polygon.color,
          "fill-opacity": style.opacity,
        },
        filter: filter(style.property, "Polygon"),
      },
      line: {
        type: "line",
        paint: {
          "line-color": colorStyle(style.property, style.line, attributeStats),
          "line-width": 5,
        },
        filter: ["==", "$type", "LineString"],
      },
    };
  }
}
