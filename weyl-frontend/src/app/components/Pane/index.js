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
    style={{ overflow: "auto", display: visible ? "flex" : "none", flexDirection: "column" }}
  >
    <div className={Classes.DIALOG_HEADER}>
      <span
        className={classNames(Classes.ICON_STANDARD, Classes.iconClass(iconName))}
        style={{ paddingRight: 10 }}
      ></span>
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
      style={{ margin: "10px", flex: 1 }}
    >
      <div style={{ height: "100%", flex: 1 }} >
        {children}
      </div>
    </div>
  </div>
);

export default Pane;
