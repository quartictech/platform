import * as React from "react";
import {
  Button,
  Classes,
  IconContents,
  Menu,
  MenuItem,
  Popover,
  Position,
} from "@blueprintjs/core";

const s = require("./style.css");

interface NormalPickerProps {
  iconName?: string;
  position?: Position;
  selected: string;
  entries: string[];
  onChange: (string) => void;
}

export default class NormalPicker extends React.Component<NormalPickerProps, {}> {
  render() {
    return (
      <Popover
        className={s.normalPicker}
        content={this.renderMenu()}
        position={this.props.position}
        popoverClassName={Classes.MINIMAL}
      >
        <Button
          className={Classes.MINIMAL}
          iconName={this.props.iconName}
          text={this.props.selected}
        />
      </Popover>
    );
  }

  private renderMenu() {
    return (
      <Menu className={s.normalPickerMenu}>
        {this.props.entries.map(entry =>
          <MenuItem
            key={entry}
            text={entry}
            label={(this.props.selected === entry) ? IconContents.TICK : ""}
            iconName={this.props.iconName}
            onClick={() => this.props.onChange(entry)}
          />)}
      </Menu>
    );
  }
}
