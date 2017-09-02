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
    "typescript" to "2.1.5",
    "url-loader" to "0.5.7",
    "webpack" to "2.6.1",
    "webpack-merge" to "4.1.0",

    // Weyl-specific (TODO: consolidate Webpack config and merge this with the above)
    "babel-eslint" to "6.1.2",
    "babel-plugin-react-transform" to "2.0.2",
    "babel-plugin-transform-react-constant-elements" to "6.9.1",
    "babel-plugin-transform-react-inline-elements" to "6.8.0",
    "babel-plugin-transform-react-remove-prop-types" to "0.2.9",
    "babel-preset-react" to "6.11.1",
    "babel-preset-react-hmre" to "1.1.1",
    "babel-preset-stage-0" to "6.5.0",
    "eventsource-polyfill" to "0.9.6",
    "exports-loader" to "0.6.3",
    "extract-text-webpack-plugin" to "2.1.2",
    "html-loader" to "0.4.3",
    "html-webpack-plugin" to "2.22.0",
    "image-webpack-loader" to "2.0.0",
    "imports-loader" to "0.6.5",
    "postcss-focus" to "1.0.0",
    "postcss-reporter" to "1.4.1",

    // Lint
    "eslint" to "3.5.0",
    "eslint-config-airbnb" to "10.0.0",
    "eslint-import-resolver-webpack" to "0.4.0",
    "eslint-plugin-import" to "1.12.0",
    "eslint-plugin-jsx-a11y" to "2.0.1",
    "eslint-plugin-react" to "6.0.0",

    "stylelint" to "7.1.0",
    "stylelint-config-standard" to "12.0.0",

    "tslint" to "5.4.3",
    "tslint-config-airbnb" to "5.2.1",
    "tslint-react" to "3.0.0",

    // Dev server
    "ts-node" to "3.2.0",
    "express" to "4.13.4",
    "webpack-dev-middleware" to "1.11.0",
    "webpack-dev-server" to "2.5.0",
    "webpack-hot-middleware" to "2.18.0",
    "http-proxy-middleware" to "0.17.3",

    // Other
    "redux-devtools-extension" to "2.13.2"
)

