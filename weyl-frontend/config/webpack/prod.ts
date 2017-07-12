import * as webpack from "webpack";
import * as merge from "webpack-merge";
import * as HtmlWebpackPlugin from "html-webpack-plugin";
import * as ExtractTextPlugin from "extract-text-webpack-plugin";
import * as baseConfig from "./base";

module.exports = merge(baseConfig, {
  bail: true,

  // In production, we skip all hot-reloading stuff
  entry: [
    "./src/app/app.js",
  ],

  // Utilize long-term caching by adding content hashes (not compilation hashes) to compiled assets
  output: {
    filename: "[name].[chunkhash].js",
    chunkFilename: "[name].[chunkhash].chunk.js",
  },

  plugins: [
    new webpack.optimize.CommonsChunkPlugin({
      name: "vendor",
      children: true,
      minChunks: 2,
      async: true,
    }),

    // Minify and optimize the JavaScript
    new webpack.optimize.UglifyJsPlugin({
      compress: {
        warnings: false, // ...but do not show warnings in the console (there is a lot of them)
      },
    }),

    // Minify and optimize the index.html
    new HtmlWebpackPlugin({
      template: "src/app/index.html",
      minify: {
        removeComments: true,
        collapseWhitespace: true,
        removeRedundantAttributes: true,
        useShortDoctype: true,
        removeEmptyAttributes: true,
        removeStyleLinkTypeAttributes: true,
        keepClosingSlash: true,
        minifyJS: true,
        minifyCSS: true,
        minifyURLs: true,
      },
      inject: true,
    }),

    // Extract the CSS into a seperate file
    new ExtractTextPlugin("[name].[contenthash].css"),
  ],
});
