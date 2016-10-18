import * as chroma from "chroma-js";

import { customStyles, liveLayerStyle } from "./customStyles.js";

function computeStops(colorScale, nStops, minValue, maxValue) {
  const result = [];
  const colors = chroma.scale(colorScale).colors(nStops);

  for (let i = 0; i < nStops; i++) {
    result.push([minValue + ((i * (maxValue - minValue)) / nStops), colors[i].toString(0)]);
  }
  return result;
}

function colorStyle(attribute, style, attributeStats) {
  if (attribute == null) {
    return style.color;
  }
  return {
    "property": attribute,
    "stops": computeStops(style.colorScale, 8, attributeStats[attribute].minimum, attributeStats[attribute].maximum),
  };
}

function filter(attribute, geomType) {
  const geomFilter = ["==", "$type", geomType];
  if (attribute != null) {
    const attrFilter = ["has", attribute];
    return ["all", geomFilter, attrFilter];
  }
  return geomFilter;
}

export function buildStyleLayers(layer) {
  const [layerName, style, attributeStats] = [layer.metadata.name, layer.style, layer.stats.attributeStats];
  if (layerName in customStyles) {
    return customStyles[layerName];
  } else if (layer.live) {
    return Object.assign({}, liveLayerStyle, { polygon: polygonSpec(style, attributeStats) });
  }
  return {
    point: {
      type: "circle",
      paint: {
        "circle-radius": style.point["circle-radius"] - 2,
        "circle-color": colorStyle(style.attribute, style.point, attributeStats),
        "circle-opacity": style.opacity,
      },
      filter: filter(style.attribute, "Point"),
    },
    point2: {
      type: "circle",
      paint: {
        "circle-radius": style.point["circle-radius"],
        "circle-color": "#FFFFFF",
        "circle-opacity": style.opacity,
      },
      filter: filter(style.attribute, "Point"),
      _zorder: 1,
    },
    polygon: polygonSpec(style, attributeStats),
    line: {
      type: "line",
      paint: {
        "line-color": colorStyle(style.attribute, style.line, attributeStats),
        "line-width": 5,
      },
      filter: ["==", "$type", "LineString"],
    },
  };
}

const polygonSpec = (style, attributeStats) => ({
  type: "fill",
  paint: {
    "fill-color": colorStyle(style.attribute, style.polygon, attributeStats),
    "fill-outline-color": style.attribute == null ? style.polygon["fill-outline-color"] : style.polygon.color,
    "fill-opacity": style.opacity,
  },
  filter: filter(style.attribute, "Polygon"),
});
