export const themes = {
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
    next: "blank",
    label: "Satellite",
    icon: "rocket",
    mapbox: "mapbox://styles/mapbox/satellite-streets-v9",
  },
  blank: {
    next: "light",
    label: "Blank",
    icon: "square",
    mapbox: "mapbox://styles/mapbox/empty-v9",
  },
};
