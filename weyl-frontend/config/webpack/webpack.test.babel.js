/**
 * TEST WEBPACK CONFIGURATION
 */

const modules = [
  'app',
  'node_modules',
];

module.exports = {
  resolve: {
    modulesDirectories: modules,
    modules,
  },
};
