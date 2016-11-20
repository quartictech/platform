var path = require('path');
var webpack = require('webpack');
var CopyWebpackPlugin = require('copy-webpack-plugin');

var baseConfig = require('./base');

var config = Object.assign({}, baseConfig, {
  bail: true,

  entry: [ './app/index.tsx' ],

  plugins: [
    new webpack.DefinePlugin({
      'process.env':{
        'NODE_ENV': JSON.stringify('production')
      }
    }),
    // new webpack.optimize.UglifyJsPlugin({
    //   compressor: {
    //     warnings: false,
    //   }
    // }),
		new CopyWebpackPlugin([{from: "public"}]),
    new webpack.optimize.OccurenceOrderPlugin(),
    new webpack.optimize.DedupePlugin()
  ]
});

module.exports = config;
