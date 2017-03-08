var path = require('path');
var webpack = require('webpack');

var config = {

  output: {
    path: path.resolve(process.cwd(), 'build', 'webpack', 'assets'),
    filename: 'bundle.js'
  },

  resolve: {
    extensions: ['', '.ts', '.tsx', '.js', '.jsx' ,'.json'],
  },

  module: {
    loaders: [
      // See (for TS -> Babel): http://www.jbrantly.com/es6-modules-with-typescript-and-webpack/
      {
        test: /\.tsx?$/,
        loader: 'react-hot!babel-loader!ts-loader',
        include: path.join(__dirname, '../../app')
      },
      {
        test: /\.json$/,
        loader: 'json'
      },
      {
        test: /\.css$/,
        include: /app/,
        loaders: [
          'style',
          'css?modules&importLoaders=2&sourceMap&localIdentName=[local]__[hash:base64:5]',
          'postcss'
        ]
      },
      {
        test: /\.css$/,
        exclude: /app/,
        loader: 'style!css'
      },
      {
        test: /\.eot(\?.*)?$/,
        loader: "file?name=fonts/[hash].[ext]",
      },
      {
        test: /\.(woff|woff2)(\?.*)?$/,
        loader:"file-loader?name=fonts/[hash].[ext]",
      },
      {
        test: /\.ttf(\?.*)?$/,
        loader: "url?limit=10000&mimetype=application/octet-stream&name=fonts/[hash].[ext]",
      },
      {
        test: /\.svg(\?.*)?$/,
        loader: "url?limit=10000&mimetype=image/svg+xml&name=fonts/[hash].[ext]",
        include: /app/
      },
      {
        test: /\.(jpe?g|png|gif)$/i,
        loader: 'url?limit=1000&name=images/[hash].[ext]',
        include: /app/
      },
    ],

    preLoaders: [
      { test: /\.js$/, loader: "source-map-loader" }
    ]
  },

  postcss: function () {
    return [
      require("postcss-cssnext")(),
    ];
  }

}

module.exports = config;
