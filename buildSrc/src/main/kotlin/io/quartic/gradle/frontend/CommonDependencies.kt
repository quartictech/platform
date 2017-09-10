package io.quartic.gradle.frontend

val standardDependencies = mapOf(
    // Webpack build
    "babel-core" to "6.18.2",
    "babel-loader" to "6.4.1",
    "babel-plugin-transform-runtime" to "^6.23.0",
    "babel-preset-es2015" to "^6.24.1",
    "copy-webpack-plugin" to "4.0.1",
    "css-loader" to "0.23.1",
    "file-loader" to "0.8.5",
    "json-loader" to "0.5.4",
    "postcss-cssnext" to "2.9.0",
    "postcss-loader" to "2.0.6",
    "react-hot-loader" to "1.3.1",
    "source-map-loader" to "0.1.5",
    "style-loader" to "0.13.1",
    "ts-loader" to "2.3.4",
    "typescript" to "2.5.0",
    "url-loader" to "0.5.7",
    "webpack" to "2.6.1",
    "webpack-merge" to "4.1.0",

    // Lint
    "tslint" to "5.7.0",
    "tslint-config-airbnb" to "5.2.1",
    "tslint-react" to "3.2.0",

    // Dev server
    "ts-node" to "3.3.0",
    "express" to "4.15.4",
    "webpack-dev-middleware" to "1.12.0",
    "webpack-dev-server" to "2.7.1",
    "webpack-hot-middleware" to "2.19.1",
    "http-proxy-middleware" to "0.17.4",

    // Other
    "redux-devtools-extension" to "2.13.2"
)

