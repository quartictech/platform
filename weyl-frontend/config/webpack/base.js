const path = require("path");
const webpack = require("webpack");
const CopyWebpackPlugin = require("copy-webpack-plugin");
const cssnext = require("postcss-cssnext");
const postcssFocus = require("postcss-focus");
const postcssReporter = require("postcss-reporter");

module.exports = {
  output: {
    path: path.resolve("build", "webpack", "assets"),
    publicPath: "",
  },

  // This is needed to make geojsonhint (imported via mapbox-gl-draw) happy
  // See e.g. https://github.com/pugjs/pug-loader/issues/8
  node: {
    fs: "empty",
    net: "empty",
    debug: "empty",
  },

  resolve: {
    modules: [
      "node_modules",                       // TODO - replace with env.node_modules_dir
      path.resolve("./src/app"),
    ],
    extensions: [".ts", ".tsx", ".js", ".jsx" ,".json"],
  },

  stats: {
    assets: false,
    chunks: false,
    errorDetails: true,
  },

  module: {
    noParse: /(mapbox-gl)\.js$/, // Just because (see https://github.com/mapbox/mapbox-gl-js/issues/4359#issuecomment-288001933)
    rules: [
      {
        test: /\.js$/, // Transform all .js files required somewhere with Babel
        exclude: /node_modules/,
        use: "babel-loader",
      },
      {
        // Transform our own .css files with PostCSS and CSS-modules
        test: /\.css$/,
        exclude: /node_modules/,
        use: [
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
        use: "file-loader",
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
        use: "html-loader",
      },
      {
        test: /\.(mp4|webm)$/,
        use: {
          loader: "url-loader",
          options: {
            limit: 10000,
          },
        },
      },
    ],
  },

  plugins: [
    new webpack.ProvidePlugin({
      fetch: "exports-loader?self.fetch!whatwg-fetch",        // make fetch available
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
  ],
};
