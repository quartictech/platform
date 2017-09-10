import * as path from "path";

module.exports = {
  output: {
    path: path.resolve("build", "bundle"),
    filename: "bundle.js"
  },

  resolve: {
    modules: ["node_modules"],  // TODO - replace with env.node_modules_dir
    extensions: [".ts", ".tsx", ".js", ".jsx" ,".json"],
  },

  module: {
    noParse: /(mapbox-gl)\.js$/, // Just because (see https://github.com/mapbox/mapbox-gl-js/issues/4359#issuecomment-288001933)
    rules: [
      {
        test: /\.js$/,
        enforce: "pre",
        use: [
          "source-map-loader",
        ],
        // Needed to suppress warnings from apollo-client
        exclude: [/node_modules/, /build/, /__test__/],
      },
      // See (for TS -> Babel): http://www.jbrantly.com/es6-modules-with-typescript-and-webpack/
      // See http://jamesknelson.com/using-es6-in-the-browser-with-babel-6-and-webpack/
      {
        test: /\.tsx?$/,
        include: /app/,
        use: [
          "react-hot-loader", // TODO - should only need this for prod builds, and should switch to react-hmre
          {
            loader: "babel-loader",
            options: {
              plugins: ["transform-runtime"],
              presets: ["es2015"],
            },
          },
          "ts-loader",
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
        test: /\.css$/,
        include: /app/,
        use: [
          "style-loader",
          {
            loader: "css-loader",
            options: {
              modules: true,
              importLoaders: 1,
              sourceMap: true,
              localIdentName: "[local]__[hash:base64:5]",
            },
          },
          {
            loader: "postcss-loader",
            options: {
              plugins: () => require("postcss-cssnext")(),
            },
          },
        ],
      },
      {
        test: /\.css$/,
        exclude: /app/,
        use: [
          "style-loader",
          "css-loader",
        ],
      },
      {
        test: /\.eot(\?.*)?$/,
        use: [
          {
            loader: "file-loader",
            options: {
              name: "fonts/[hash].[ext]",
            },
          },
        ],
      },
      {
        test: /\.(woff|woff2)(\?.*)?$/,
        use: [
          {
            loader: "file-loader",
            options: {
              name: "fonts/[hash].[ext]",
            },
          },
        ],
      },
      {
        test: /\.ttf(\?.*)?$/,
        use: [
          {
            loader: "url-loader",
            options: {
              limit: 10000,
              mimetype: "application/octet-stream",
              name: "fonts/[hash].[ext]",
            },
          },
        ],
      },
      {
        test: /\.svg(\?.*)?$/,
        include: /app/,
        use: [
          {
            loader: "url-loader",
            options: {
              limit: 10000,
              mimetype: "image/svg+xml",
              name: "images/[hash].[ext]",
            },
          },
        ],
      },
      {
        test: /\.(jpe?g|png|gif)$/i,
        include: /app/,
        use: [
          {
            loader: "url-loader",
            options: {
              limit: 10000,
              name: "images/[hash].[ext]",
            },
          },
        ],
      },
    ],
  },
};
