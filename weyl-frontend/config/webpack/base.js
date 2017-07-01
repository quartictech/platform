const path = require("path");
const webpack = require("webpack");
const CopyWebpackPlugin = require("copy-webpack-plugin");

module.exports = (options) => ({
  entry: options.entry,

  output: Object.assign({ // Compile into js/build.js
    path: path.resolve(process.cwd(), "build", "webpack", "assets"),
    publicPath: "",
  }, options.output), // Merge with env dependent settings

  module: {
    rules: [
      {
        test: /\.js$/, // Transform all .js files required somewhere with Babel
        exclude: /node_modules/,
        use: [
          {
            loader: "babel-loader",
            options: options.babelOptions,
          },
        ],
      },
      {
        // Transform our own .css files with PostCSS and CSS-modules
        test: /\.css$/,
        exclude: /node_modules/,
        use: options.cssLoaders,
      },
      {
        // Do not transform vendor's CSS with CSS-modules
        // The point is that they remain in global scope.
        // Since we require these CSS files in our JS or CSS files,
        // they will be a part of our compilation either way.
        // So, no need for ExtractTextPlugin here.
        test: /\.css$/,
        include: /node_modules/,
        use: [
          "style-loader",
          "css-loader",
        ],
      },
      {
        // CSS files named appropriately get loaded into global scope (for plottable)
        test: /\.css.global$/,
        exclude: /node_modules/,
        use: [
          "style-loader",
          "css-loader",
        ],
      },
      {
        test: /\.(eot|svg|ttf|woff|woff2)$/,
        use: [
          "file-loader",
        ],
      },
      {
        test: /\.(jpg|png|gif)$/,
        use: [
          "file-loader",
          {
            loader: "image-webpack-loader",
            options: {
              progressive: true,
              optimizationLevel: 7,
              interlaced: false,
              pngquant: {
                quality: "65-90",
                speed: 4,
              },
            },
          },
        ],
      },
      {
        test: /\.html$/,
        use: [
          "html-loader",
        ],
      },
      {
        test: /\.(mp4|webm)$/,
        use: [
          {
            loader: "url-loader",
            options: {
              limit: 10000,
            },
          },
        ],
      },
    ],
  },

  plugins: options.plugins.concat([
    // TODO - eliminate this
    new webpack.LoaderOptionsPlugin({
      options: {
        postcss: () => options.postcssPlugins,
      }
    }),

    new webpack.ProvidePlugin({
      // make fetch available
      fetch: "exports-loader?self.fetch!whatwg-fetch",
    }),

    // Always expose NODE_ENV to webpack, in order to use `process.env.NODE_ENV`
    // inside your code for any environment checks; UglifyJS will automatically
    // drop any unreachable code.
    new webpack.DefinePlugin({
      "process.env": {
        NODE_ENV: JSON.stringify(process.env.NODE_ENV),
        BUILD_VERSION: JSON.stringify(process.env.BUILD_VERSION),
      },
    }),

    new CopyWebpackPlugin([{ from: "src/static" }]),

    // Some BS for moment.js (see https://github.com/moment/moment/issues/2979)
    // If you ever remove this line, also remove the devDependency on empty-module
    new webpack.ContextReplacementPlugin(/\.\/locale$/, "empty-module", false, /js$/),
  ]),

  resolve: {
    modules: [
      "node_modules",                       // TODO - replace with env.node_modules_dir
      path.resolve("./src/app"),
    ],
    extensions: [".ts", ".tsx", ".js", ".jsx" ,".json"],
  },

  // This is needed to make geojsonhint (imported via mapbox-gl-draw) happy
  // See e.g. https://github.com/pugjs/pug-loader/issues/8
  node: {
    fs: "empty",
    net: "empty",
    debug: "empty",
  },

  devtool: options.devtool,

  target: "web", // Make web variables accessible to webpack, e.g. window

  stats: {
    assets: false,
    chunks: false,
    errorDetails: true,
  },
});
