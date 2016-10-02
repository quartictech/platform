/* eslint consistent-return:0 */

const express = require("express");
const logger = require("./logger");

const argv = require("minimist")(process.argv.slice(2));
const setup = require("./middlewares/frontendMiddleware");
const isDev = process.env.NODE_ENV !== "production";
const ngrok = (isDev && process.env.ENABLE_TUNNEL) || argv.tunnel ? require("ngrok") : false;
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
  publicPath: "/",
});

// get the intended port number, use port 3000 if not provided
const port = argv.port || process.env.PORT || 3000;


// Start your app.
const server = app.listen(port, (err) => {
  if (err) {
    return logger.error(err.message);
  }

  // Connect to ngrok in dev mode
  if (ngrok) {
    ngrok.connect(port, (innerErr, url) => {
      if (innerErr) {
        return logger.error(innerErr);
      }

      logger.appStarted(port, url);
    });
  } else {
    logger.appStarted(port);
  }
});

server.on("upgrade", wsProxy.upgrade);
