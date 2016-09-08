/**
*
* BarChart
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';

import {LineChart} from 'react-d3-basic';
import SizeMe from 'react-sizeme';

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
      <div style={{"visibility": this.props.visible ? "visible" : "hidden"}} className={styles.lineChart}>
        <div className="ui card fluid">
          <div className="ui content" style={{"padding": "5px"}}>
          <LineChart
            showXGrid= {false}
            showYGrid= {false}
            title={"test"}
            data={this.test}
            chartSeries={this.chartSeries}
            x={this.x}
            width={this.props.size.width - 20}
            height={this.props.size.height - 20}
          />
      </div>
      </div>
      </div>
    );
  }
}

export default SizeMe({monitorHeight:true})(BarChart);
