import { openMapTiles } from "./openmaptiles";

export const mapThemes = {
  light: {
    label: "Light",
    icon: "flash",
    mapbox: "mapbox://styles/mapbox/outdoors-v9",
  },
  dark: {
    label: "Dark",
    icon: "moon",
    mapbox: "mapbox://styles/mapbox/dark-v9",
  },
  satellite: {
    label: "Satellite",
    icon: "globe",
    mapbox: "mapbox://styles/mapbox/satellite-streets-v9",
  },
  mapzen: {
    label: "Open Map Tiles - OSM Bright",
    icon: "globe",
    mapbox: openMapTiles,
  },
  blank: {
    label: "Blank",
    icon: "square",
    mapbox: "mapbox://styles/mapbox/empty-v9",
  },
};

export const layerThemes = [
  {
    name: "Red",
    fill: "#67001f",
    line: "#e7298a",
    colorScale: ["white", "#67001f"],
  },
  {
    name: "Orange",
    fill: "#673d00",
    line: "#e76529",
    colorScale: ["white", "#673d00"],
  },
  {
    name: "Green",
    fill: "#00671f",
    line: "#29e78a",
    colorScale: ["white", "#00671f"],
  },
  {
    name: "Blue",
    fill: "#001f67",
    line: "#298ae7",
    colorScale: ["white", "#001f67"],
  },
  {
    name: "Purple",
    fill: "#1f0067",
    line: "#8a29e7",
    colorScale: ["white", "#1f0067"],
  },
];
