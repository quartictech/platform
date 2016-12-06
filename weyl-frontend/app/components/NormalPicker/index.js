import React from "react";
import {
  Button,
  Classes,
  IconContents,
  Menu,
  MenuItem,
  Popover,
} from "@blueprintjs/core";

const NormalPicker = ({
  iconName,
  position,
  selected,
  entries,
  onChange,
}) => {
  const menu = (
    <Menu>
      {entries.map(entry =>
        <MenuItem
          key={entry}
          text={entry}
          label={(selected === entry) ? IconContents.TICK : ""}
          iconName={iconName}
          onClick={() => onChange(entry)}
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
