import * as chroma from "chroma-js";

import { customStyles, liveLayerStyle } from "./customStyles.js";

function computeCategoricalStops(categories) {
  // Mapbox seems to complain if the categories are not sorted.
  return categories.sort()
    .map((c, i) => [c, chroma.hsl((360 * i) / categories.length, 0.8, 0.5).toString(0)]);
}

function computeNumericStops(colorScale, numStops, minValue, maxValue) {
  return chroma.scale(colorScale).colors(numStops)
    .map((c, i) => [minValue + ((i * (maxValue - minValue)) / numStops), c.toString(0)]);
}

 // Dynamic info may arrive later (i.e. asynchronously wrt the static info), so account for this by bombing out gracefully in various places
function colorStyle(attribute, style, attributes, attributeRange) {
  if (!attribute) {
    return style.color;
  }

  const attributeInfo = attributes[attribute];
  if (!attributeInfo) {
    return style.color;
  }

  if (!attributeInfo.categories) {
    if (!attributeRange) {
      return style.color;
    }

    return {
      "property": attribute,
      "stops": computeNumericStops(style.colorScale, 8, attributeRange.minimum, attributeRange.maximum),
    };
  }

  return {
    "property": attribute,
    "type": "categorical",
    "stops": computeCategoricalStops(attributes[attribute].categories),
  };
}

function filter(attribute, geomType, attributeRange) {
  const geomFilter = ["==", "$type", geomType];
  if (attribute != null) {
    const attrFilter = ["has", attribute];
    if (attributeRange != null) {
      const rangefilter = ["all", ["<=", attribute, attributeRange.maximum], [">=", attribute, attributeRange.minimum]];
      return ["all", geomFilter, attrFilter, rangefilter];
    }
    return ["all", geomFilter, attrFilter];
  }
  return geomFilter;
}

export function buildStyleLayers(layer) {
  const [layerName, style, attributes, attributeRange] = [layer.metadata.name, layer.style, layer.dynamicSchema.attributes, layer.style.attributeRange];
  if (layerName in customStyles) {
    return customStyles[layerName];
  } else if (layer.live) {
    return Object.assign({}, liveLayerStyle, { polygon: polygonSpec(style, attributes, attributeRange) });
  }
  return {
    point: {
      type: "circle",
      paint: {
        "circle-radius": style.point["circle-radius"] - 2,
        "circle-color": colorStyle(style.attribute, style.point, attributes, attributeRange),
        "circle-opacity": style.opacity,
      },
      filter: filter(style.attribute, "Point", attributeRange),
    },
    point2: {
      type: "circle",
      paint: {
        "circle-radius": style.point["circle-radius"],
        "circle-color": "#FFFFFF",
        "circle-opacity": style.opacity,
      },
      filter: filter(style.attribute, "Point", attributeRange),
      _zorder: 1,
    },
    polygon: polygonSpec(style, attributes, attributeRange),
    line: {
      type: "line",
      paint: {
        "line-color": colorStyle(style.attribute, style.line, attributes, attributeRange),
        "line-width": 5,
      },
      filter: filter(style.attribute, "LineString", attributeRange),
    },
  };
}

const polygonSpec = (style, attributes, attributeRange) => ({
  type: "fill",
  paint: {
    "fill-color": style.isTransparent ? "rgba(0, 0, 0, 0)" : colorStyle(style.attribute, style.polygon, attributes, attributeRange),
    "fill-outline-color": style.polygon["fill-outline-color"],
    "fill-opacity": style.opacity,
  },
  filter: filter(style.attribute, "Polygon"),
});
