import * as webpack from "webpack"
import CopyWebpackPlugin from "copy-webpack-plugin";
import * as baseConfig from "./base";

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
		new webpack.NoEmitOnErrorsPlugin(),
		new CopyWebpackPlugin([{from: "src/public"}]),
	]

});

module.exports = config;
