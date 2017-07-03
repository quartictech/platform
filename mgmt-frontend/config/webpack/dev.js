var path = require("path");
var webpack = require("webpack");

var baseConfig = require("./base");

var config = Object.assign({}, baseConfig, {
	devtool: "source-map",

	entry: [
		"webpack-hot-middleware/client?reload=true",
		"./src/app/index.tsx"
	],

	plugins: [
		new webpack.DefinePlugin({
		  "process.env":{
		    "NODE_ENV": JSON.stringify("development")
		  }
		}),
		new webpack.HotModuleReplacementPlugin(),
		new webpack.NoEmitOnErrorsPlugin()
	]

});

module.exports = config;
