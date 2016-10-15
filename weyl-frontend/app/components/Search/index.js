import React from "react";
const $ = require("jquery");

class Search extends React.Component {
  componentDidMount() {
    $(".ui.search").search({
      apiSettings: {
        responseAsync: (settings, callback) => {
          this.props.onSearch(settings.urlData.query, callback);
        },
      },
      type: "category",
      onSelect: (result) => {
        switch (result.category) {
          case "layer":
            this.props.onSelectLayer(result.payload);
            break;
          case "place":
            this.props.onSelectPlace(result.payload);
            break;
        }
      },
    });
  }

  render() {
    return (
      <div className="ui category search right aligned">
        <div className="ui icon input">
          <input className="prompt" placeholder="Search..." type="text"></input>
          <i className="search icon"></i>
        </div>
        <div className="results"></div>
      </div>
    );
  }
}

export default Search;
