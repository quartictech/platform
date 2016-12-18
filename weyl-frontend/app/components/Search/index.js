import React from "react";
import * as _ from "underscore";

import PredictingPicker from "../PredictingPicker";

class Search extends React.Component {
  constructor(props) {
    super(props);
    this.onChange = this.onChange.bind(this);
  }

  render() {
    return (
      <PredictingPicker
        type="search"
        leftIconName="search"
        iconName="layers"
        placeholder="Search datasets..."
        errorDisabled
        entries={this.formatLayerList()}
        selectedKey={null}
        onChange={(layerId) => this.onChange(layerId)}
      />
    );
  }

  formatLayerList() {
    return _.object(_.map(this.props.layerList, item => [item.id, {
      name: item.metadata.name,
      description: item.metadata.description,
      category: item.live ? "Live layers" : "Static layers",
    }]));
  }

  onChange(layerId) {
    if (layerId) {
      this.props.onSelectLayer(_.find(this.props.layerList, item => item.id === layerId));
    }
  }
}

export default Search;
