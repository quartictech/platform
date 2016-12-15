import React from "react";
import {
  Classes,
  IconContents,
  InputGroup,
  Intent,
  Menu,
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
        content={this.menu()}
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
          onChange={(e) => this.onChangeText(e.target.value)}
          intent={(this.props.selectedKey || this.props.disabled || this.props.errorDisabled) ? Intent.NONE : Intent.DANGER}
        />
      </Popover>
    );
  }

  menu() {
    const items = _.chain(this.entriesAsMap())
      .pairs()
      .filter(entry => !this.state.shouldFilter || !this.state.text || entryName(entry[1]).toLowerCase().includes(this.state.text.toLowerCase()))
      .map(entry =>
        <MenuItem
          key={entry[0]}
          text={
            <div>
              <div>{entryName(entry[1])}</div>
              <small className="pt-text-muted">{entryDescription(entry[1])}</small>
            </div>
          }
          label={(this.props.selectedKey === entry[0]) ? IconContents.TICK : ""}
          iconName={this.props.iconName}
          className={classNames(Classes.MENU_ITEM)}
          onClick={() => this.onSelectEntry(entry[0])}
        />
      )
      .value();

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

  onInteraction(nextOpenState) {
    this.setState({ menuVisible: nextOpenState, shouldFilter: false });
  }

  onSelectEntry(key) {
    this.setState({ menuVisible: false });
    this.props.onChange(key);
  }

  onChangeText(text) {
    this.setState({ text, menuVisible: true, shouldFilter: true });
    this.props.onChange(_.invert(this.entriesAsMap())[text]);
  }

  entriesAsMap() {
    return _.isArray(this.props.entries)
      ? _.object(_.map(this.props.entries, x => [x, x]))
      : this.props.entries;
  }
}

const entryName = (entry) => ((typeof entry === "object") ? entry.name : entry);
const entryDescription = (entry) => ((typeof entry === "object") ? entry.description : undefined);

PredictingPicker.defaultProps = {
  errorDisabled: false,
  disabled: false,
};

export default PredictingPicker;
