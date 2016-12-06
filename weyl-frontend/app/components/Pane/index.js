import React from "react";
import {
  Classes,
} from "@blueprintjs/core";
import classNames from "classnames";
import styles from "./styles.css";

const Pane = ({
  title,
  iconName,
  visible,
  extraHeaderContent,
  onClose,
  children,
}) => (
  <div
    className={classNames(Classes.DARK, Classes.CARD, Classes.ELEVATION_3, styles.pane)}
    style={{ overflow: "scroll", display: visible ? "block" : "none" }}
  >
    <div className={Classes.DIALOG_HEADER}>
      <span className={classNames(Classes.ICON_STANDARD, Classes.iconClass(iconName))}></span>
      <h5>{title}</h5>
      {extraHeaderContent}
      {
        onClose
          ? (
          <button
            aria-label="Close"
            className={classNames(Classes.DIALOG_CLOSE_BUTTON, Classes.iconClass("small-cross"))}
            onClick={onClose}
          />
          )
          : null
      }
    </div>
    <div
      className={Classes.DIALOG_BODY}
      style={{ margin: "10px", height: "100%" }}
    >
      {children}
    </div>
  </div>
);

export default Pane;
