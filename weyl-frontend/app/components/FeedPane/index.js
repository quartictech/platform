import React from "react";
import ReactCSSTransitionGroup from "react-addons-css-transition-group";
import moment from "moment";
import styles from "./styles.css";
import classNames from "classnames";

const FeedEvent = ({ event }) => (
  <div className={classNames("event", styles.slideFade)}>
    <div className="label">
      <i className={`circular phone icon`}></i>
    </div>
    <div className="content">
      <div className="summary">
        <a className="user">{event.source}</a> <div className="date">{moment(event.timestamp).fromNow()}</div>
      </div>
      <div className="extra text">{event.message}</div>
    </div>
  </div>
);

function FeedPane({ feed, layers }) {
  let visibleLayerIds = new Set(layers.filter(layer => layer.visible).map(layer => layer.id));
  let events = [].concat.apply([],
    Object.keys(feed.events)
      .filter(layerId => visibleLayerIds.has(layerId))
      .map(k => feed.events[k])
    )
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(1, 10);

  return (
    <div className={styles.feedPane}>
      <div className="ui raised fluid card">
        <div className="content">
          <div className="header">
            <a>
              <i className="icon close"></i>
            </a>
            Live feed
          </div>
          <div className="meta">
            Random BS
          </div>

          <div className="ui small feed">
            <ReactCSSTransitionGroup transitionName="example">
              {
                events
                .map(e => (
                  <FeedEvent key={e.id} event={e} />
                ))
              }
          </ReactCSSTransitionGroup>
          </div>
        </div>
      </div>
    </div>
  );
}

// Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.

export default FeedPane;
