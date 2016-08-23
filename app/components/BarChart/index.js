/**
*
* BarChart
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';

import {Chart} from 'react-d3-core';
import {LineChart} from 'react-d3-basic';


class BarChart extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super()
    this.test = [
  {
    name: "Lavon Hilll I",
    BMI: 20.57,
    age: 12,
    birthday: "1994-10-26T00:00:00.000Z",
    city: "Annatown",
    married: true,
    index: 1
  },
  {
    name: "Clovis Pagac",
    BMI: 24.28,
    age: 26,
    birthday: "1995-11-10T00:00:00.000Z",
    city: "South Eldredtown",
    married: false,
    index: 3
  },
  {
    name: "Gaylord Paucek",
    BMI: 24.41,
    age: 30,
    birthday: "1975-06-12T00:00:00.000Z",
    city: "Koeppchester",
    married: true,
    index: 5
  },
  {
    name: "Ashlynn Kuhn MD",
    BMI: 23.77,
    age: 32,
    birthday: "1985-08-09T00:00:00.000Z",
    city: "West Josiemouth",
    married: false,
    index: 6
  }
];

this.x = function(d) {
  return d.index;
}

this.chartSeries = [
      {
        field: 'BMI',
        name: 'BMI',
        color: '#ff7f0e'
      }
    ];
    }

  render() {
    return (
      <div className={styles.barChart}>
        <Chart
          title={"test"}
          width={700}
          height={700}
          margins={{left: 100, right: 100, top: 50, bottom: 50}}>
          <LineChart
            showXGrid= {false}
            showYGrid= {false}
            margins= {{left: 100, right: 100, top: 50, bottom: 50}}
            title={"test"}
            data={this.test}
            width={700}
            height={700}
            chartSeries={this.chartSeries}
            x={this.x}
          />
          </Chart>
      </div>
    );
  }
}

export default BarChart;
