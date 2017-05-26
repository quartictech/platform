import * as React from "react";
const DocumentTitle = require("react-document-title");  // TODO: wtf - doesn't work with import
import { connect } from "react-redux";
import { createStructuredSelector } from "reselect";
import * as _ from "underscore";
import Pane from "../../components/Pane";
import RoadSchematic, { RoadSchematicSection } from "../../components/RoadSchematic";


const DATA: RoadSchematicSection[] = _.flatten(
  _.range(40).map(x => _.range(4).map(y => ({
    xMin: x * 20,
    xMax: (x + 1) * 20,
    lane: y,
    value: (Math.random() < 0.25) ? Math.random() : 0
  } as RoadSchematicSection)))
);

interface State {
  plot: any;
}

class SchematicView extends React.Component<{}, State> {
  constructor(props: {}) {
    super(props);
    this.state = {
      plot: null,
    };
  }

  render() {
    return (
      <DocumentTitle title="Quartic - Road schematic">
        <div style={{ width: "100%" }}>
          <h1>Oh hello</h1>
          <Pane title="Roads are noob" iconName="drive-time">
            <RoadSchematic sections={DATA} />
          </Pane>
        </div>
      </DocumentTitle>
    );
  }
}



const mapDispatchToProps = {
};

const mapStateToProps = createStructuredSelector({
});

export default connect(mapStateToProps, mapDispatchToProps)(SchematicView);
