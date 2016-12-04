import React from "react";
import { Classes, Dialog, Spinner } from "@blueprintjs/core";

function ConnectionStatus(props) {
  return (
    <Dialog
      isOpen={!props.connectionUp}
      title="Server connection lost"
      iconName="exchange"
      isCloseButtonShown={false}
      canEscapeKeyClose={false}
      canOutsideClickClose={false}
    >
      <div className={Classes.DIALOG_BODY}>
        <div style={{ textAlign: "center" }}>
          <Spinner
            className={Classes.LARGE}
          />
          <h5>Re-establishing connection...</h5>
        </div>
      </div>
    </Dialog>
  );
}

export default ConnectionStatus;
