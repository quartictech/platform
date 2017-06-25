var path = require('path');
var webpack = require('webpack');

var config = {

  output: {
    path: path.resolve(process.cwd(), 'build', 'webpack', 'assets'),
    filename: 'bundle.js'
  },

  node: {
    fs: "empty"
  },

  resolve: {
    root: [ path.resolve(process.cwd(), "src") ],
    extensions: ['', '.ts', '.tsx', '.js', '.jsx' ,'.json'],
  },

  module: {
    noParse: /node_modules\/mapbox-gl\/dist\/mapbox-gl.js/, // See https://github.com/mapbox/mapbox-gl-js/issues/2742#issuecomment-267001402
    loaders: [
      // See (for TS -> Babel): http://www.jbrantly.com/es6-modules-with-typescript-and-webpack/
      // See http://jamesknelson.com/using-es6-in-the-browser-with-babel-6-and-webpack/
      {
        test: /\.tsx?$/,
        loaders: ['react-hot', 'babel-loader?' + JSON.stringify({
          plugins: ['transform-runtime'],
          presets: ['es2015'],
        }), 'ts-loader'],
        include: /app/,
      },
      {
        test: /\.json$/,
        loader: 'json'
      },
      {
      // CSS files named appropriately get loaded into global scope (for plottable)
        test: /\.css.global$/,
        exclude: /node_modules/,
        loaders: ['style-loader', 'css-loader'],
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
