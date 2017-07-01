const path = require("path");
const fs = require("fs");
const webpack = require("webpack");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const logger = require("../../src/server/logger");

// PostCSS plugins
const cssnext = require("postcss-cssnext");
const postcssFocus = require("postcss-focus");
const postcssReporter = require("postcss-reporter");

const plugins = [
  new webpack.HotModuleReplacementPlugin(), // Tell webpack we want hot reloading
  new webpack.NoEmitOnErrorsPlugin(),
  new HtmlWebpackPlugin({
    inject: true, // Inject all files that are generated by webpack, e.g. bundle.js
    templateContent: fs.readFileSync(path.resolve(process.cwd(), "src/app/index.html")).toString(), // eslint-disable-line no-use-before-define
  }),
];

module.exports = require("./base")({
  // Add hot reloading in development
  entry: [
    "eventsource-polyfill", // Necessary for hot reloading with IE
    "webpack-hot-middleware/client",
    "./src/app/app.js", // Start with js/app.js
  ],

  // Don"t use hashes in dev mode for better performance
  output: {
    filename: "[name].js",
    chunkFilename: "[name].chunk.js",
  },

  // Add development plugins
  plugins: [
    new webpack.optimize.CommonsChunkPlugin({
      name: "vendor",
      children: true,
      minChunks: 2,
      async: true,
    }),
  ].concat(plugins), // eslint-disable-line no-use-before-define

  // Load the CSS in a style tag in development
  cssLoaders: [
    "style-loader",
    {
      loader: "css-loader",
      options: {
        localIdentName: "[local]__[hash:base64:5]",
        modules: true,
        importLoaders: 1,
        sourceMap: true,
      },
    },
    {
      loader: "postcss-loader",
      options: {
        plugins: () => [
          postcssFocus(), // Add a :focus to every :hover
          cssnext({ // Allow future CSS features to be used, also auto-prefixes the CSS...
            browsers: ["last 2 versions", "IE > 10"], // ...based on this browser list
          }),
          postcssReporter({ // Posts messages from plugins to the terminal
            clearMessages: true,
          }),
        ],
      },
    },
  ],

  // Tell babel that we want to hot-reload
  babelOptions: {
    presets: ["react-hmre"],
  },

  // Emit a source map for easier debugging
  devtool: "source-map",
});
