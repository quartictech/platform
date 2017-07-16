import * as React from "react";
import {
  Classes,
} from "@blueprintjs/core";
import * as classNames from "classnames";
const styles = require("./style.css");

interface PaneProps {
  title?: string;
  iconName?: string;
  extraHeaderContent?: JSX.Element;
  onClose?: () => void;
  children?: any;
  style?: React.CSSProperties;
}

export default class Pane extends React.Component<PaneProps, {}> {
  render() {
    return (
      <div
        className={classNames(Classes.CARD, Classes.ELEVATION_3, styles.pane)}
        style={{ ...this.props.style, overflow: "auto", display: "flex", flexDirection: "column" }}
      >
        {this.maybeHeader()}

        <div className={Classes.DIALOG_BODY} style={{ margin: "10px", flex: 1 }}>
          <div style={{ height: "100%", flex: 1 }} >
            {this.props.children}
          </div>
        </div>
      </div>
    );
  }

  private maybeHeader() {
    if (!this.props.title) {
      return null;
    }

    return (
      <div className={Classes.DIALOG_HEADER} style={{ paddingRight: "10px" }}>
        <span
          className={classNames(Classes.ICON_STANDARD, Classes.iconClass(this.props.iconName))}
          style={{ paddingRight: 10 }}
        />
        <h5>{this.props.title}</h5>
        {this.props.extraHeaderContent}
        {this.maybeCloseButton()}
      </div>
    );
  }

  private maybeCloseButton() {
    if (!this.props.onClose) {
      return null;
    }

    return (
      <button
        aria-label="Close"
        className={classNames(Classes.DIALOG_CLOSE_BUTTON, Classes.iconClass("small-cross"))}
        onClick={this.props.onClose}
      />
    );
  }
}
