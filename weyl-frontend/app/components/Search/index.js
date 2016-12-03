import React from "react";
import {
  Button,
  Classes,
  InputGroup,
  Menu,
  MenuDivider,
  MenuItem,
  Popover,
  PopoverInteractionKind,
  Position
} from "@blueprintjs/core";
import * as classnames from "classnames";
import * as _ from "underscore";

class Search extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      query: "",
      results: {},
    };

    this.setQuery = this.setQuery.bind(this);
    this.clearQuery = this.clearQuery.bind(this);
    this.onInteraction = this.onInteraction.bind(this);
  }

  // TODO: no results

  render() {
    const popoverContent = (
      <Menu>{this.renderMenuItems()}</Menu>
    );

    return (
      <Popover
        autoFocus={false}
        enforceFocus={false}
        popoverClassName={Classes.MINIMAL}
        content={popoverContent}
        interactionKind={PopoverInteractionKind.CLICK}
        inline={true}
        isOpen={!_.isEmpty(this.state.results)}
        onInteraction={this.onInteraction}
        position={Position.BOTTOM_LEFT}
      >
        <InputGroup
          type="search"
          leftIconName="search"
          placeholder="Search datasets..."
          value={this.state.query}
          onChange={(e) => this.setQuery(e.target.value)}
        />
      </Popover>
    );
  }

  renderMenuItems() {
    const items = _.values(this.state.results)
      .filter(r => !_.isEmpty(r.results))
      .map(r => (
        <div key={r.name}>
          <MenuDivider title={r.name} />
          {
            _.map(r.results, (result, idx) =>
              <a
                key={idx}
                className={classnames(
                  Classes.MENU_ITEM,
                  Classes.POPOVER_DISMISS,
                  (result.category === "place") ? "pt-icon-map-marker" : "pt-icon-layer"
                )}
                onClick={() => ((result.category === "place")
                  ? this.props.onSelectPlace(result.payload)
                  : this.props.onSelectLayer(result.payload)
                )}
              >
                <div>{result.title}</div>
                <small className="pt-text-muted">{result.description}</small>
              </a>
            )
          }
        </div>
      ));

    return _.isEmpty(items)
      ? <MenuItem className={Classes.DISABLED} text="No results found." />
      : items;
  }

  setQuery(query) {
    if (query) {
      this.setState({ query: query });
      this.props.onSearch(query, (results) => this.setState({ results }));
    } else {
      this.clearQuery();
    }
  }

  clearQuery() {
    this.setState({ query: "", results: {} });
  }

  onInteraction(nextOpenState) {
    if (!nextOpenState) {
      this.clearQuery();
    }
  }
}

export default Search;
