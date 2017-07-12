const path = require("path");
const express = require("express");
const webpack = require("webpack");
const config = require("../config/webpack/dev");

const app = express();
const compiler = webpack(config);

// Stuff to proxy to backend during dev
const proxy = require("http-proxy-middleware");
app.use(proxy("/api", { target: "http://localhost:8160" }));
app.use(proxy("/ws", { target: "ws://localhost:8160" }));

app.use(require("webpack-dev-middleware")(compiler, {
	noInfo: true,
	publicPath: config.output.publicPath,
	stats: {
		timings: false,
		hash: false,
		version: false,
		assets: false,
		chunks: false,
		colors: true,
	},
}));

app.use(require("webpack-hot-middleware")(compiler));

app.get("*", function (_, res) {
	res.sendFile(path.join(__dirname, "public/index.html"));
});

app.listen(3020, "localhost", function (err) {
	if (err) {
		console.error(err);
		return;
	}

	console.log("Listening at http://localhost:3020");
});
