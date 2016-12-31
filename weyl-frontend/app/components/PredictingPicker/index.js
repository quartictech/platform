import React from "react";
import {
  Classes,
  IconContents,
  InputGroup,
  Intent,
  Keys,
  Menu,
  MenuDivider,
  MenuItem,
  Popover,
  PopoverInteractionKind,
  Position,
} from "@blueprintjs/core";
import classNames from "classnames";
import * as _ from "underscore";

class PredictingPicker extends React.Component { // eslint-disable-line react/prefer-stateless-function
  constructor(props) {
    super(props);
    this.state = {
      text: "",
      menuVisible: false,
      shouldFilter: false,
      idxHighlighted: 0,
    };

    this.onInteraction = this.onInteraction.bind(this);
    this.onSelectEntry = this.onSelectEntry.bind(this);
    this.onChangeText = this.onChangeText.bind(this);
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.selectedKey && (this.props.selectedKey !== nextProps.selectedKey)) {
      this.setState({ text: this.entriesAsMap()[nextProps.selectedKey] });
    }
  }

  render() {
    return (
      <Popover
        autoFocus={false}
        enforceFocus={false}
        popoverClassName={Classes.MINIMAL}
        content={this.renderMenu()}
        isOpen={!this.props.disabled && this.state.menuVisible}
        onInteraction={this.onInteraction}
        interactionKind={PopoverInteractionKind.CLICK}
        position={Position.BOTTOM_LEFT}
      >
        <InputGroup
          disabled={this.props.disabled}
          type={this.props.type}
          leftIconName={this.props.leftIconName || this.props.iconName}
          placeholder={this.props.placeholder}
          value={this.state.text}
          onKeyDown={(e) => this.onKeyDown(e.which)}
          onChange={(e) => this.onChangeText(e.target.value)}
          intent={(this.props.selectedKey || this.props.disabled || this.props.errorDisabled) ? Intent.NONE : Intent.DANGER}
        />
      </Popover>
    );
  }

  renderMenu() {
    const items = _.map(this.categorisedFilteredEntries(), (entries, category) => this.renderCategory(category, entries));

    return (
      <Menu>
        {
          _.isEmpty(items)
            ? <MenuItem className={classNames(Classes.MENU_ITEM, Classes.DISABLED)} text="No results found." />
            : items
        }
      </Menu>
    );
  }

  renderCategory(category, entries) {
    return (
      <div key={category}>
        {(category !== "undefined") && <MenuDivider title={category} />}
        {_.map(entries, entry => this.renderEntry(entry))}
      </div>
    );
  }

  // 25px is a hack - compensates for hardcoded ::before size in Blueprint CSS
  renderEntry(entry) {
    return (
      <MenuItem
        key={entry.key}
        text={
          <div style={{ marginLeft: "25px" }}>
            <div><b>{entry.name}{(entry.idx === this.state.idxHighlighted) ? " (X)" : ""}</b></div>
            <small className="pt-text-muted">
              {
                entry.extra
                  ? (
                  <div>
                    <p><b>{entry.description}</b></p>
                    <div style={{ textAlign: "right" }}>
                      <em>{entry.extra}</em>
                    </div>
                  </div>
                  )
                  : (
                  <b>{entry.description}</b>
                  )
              }

            </small>
          </div>
        }
        label={(this.props.selectedKey === entry.key) ? IconContents.TICK : ""}
        iconName={this.props.iconName}
        className={classNames(Classes.MENU_ITEM)}
        onClick={() => this.onSelectEntry(entry.key)}
      />
    );
  }

  onKeyDown(key) {
    switch (key) {
      case Keys.ARROW_DOWN:
        this.setState({ idxHighlighted: Math.min(_.size(this.filteredEntries()) - 1, this.state.idxHighlighted + 1)});
        break;
      case Keys.ARROW_UP:
        this.setState({ idxHighlighted: Math.max(0, this.state.idxHighlighted - 1)});
        break;
      case Keys.ENTER: {
        const entry = _.chain(this.categorisedFilteredEntries())
          .values()
          .flatten()
          .find(entry => entry.idx === this.state.idxHighlighted)
          .value();
        if (entry) {
          this.onSelectEntry(entry.key);
        }
        break;
      }
    }
  }

  onSelectEntry(key) {
    this.setState({ menuVisible: false });
    this.props.onChange(key);
  }

  onInteraction(nextOpenState) {
    this.setState({
      menuVisible: nextOpenState,
      shouldFilter: false,
      idxHighlighted: 0
    });
  }

  onChangeText(text) {
    this.setState({
      text,
      menuVisible: true,
      shouldFilter: true,
      idxHighlighted: 0,
    });
    this.props.onChange(_.invert(this.entriesAsMap())[text]);
  }

  categorisedFilteredEntries() {
    var idx = 0;
    return _.chain(this.filteredEntries())
      .groupBy(entry => entry.category)
      .mapObject(entries => _.map(entries, entry => ({ ...entry, idx: idx++ })))
      .value();
  }

  filteredEntries() {
    return _.chain(this.entriesAsMap())
      .mapObject((v, k) => normalize(k, v))
      .values()
      .filter(entry => !this.state.shouldFilter || !this.state.text || entry.name.toLowerCase().includes(this.state.text.toLowerCase()))
      .value();
  }

  entriesAsMap() {
    return _.isArray(this.props.entries)
      ? _.object(_.map(this.props.entries, x => [x, x]))
      : this.props.entries;
  }
}

const normalize = (key, entry) => {
  const isObject = (typeof entry === "object");
  return {
    key,
    name: isObject ? entry.name : entry,
    description: isObject ? entry.description : undefined,
    extra: isObject ? entry.extra : undefined,
    category: isObject ? entry.category : undefined,
  };
};

PredictingPicker.defaultProps = {
  errorDisabled: false,
  disabled: false,
};

export default PredictingPicker;
