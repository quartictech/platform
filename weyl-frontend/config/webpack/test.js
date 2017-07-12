// TODO: this file can be eliminated once we move Weyl to TS and no longer use eslint.json
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
