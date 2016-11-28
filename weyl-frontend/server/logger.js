/* eslint-disable no-console */
const chalk = require("chalk");

const logger = {
  error: err => {
    console.error(chalk.red(err));
  },
};

module.exports = logger;
