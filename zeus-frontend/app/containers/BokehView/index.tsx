import * as React from "react";
const DocumentTitle = require("react-document-title");  // TODO: wtf - doesn't work with import
import Pane from "../../components/Pane";


const BokehView: React.SFC<{}> = (_props) => (
  <DocumentTitle title="Quartic - Bokeh weirdness">
    <div style={{ width: "100%" }}>
      <Pane title="Some weirdness" iconName="help">
        <iframe
          src="https://core.quartic.io/dashboards/flytipping"
          width="100%"
          height="100%"
        />
      </Pane>
    </div>
  </DocumentTitle>
);


export default BokehView;
