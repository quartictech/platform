// import mapboxgl from "mapbox-gl/dist/mapbox-gl.js";
const mapboxgl = require("mapbox-gl/dist/mapbox-gl.js");
mapboxgl.accessToken = "pk.eyJ1IjoiYWxzcGFyIiwiYSI6ImNpcXhybzVnZTAwNTBpNW5uaXAzbThmeWEifQ.s_Z4AWim5WwKa0adU9P2Uw";
import "mapbox-gl/dist/mapbox-gl.css";
(<any>window).mapboxgl = mapboxgl;
export default mapboxgl;
