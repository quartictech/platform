export const mapThemes = {
  light: {
    next: "dark",
    label: "Light",
    icon: "sun",
    mapbox: "mapbox://styles/mapbox/outdoors-v9",
  },
  dark: {
    next: "satellite",
    label: "Dark",
    icon: "moon",
    mapbox: "mapbox://styles/mapbox/dark-v9",
  },
  satellite: {
    next: "light",
    label: "Satellite",
    icon: "rocket",
    mapbox: "mapbox://styles/mapbox/satellite-streets-v9",
  },
};

export const layerThemes = [
  {
    name: "red",
    fill: "#67001f",
    line: "#e7298a",
    colorScale: ["white", "#67001f"],
  },
  {
    name: "orange",
    fill: "#673d00",
    line: "#e76529",
    colorScale: ["white", "#673d00"],
  },
  {
    name: "green",
    fill: "#00671f",
    line: "#29e78a",
    colorScale: ["white", "#00671f"],
  },
  {
    name: "blue",
    fill: "#001f67",
    line: "#298ae7",
    colorScale: ["white", "#001f67"],
  },
  {
    name: "purple",
    fill: "#1f0067",
    line: "#8a29e7",
    colorScale: ["white", "#1f0067"],
  },
];
