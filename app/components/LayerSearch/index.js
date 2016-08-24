/**
*
* LayerSearch
*
*/

import React from 'react';

import { FormattedMessage } from 'react-intl';
import messages from './messages';

import styles from './styles.css';

//var search = require('semantic-ui/dist/components/search.js');

class LayerSearch extends React.Component {
    componentDidMount() {
      let content = [
    {
      title: 'Horse',
      description: 'An Animal',
    },
    {
      title: 'Cow',
      description: 'Another Animal',
    }
  ]
;
  //     search').search({
  //   source : content,
  //   searchFields   : [
  //     'title'
  //   ],
  //   searchFullText: false
  // });
    }

    render() {
      return (
        <div className="ui category search">
        <div className="ui icon input">
        <input className="prompt" placeholder="Search layers.." type="text"></input>
        <i className="search icon"></i>
        </div>
        <div className="results"></div>
        </div>
      );
    }
}

export default LayerSearch;
