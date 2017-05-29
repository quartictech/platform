import * as React from "react";
import Pane from "../../components/Pane";
import { Map } from "../../components/Map";
import { Asset } from "../../models";

interface PreviewMapProps {
  asset: Asset;
}

const PreviewMap: React.SFC<PreviewMapProps> = (props) => {
  const fc: GeoJSON.FeatureCollection<GeoJSON.LineString> = {
    type: "FeatureCollection",
    features: [
      {
        type: "Feature",
        geometry: props.asset._geometry,
        properties: {},
      },
    ],
  };
  return (
    <Pane>
      <Map height={100} width={500} featureCollection={fc}/>
    </Pane>
  );
};

export default PreviewMap;
