import * as React from "react";
import {
  AnchorButton,
  Classes,
  Position,
  Tooltip,
} from "@blueprintjs/core";
import Pane from "../../components/Pane";
import Map from "../../components/Map";
import { Asset } from "../../models";

interface PreviewMapProps {
  asset: Asset;
}

interface State {
  satellite: boolean;
}

class PreviewMap extends React.Component<PreviewMapProps, State> {
  constructor(props: PreviewMapProps) {
    super(props);
    this.state = {
      satellite: false,
    };
  }

  render() {
    const fc: GeoJSON.FeatureCollection<GeoJSON.LineString> = {
      type: "FeatureCollection",
      features: [
        {
          type: "Feature",
          geometry: this.props.asset._geometry,
          properties: {},
        },
      ],
    };
    return (
      <Pane
        iconName="globe"
        title="Map"
        extraHeaderContent={this.stylePicker()}
      >
        <Map
          height={300}
          width={500}
          featureCollection={fc}
          style={this.state.satellite ? "satellite" : "bright"}
        />
      </Pane>
    );
  }

  private stylePicker() {
    return (
      <Tooltip content="Satellite" position={Position.BOTTOM}>
        <AnchorButton
          className={Classes.MINIMAL}
          iconName="satellite"
          active={this.state.satellite}
          onClick={() => this.setState({ satellite: !this.state.satellite })}
        />
      </Tooltip>
    );
  }
}


export default PreviewMap;
