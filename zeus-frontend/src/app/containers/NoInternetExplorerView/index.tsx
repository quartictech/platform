import * as React from "react";
const DocumentTitle = require("react-document-title");  // tslint:disable-line:variable-name
import {
  Dialog,
  NonIdealState,
} from "@blueprintjs/core";

// tslint:disable-next-line:variable-name
const NoInternetExplorerView: React.SFC<{}> = () => (
  <DocumentTitle title="Quartic - Please use Firefox or Chrome">
    <Dialog
      iconName="warning-sign"
      title="Please switch to Firefox or Chrome"
      isOpen={true}
      canEscapeKeyClose={false}
      canOutsideClickClose={false}
      isCloseButtonShown={false}
    >
      <div style={{ paddingTop: "30px" }}>
        <NonIdealState
          visual="warning-sign"
          title="Quartic currently doesn't support Internet Explorer"
          description={
            <div>
              <p>
                You can download a portable version of Firefox
                from <a href="https://tools.quartic.io/FirefoxPortable_53.0.3_English.paf.exe">the Quartic website</a>.
              </p>
            </div>
          }
        />
      </div>
    </Dialog>
  </DocumentTitle>
);

export default NoInternetExplorerView;
