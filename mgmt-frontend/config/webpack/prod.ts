import * as webpack from "webpack";
import CopyWebpackPlugin from "copy-webpack-plugin";
import * as baseConfig from "./base";

const config = Object.assign({}, baseConfig, {
  bail: true,

  entry: [ "./src/app/index.tsx" ],

  plugins: [
    new webpack.DefinePlugin({
      "process.env":{
        "NODE_ENV": JSON.stringify("production"),
        "BUILD_VERSION": JSON.stringify(process.env.BUILD_VERSION),
      }
    }),
    new webpack.optimize.UglifyJsPlugin({
      minimize: true,
    }),
		new CopyWebpackPlugin([{from: "src/public"}]),
  ],
});

module.exports = config;
