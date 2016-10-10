import React from "react";
import ReactCSSTransitionGroup from "react-addons-css-transition-group";
import Pane from "../Pane";
import moment from "moment";
import styles from "./styles.css";
import classNames from "classnames";
const _ = require("underscore");

const DEFAULT_ICON = "grey map";

const FeedEvent = ({ event }) => (
  <div className={classNames("event", styles.slideFade)}>
    <div className="label">
      <i className={`circular ${event.icon} icon`}></i>
    </div>
    <div className="content">
      <div className="summary">
        <a className="user">{event.source}</a> <div className="date">{moment(event.timestamp).fromNow()}</div>
      </div>
      <div className="extra text">{event.message}</div>
    </div>
  </div>
);

const visibleEvents = (events, layers) => {
  const icons = _.mapObject(layers, (v, k) => (v.metadata.icon || DEFAULT_ICON));
  const visibleLayerIds = new Set(_.values(layers).filter(layer => layer.visible).map(layer => layer.id));

  return _.chain(events).keys()
    .filter(layerId => visibleLayerIds.has(layerId))
    .map(layerId => events[layerId].map(e => ({ ...e, icon: icons[layerId] })))
    .flatten()
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(1, 10)
    .value();
};

function FeedPane({ feed, layers, visible, onUiToggle }) {
  return (
    <Pane title="Live feed" visible={visible} onClose={() => onUiToggle("liveFeed")}>
      <div style={{ maxHeight: "30em", overflow: "auto" }}>
        <ReactCSSTransitionGroup
          component="div"
          className="ui feed"
          transitionName="example"
          transitionEnterTimeout={500}
          transitionLeaveTimeout={500}
        >
          {
            visibleEvents(feed.events, layers)
              .map(e => (
                <FeedEvent key={e.id} event={e} />
              ))
          }
        </ReactCSSTransitionGroup>
      </div>
    </Pane>
  );
}

export default FeedPane;
