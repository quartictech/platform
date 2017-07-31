import * as React from "react";
import * as _ from "underscore";
import Pane from "../../components/Pane";
const styles = require("./style.css");

class Flytipping extends React.Component<{}, {}> {
  public render() {
    return (
    <div style={{ flex: 1 }}>
      <div className={styles.splitRow}>
        <Pane title="Flytipping over time" iconName="help">
          <div className="bk-root" style={{ width: "100%" }}>
            <div className="bk-plotdiv" style={{ position: "relative" }}>
              <div id="bokeh-plot" />
            </div>
          </div>
        </Pane>

        <Pane title="Flytipping map" iconName="help">
          <div className="bk-root" style={{ width: "100%" }}>
            <div className="bk-plotdiv" style={{ position: "relative", height: "500px" }}>
              <div id="bokeh-map" />
            </div>
          </div>
        </Pane>
      </div>
    </div>
    );
  }

  public componentDidMount() {
    const sessionId = Math.random().toString(36).slice(2);
    const bokeh = (window as any).bokeh;

    bokeh.client.pull_session(`wss://${location.host}/dashboards/flytipping/ws`, sessionId)
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

          bokeh.embed.embed_items(null, items, "/dashboards/flytipping", `wss://${location.host}`);
        },
        error => console.error(error),
      );
  }
}

export default Flytipping;
