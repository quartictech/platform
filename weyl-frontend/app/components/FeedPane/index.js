import React from "react";
import moment from "moment";
import styles from "./styles.css";

const FeedEvent = ({ event }) => (
  <div className="event">
    <div className="label">
      <i className={`circular ${event.icon} icon`}></i>
    </div>
    <div className="content">
      <div className="summary">
        <a className="user">{event.user}</a> <div className="date">{moment.unix(event.timestamp).fromNow()}</div>
      </div>
      <div className="extra text">{event.body}</div>
    </div>
  </div>
);

function FeedPane({ feed }) {
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
            {
              feed.events.map(e => (
                <FeedEvent key={e.id} event={e} />
              ))
            }
          </div>
        </div>
      </div>
    </div>
  );
}

// Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.

export default FeedPane;

