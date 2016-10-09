import React from "react";
import ReactCSSTransitionGroup from "react-addons-css-transition-group";
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
  const icons = layers.reduce((p, layer) => {
    p[layer.id] = (layer.metadata.icon || DEFAULT_ICON);  // eslint-disable-line no-param-reassign
    return p;
  }, {});
  const visibleLayerIds = new Set(layers.filter(layer => layer.visible).map(layer => layer.id));

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
    <div className={styles.feedPane} style={{ "visibility": visible ? "visible" : "hidden" }}>
      <div className="ui raised fluid segment">
        <h4 className="ui header">
          <a>
            <i className="icon close" onClick={() => onUiToggle("liveFeed")}></i>
          </a>
          Live feed
        </h4>

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
    </div>
  );
}

// Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.

export default FeedPane;
