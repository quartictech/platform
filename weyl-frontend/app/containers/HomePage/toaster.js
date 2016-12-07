import * as React from "react";
import * as ReactDOM from "react-dom";
import {
  Intent,
  Position,
  Toaster,
} from "@blueprintjs/core";

// This is copy-pasted from blueprint toaster.tsx, but with the hardcoded "inline" prop removed
const createToaster = (props) => {
  const containerElement = document.createElement("div");
  document.body.appendChild(containerElement);
  return ReactDOM.render(<Toaster {...props} />, containerElement);
}

const intent = (level) => {
  switch (level) {
    case "WARNING":
      return Intent.WARNING;
    case "SEVERE":
      return Intent.DANGER;
    default:
      return Intent.PRIMARY;
  }
}

const iconName = (level) => {
  switch (level) {
    case "WARNING":
    case "SEVERE":
      return "warning-sign";
    default:
      return "info-sign";
  }
}

export const showToast = (alert) => {
  OurToaster.show({
    message: <div>{alert.title}<br /><small>{alert.body}</small></div>,
    intent: intent(alert.level),
    iconName: iconName(alert.level),
  });
}

export const OurToaster = createToaster({
    position: Position.TOP_RIGHT,
});
