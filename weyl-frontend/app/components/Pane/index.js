import React from "react";
import {
  Button,
  Classes,
} from "@blueprintjs/core";
import classNames from "classnames";
import styles from "./styles.css";

const Pane = ({
  title,
  iconName,
  visible,
  onClose,
  children,
}) => (
  <div
    className={classNames(Classes.CARD, Classes.ELEVATION_3, styles.pane)}
    style={{
      padding: 0,
      visibility: visible ? "visible" : "hidden",
    }}
  >
    <div className={Classes.DIALOG_HEADER}>
      <span className={classNames(Classes.ICON_LARGE, Classes.iconClass("chart"))}></span>
      <h5>{title}</h5>
      <button
        aria-label="Close"
        className={classNames(Classes.DIALOG_CLOSE_BUTTON, Classes.iconClass("small-cross"))}
        onClick={onClose}
      />
    </div>
    <div className={Classes.DIALOG_BODY}>
      {children}
    </div>
  </div>
);

export default Pane;
