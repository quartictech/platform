/**
*
* LayerSearch
*
*/

import React from "react";
const $ = require("jquery");

class LayerSearch extends React.Component {
  componentDidMount() {
     $(".ui.search").search({
       apiSettings: {
         responseAsync: (settings, callback) => {
           this.props.onSearch(settings.urlData.query, callback);
         },
       },
       type: "category",
       onSelect: (result, response) => {
         this.props.onSelect(result);
       },
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

export default LayerSearch;
