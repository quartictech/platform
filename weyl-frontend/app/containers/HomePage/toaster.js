import * as React from "react";
import * as ReactDOM from "react-dom";
import { Position, Toaster } from "@blueprintjs/core";

// This is copy-pasted from blueprint toaster.tsx, but with the hardcoded "inline" prop removed
const createToaster = (props) => {
  const containerElement = document.createElement("div");
  document.body.appendChild(containerElement);
  return ReactDOM.render(<Toaster {...props} />, containerElement);
}

export const OurToaster = createToaster({
    position: Position.TOP,
});
