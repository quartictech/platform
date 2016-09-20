
const tubeColorStops = [
  ["Bakerloo Line", "#B36305"],
  ["Central Line", "#E32017"],
  ["Circle", "#FFD300"],
  ["Crossrail", "#7156A5"],
  ["District", "#00782A"],
  ["DLR", "#00A4A7"],
  ["Emirates Air Line", "#E51836"],
  ["Hammersmith & City", "#F3A9BB"],
  ["Jubilee Line", "#A0A5A9"],
  ["London Overground", "#EE7C0E"],
  ["Metropolitan Line", "#9B0056"],
  ["Northern", "#000000"],
  ["Piccadilly", "#003688"],
  ["Tramlink", "#84B817"],
  ["Victoria Line", "#0098D4"],
  ["Waterloo & City Line", "#95CDBA"],
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


const nightLifeStyles = {
  polygon: {
    type: "fill",
    paint: {
      "fill-color": "#807788",
      "fill-opacity": 0.7,
    },
  },
  line: {
    type: "line",
    paint: {
      "line-color": "#ABA491",
      "line-width": 5,
    },
    _zorder: 1,
  },
  point: {
    type: "circle",
    paint: {
      "circle-color": "#807788",
      "circle-opacity": 0.7,
      "circle-radius": 7,
    },
  },
};

const roadsStyles = {
  line: {
    type: "line",
    paint: {
      "line-color": "#626262",
      "line-width": {
        stops: [
          [5, 0.2],
          [10, 2],
          [15, 5],
        ],
      },
    },
  },
};

const customStyles = {
  "TFL Lines": tubeLineStyles,
  "Green Spaces": greenSpacesStyles,
  "Nightlife": nightLifeStyles,
  "Roads": roadsStyles,
};

export { customStyles };
