import React from "react";
import {
  Button,
  Classes,
  IconContents,
  Menu,
  MenuItem,
  Popover,
} from "@blueprintjs/core";
import * as _ from "underscore";

const NormalPicker = ({
  iconName,
  position,
  selected,
  entries,
  onChange,
}) => {
  const menu = (
    <Menu>
      {_.map(entries, (value, key) =>
        <MenuItem
          key={key || value}
          text={value}
          label={(selected === (key || value)) ? IconContents.TICK : ""}
          iconName={iconName}
          onClick={() => onChange(key || value)}
        />
      )}
    </Menu>
  );

  return (
    <Popover
      content={menu}
      position={position}
      popoverClassName={Classes.MINIMAL}
    >
      <Button
        className={Classes.MINIMAL}
        iconName={iconName}
        text={selected}
      />
    </Popover>
  );
};

export default NormalPicker;
