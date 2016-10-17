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
  },
  {
    name: "green",
    fill: "#00671f",
    line: "#29e78a",
  },
  {
    name: "blue",
    fill: "#001f67",
    line: "#298ae7",
  },
  {
    name: "purple",
    fill: "#1f0067",
    line: "#8a29e7",
  },
];
