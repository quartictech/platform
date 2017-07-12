
const tubeColorStops = [
  ["Bakerloo", "#B36305"],
  ["Central", "#E32017"],
  ["Circle", "#FFD300"],
  ["Crossrail", "#7156A5"],
  ["District", "#00782A"],
  ["DLR", "#00A4A7"],
  ["Emirates Air Line", "#E51836"],
  ["Hammersmith & City", "#F3A9BB"],
  ["Jubilee", "#A0A5A9"],
  ["London Overground", "#EE7C0E"],
  ["Metropolitan", "#9B0056"],
  ["Northern", "#000000"],
  ["Piccadilly", "#003688"],
  ["Tramlink", "#84B817"],
  ["Victoria", "#0098D4"],
  ["Waterloo & City", "#95CDBA"],
];

const tubeLineStyles = {
  line: {
    type: "line",
    paint: {
      "line-color": {
        property: "name",
        type: "categorical",
        stops: tubeColorStops,
      },
      "line-width": 5,
    },
  },
};

const greenSpacesStyles = {
  polygon: {
    type: "fill",
    paint: {
      "fill-color": "#86C67C",
      "fill-opacity": 0.7,
    },
  },
  line: {
    type: "line",
    paint: {
      "line-color": "#2F4F2F",
      "line-width": 5,
    },
    _zorder: 1,
  },
};

const residentialBuildingsStyles = {
  polygon: {
    type: "fill",
    paint: {
      "fill-color": "#4584A3",
      "fill-opacity": 0.7,
    },
  },
  line: {
    type: "line",
    paint: {
      "line-color": "#50ADB2",
      "line-width": 3,
    },
    _zorder: 1,
  },
};

const residentialLandUseStyles = {
  polygon: {
    type: "fill",
    paint: {
      "fill-color": "#F3E4AD",
      "fill-opacity": 0.7,
    },
  },
  line: {
    type: "line",
    paint: {
      "line-color": "#A3825F",
      "line-width": 5,
    },
    _zorder: 1,
  },
};


const nightLifeStyles = {
  // These are for the layer from OSM (polygons)
  polygon: {
    type: "fill",
    paint: {
      "fill-color": "#807788",
      "fill-opacity": 0.7,
    },
    filter: ["==", "$type", "Polygon"],
  },
  // MapboxGL doesn't have fill-outline-width so we add a separate line layer
  line: {
    type: "line",
    paint: {
      "line-color": "#ABA491",
      "line-width": 5,
    },
    filter: ["==", "$type", "Polygon"],
    _zorder: 1,
  },
  // This is for the point-like dataset from OSM
  point: {
    type: "circle",
    paint: {
      "circle-color": "#807788",
      "circle-opacity": 0.7,
      "circle-radius": 7,
    },
    filter: ["==", "$type", "Point"],
  },
};

const roadsStyles = {
  line: {
    type: "line",
    paint: {
      "line-color": "#FF8D7C", // CAC9EF",
      "line-width": {
        stops: [
          [5, 0.1],
          [10, 1],
          [15, 5],
        ],
      },
    },
  },
};

const liveLayerStyle = {
  point: {
    "type": "circle",
    "paint": {
      "circle-radius": 5,
      "circle-color": {
        property: "name",
        stops: [
          ["Alex", "#e7298a"],
          ["Arlo", "#AAFD6F"],
        ],
        type: "categorical",
      },
    },
    "filter": ["==", "$type", "Point"],
    "_zorder": 0,
  },
  point2: {
    "type": "circle",
    "paint": {
      "circle-radius": 7,
      "circle-color": "#FFFFFF",
    },
    "filter": ["==", "$type", "Point"],
    "_zorder": 1,
  },
  line: {
    "type": "line",
    "paint": {
      "line-opacity": 0.5,
      "line-width": 2,
      "line-color": {
        property: "name",
        stops: [
          ["Alex", "#e7298a"],
          ["Arlo", "#AAFD6F"],
        ],
        type: "categorical",
      },
    },
    "filter": ["==", "$type", "LineString"],
    "_zorder": 4,
  },
  line2: {
    "type": "circle",
    "paint": {
      "circle-radius": 5,
      "circle-opacity": 0.5,
      "circle-color": {
        property: "name",
        stops: [
          ["Alex", "#e7298a"],
          ["Arlo", "#AAFD6F"],
        ],
        type: "categorical",
      },
    },
    "filter": ["==", "$type", "LineString"],
    "_zorder": 3,
  },
  line3: {
    "type": "circle",
    "paint": {
      "circle-radius": 7,
      "circle-opacity": 0.5,
      "circle-color": "#FFFFFF",
    },
    "filter": ["==", "$type", "LineString"],
    "_zorder": 4,
  },
};

const customStyles = {
  "TFL Lines": tubeLineStyles,
  "Green Spaces": greenSpacesStyles,
  "Nightlife": nightLifeStyles,
  "Roads": roadsStyles,
  "Green Belts 2014-2015": greenSpacesStyles,
  "Residential Buildings": residentialBuildingsStyles,
  "Residential Land Use": residentialLandUseStyles,
};

export { customStyles, liveLayerStyle };
