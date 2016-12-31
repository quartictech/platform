/**
 * COMMON WEBPACK CONFIGURATION
 */

const path = require('path');
const webpack = require('webpack');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const StringReplacePlugin = require('string-replace-webpack-plugin');

const strRepLoader = StringReplacePlugin.replace({
  replacements: [
    {
      pattern: /#(10161a|182026|202b33|293742|30404d|394b59|5c7080|738694|8a9ba8|a7b6c2|bfccd6|ced9e0|d8e1e8|e1e8ed|ebf1f5|f5f8fa)/g,
      replacement: (m) => `#${m.substr(5, 2)}${m.substr(1, 2)}${m.substr(3, 2)}`,
    }
  ]});

module.exports = (options) => ({
  entry: options.entry,
  output: Object.assign({ // Compile into js/build.js
    path: path.resolve(process.cwd(), 'build', 'webpack', 'assets'),
    publicPath: '',
  }, options.output), // Merge with env dependent settings
  module: {
    loaders: [{
      test: /\.js$/, // Transform all .js files required somewhere with Babel
      loader: 'babel',
      exclude: /node_modules/,
      query: options.babelQuery,
    }, {
      // Transform our own .css files with PostCSS and CSS-modules
      test: /\.css$/,
      exclude: /node_modules/,
      loader: options.cssLoaders,
    }, {
      // Do not transform vendor's CSS with CSS-modules
      // The point is that they remain in global scope.
      // Since we require these CSS files in our JS or CSS files,
      // they will be a part of our compilation either way.
      // So, no need for ExtractTextPlugin here.
      test: /\.css$/,
      include: /node_modules/,
      loaders: ['style-loader', 'css-loader', strRepLoader],
    }, {
      // CSS files named appropriately get loaded into global scope (for plottable)
      test: /\.css.global$/,
      exclude: /node_modules/,
      loaders: ['style-loader', 'css-loader'],
    }, {
      test: /\.(eot|svg|ttf|woff|woff2)$/,
      loader: 'file-loader',
    }, {
      test: /\.(jpg|png|gif)$/,
      loaders: [
        'file-loader',
        'image-webpack?{progressive:true, optimizationLevel: 7, interlaced: false, pngquant:{quality: "65-90", speed: 4}}',
      ],
    }, {
      test: /\.html$/,
      loader: 'html-loader',
    }, {
      test: /\.json$/,
      loader: 'json-loader',
    }, {
      test: /\.(mp4|webm)$/,
      loader: 'url-loader?limit=10000',
    }],
  },
  plugins: options.plugins.concat([
    new webpack.ProvidePlugin({
      // make fetch available
      fetch: 'exports?self.fetch!whatwg-fetch',
    }),

    // Always expose NODE_ENV to webpack, in order to use `process.env.NODE_ENV`
    // inside your code for any environment checks; UglifyJS will automatically
    // drop any unreachable code.
    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: JSON.stringify(process.env.NODE_ENV),
        BUILD_VERSION: JSON.stringify(process.env.BUILD_VERSION),
      },
    }),

    new CopyWebpackPlugin([{ from: 'static' }]),

    // Some BS for moment.js (see https://github.com/moment/moment/issues/2979)
    // If you ever remove this line, also remove the devDependency on empty-module
    new webpack.ContextReplacementPlugin(/\.\/locale$/, 'empty-module', false, /js$/),

    new StringReplacePlugin(),
  ]),
  postcss: () => options.postcssPlugins,
  resolve: {
    modules: ['app', 'node_modules'],
    extensions: [
      '',
      '.js',
      '.jsx',
      '.react.js',
    ],
    mainFields: [
      'jsnext:main',
      'main',
    ],
  },
  // This is needed to make geojsonhint (imported via mapbox-gl-draw) happy
  // See e.g. https://github.com/pugjs/pug-loader/issues/8
  node: {
    fs: "empty",
    net: "empty",
    debug: "empty",
  },
  devtool: options.devtool,
  target: 'web', // Make web variables accessible to webpack, e.g. window
  stats: false, // Don't show stats in the console
  progress: true,
});
