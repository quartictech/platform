import { apiRoot } from "../../../weylConfig.js";
const $ = require("jquery");

const oldViolations = {};

const requestViolations = (handler) => {
  $.getJSON(`${apiRoot}/geofence/violations`, (data) => {
    Object.keys(data)
      .filter(k => !(k in oldViolations))
      .forEach(k => handler(k, data[k]));
  });
};

const initViolations = () => {
  Notification.requestPermission().then(result => {
    console.log("requestPermission result:");
    console.log(result);
  });

  requestViolations((k, v) => (oldViolations[k] = v)); // Seed things
};

const pollViolations = () => {
  requestViolations((k, v) => {
    oldViolations[k] = v;
    const n = new Notification("Geofence violation", {
      body: v.message,
      tag: k,
    });
    setTimeout(n.close.bind(n), 5000);
  });
};

export function startPolling() {
  initViolations();
  console.log("Polling started");

  window.setInterval(
    () => {
      pollViolations();
    },
    2000
  );
}
