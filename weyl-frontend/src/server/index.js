const express = require("express");
const setup = require("./middlewares/frontendMiddleware");
const resolve = require("path").resolve;
const app = express();

// Stuff to proxy to backend during dev
const proxy = require("http-proxy-middleware");
const apiProxy = proxy("/api", { target: "http://localhost:8080" });
const wsProxy = proxy("/ws", { target: "ws://localhost:8080" });
app.use(apiProxy);
app.use(wsProxy);

// In production we need to pass these values in instead of relying on webpack
setup(app, {
  outputPath: resolve(process.cwd(), "build"),
  publicPath: "",
});

const server = app.listen(3000, (err) => {
  if (err) {
    console.error(err);
    return;
  }

  console.log("Listening at http://localhost:3000"); // eslint-disable-line no-console
});

server.on("upgrade", wsProxy.upgrade);
