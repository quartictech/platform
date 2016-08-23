/**
*
* BarChart
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';
import styles from './styles.css';

import NVD3Chart from 'react-nvd3';

class BarChart extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor() {
    super()
    this.test = [{
          key: "Cumulative Return",
          values: [
            {
              "label" : "A" ,
              "value" : -29.765957771107
            } ,
            {
              "label" : "B" ,
              "value" : 0
            } ,
            {
              "label" : "C" ,
              "value" : 32.807804682612
            } ,
            {
              "label" : "D" ,
              "value" : 196.45946739256
            } ,
            {
              "label" : "E" ,
              "value" : 0.19434030906893
            } ,
            {
              "label" : "F" ,
              "value" : -98.079782601442
            } ,
            {
              "label" : "G" ,
              "value" : -13.925743130903
            } ,
            {
              "label" : "H" ,
              "value" : -5.1387322875705
            }
          ]
        }
      ];
    }

  render() {
    return (
      <div className={styles.barChart}>
        <FormattedMessage {...messages.header} />
        <NVD3Chart id="barChart" type="discreteBarChart" datum={this.test} x="label" y="value"/>
      </div>
    );
  }
}

export default BarChart;
