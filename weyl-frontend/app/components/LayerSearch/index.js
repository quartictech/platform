/**
*
* LayerSearch
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';

import styles from './styles.css';

import { search } from 'semantic-ui/dist/semantic.min.js';
var $ = require('jquery');

class LayerSearch extends React.Component {
    componentDidMount() {
       $('.ui.search').search({
         apiSettings: {
           responseAsync: (settings, callback) => {
             this.props.onSearch(settings.urlData.query, callback);
           }
         },
         type: 'category',
         onSelect: (result, response) => {
           this.props.onSelect(result);
         }
       });
     }

    render() {
      return (
        <div className="ui category search right aligned">
        <div className="ui icon input">
        <input className="prompt" placeholder="Search layers.." type="text"></input>
        <i className="search icon"></i>
        </div>
        <div className="results"></div>
        </div>
      );
    }
}

LayerSearch.propTypes = {
  onSearch: React.PropTypes.func,
  onSelect: React.PropTypes.func
}

export default LayerSearch;
