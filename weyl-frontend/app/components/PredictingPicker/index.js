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
    };

    this.menu = this.menu.bind(this);
    this.onSelectEntry = this.onSelectEntry.bind(this);
    this.onChangeText = this.onChangeText.bind(this);
    this.entriesAsMap = this.entriesAsMap.bind(this);
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
        onInteraction={(nextOpenState) => this.setState({ menuVisible: nextOpenState })}
        interactionKind={PopoverInteractionKind.CLICK}
        position={Position.BOTTOM_LEFT}
      >
        <InputGroup
          disabled={this.props.disabled}
          leftIconName={this.props.iconName}
          placeholder={this.props.placeholder}
          value={this.state.text}
          onChange={(e) => this.onChangeText(e.target.value)}
          intent={(this.props.selectedKey || this.props.disabled) ? Intent.NONE : Intent.DANGER}
        />
      </Popover>
    );
  }

  menu() {
    const items = _.chain(this.entriesAsMap())
      .pairs()
      .filter(entry => !this.state.text || entry[1].toLowerCase().includes(this.state.text.toLowerCase()))
      .map(entry =>
        <MenuItem
          key={entry[0]}
          text={entry[1]}
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

  onSelectEntry(key) {
    this.setState({ menuVisible: false });
    this.props.onChange(key);
  }

  onChangeText(text) {
    this.setState({ text, menuVisible: true });
    this.props.onChange(_.invert(this.entriesAsMap())[text]);
  }

  entriesAsMap() {
    return _.isArray(this.props.entries)
      ? _.object(_.map(this.props.entries, x => [x, x]))
      : this.props.entries;
  }
}

PredictingPicker.defaultProps = {
  disabled: false,
};

export default PredictingPicker;
