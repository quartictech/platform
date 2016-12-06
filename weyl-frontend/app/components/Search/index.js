import React from "react";
import {
  Classes,
  InputGroup,
  Menu,
  MenuDivider,
  MenuItem,
  Popover,
  PopoverInteractionKind,
  Position,
} from "@blueprintjs/core";
import * as classNames from "classnames";
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

  render() {
    const popoverContent = (
      <Menu>{this.renderMenuItems()}</Menu>
    );

    // Hack because pt-input overrides font-family and no way to pass styles through InputGroup
    const fontSetter = (e) => e && (e.style.fontFamily = "Roboto-Light"); // eslint-disable-line no-param-reassign

    return (
      <Popover
        autoFocus={false}
        enforceFocus={false}
        popoverClassName={Classes.MINIMAL}
        content={popoverContent}
        interactionKind={PopoverInteractionKind.CLICK}
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
          inputRef={fontSetter}
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
                className={classNames(
                  Classes.MENU_ITEM,
                  Classes.POPOVER_DISMISS,
                  (result.category === "place") ? "pt-icon-map-marker" : "pt-icon-layers"
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
      this.setState({ query });
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
