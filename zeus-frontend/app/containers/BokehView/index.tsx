import * as React from "react";
const DocumentTitle = require("react-document-title");  // TODO: wtf - doesn't work with import
import * as _ from "underscore";
import Pane from "../../components/Pane";
const styles = require("./style.css");

interface BokehViewProps {
  sessionId: string;
}

class BokehView extends React.Component<BokehViewProps, {}> {
  public render() {
    return (
      <DocumentTitle title="Quartic - Bokeh weirdness">
        <div className={styles.container}>
          <Pane title="Some weirdness" iconName="help">
            <div className="bk-root" style={{ width: "100%" }}>
              <div className="bk-plotdiv" style={{ position: "relative" }}>
                <div id="bokeh-plot" />
              </div>
            </div>
          </Pane>

          <Pane title="Some other weirdness" iconName="help">
            <div className="bk-root" style={{ width: "100%" }}>
              <div className="bk-plotdiv" style={{ position: "relative", height: "500px" }}>
                <div id="bokeh-map" />
              </div>
            </div>
          </Pane>
        </div>
      </DocumentTitle>
    );
  }

  public componentDidMount() {
    const sessionId = Math.random().toString(36).slice(2);
    const bokeh = (window as any).bokeh;


    bokeh.client.pull_session("ws://localhost:5006/dashboards/flytipping/ws", sessionId)
      .then(
        (session) => {
            const roots = session.document.roots();

            const itemFor = (modelName: string, elementId: string) => ({
              elementid: elementId,
              modelid: _.find(roots, (r: any) => r.name === modelName).id,
              sessionid: sessionId,
            });

            const items = [
              itemFor("plot", "bokeh-plot"),
              itemFor("map", "bokeh-map"),
            ];

            bokeh.embed.embed_items(null, items, "/dashboards/flytipping", "ws://localhost:5006");
        },
        (error) => console.error(error),
      );
  }
}

export default BokehView;
